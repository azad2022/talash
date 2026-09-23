package com.example.hardware

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.example.hardware.connection.BluetoothSppTransport
import com.example.hardware.connection.UsbPermissionBroker
import com.example.hardware.connection.UsbSerialTransport
import com.example.hardware.core.HardwareConnectionState
import com.example.hardware.core.HardwareDevice
import com.example.hardware.core.HardwareDeviceType
import com.example.hardware.core.HardwareResult
import com.example.hardware.core.HardwareTransport
import com.example.hardware.core.HardwareTransportType
import com.example.hardware.core.SerialConnectionSettings
import com.example.hardware.core.StableWeight
import com.example.hardware.scale.ScaleWeightParser
import com.example.hardware.scale.StableWeightDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PairedBluetoothDevice(
    val name: String,
    val address: String
)

class HardwareManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _connectionStates =
        MutableStateFlow<Map<HardwareDeviceType, HardwareConnectionState>>(emptyMap())
    val hardwareConnectionStates: StateFlow<Map<HardwareDeviceType, HardwareConnectionState>> =
        _connectionStates

    private val _connectedDevices =
        MutableStateFlow<Map<HardwareDeviceType, HardwareDevice>>(emptyMap())
    val hardwareConnectedDevices: StateFlow<Map<HardwareDeviceType, HardwareDevice>> =
        _connectedDevices

    val connectionState: StateFlow<HardwareConnectionState> =
        _connectionStates
            .map { states ->
                when {
                    states.values.any { it == HardwareConnectionState.CONNECTED } ->
                        HardwareConnectionState.CONNECTED
                    states.values.any { it == HardwareConnectionState.CONNECTING } ->
                        HardwareConnectionState.CONNECTING
                    states.values.any { it == HardwareConnectionState.ERROR } ->
                        HardwareConnectionState.ERROR
                    else -> HardwareConnectionState.DISCONNECTED
                }
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = HardwareConnectionState.DISCONNECTED
            )

    val connectedDevice: StateFlow<HardwareDevice?> =
        _connectedDevices
            .map { devices -> devices.values.lastOrNull() }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    private val _latestStableWeight = MutableStateFlow<StableWeight?>(null)
    val latestStableWeight: StateFlow<StableWeight?> = _latestStableWeight

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val transports = mutableMapOf<HardwareDeviceType, HardwareTransport>()
    private val readerJobs = mutableMapOf<HardwareDeviceType, Job>()
    private val stateJobs = mutableMapOf<HardwareDeviceType, Job>()
    private var scaleWatchdogJob: Job? = null

    private val weightDetector = StableWeightDetector()
    private val inputBuffer = StringBuilder()

    fun pairedBluetoothDevices(): List<PairedBluetoothDevice> {
        if (
            android.os.Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices
            .map { device ->
                PairedBluetoothDevice(
                    name = device.name ?: "Bluetooth",
                    address = device.address
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun usbSerialDevices(): List<HardwareDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return com.hoho.android.usbserial.driver.UsbSerialProber
            .getDefaultProber()
            .findAllDrivers(usbManager)
            .map { driver ->
                HardwareDevice(
                    id = driver.device.deviceId.toString(),
                    name = driver.device.productName ?: "USB Serial",
                    type = HardwareDeviceType.SCALE,
                    transport = HardwareTransportType.USB_SERIAL,
                    vendorId = driver.device.vendorId,
                    productId = driver.device.productId
                )
            }
    }

    fun connectBluetooth(
        name: String,
        address: String,
        type: HardwareDeviceType
    ) {
        scope.launch {
            disconnect(type)
            val device = HardwareDevice(
                id = address,
                name = name,
                type = type,
                transport = HardwareTransportType.BLUETOOTH_SPP,
                address = address
            )
            val transport = BluetoothSppTransport(context)
            transports[type] = transport
            setState(type, HardwareConnectionState.CONNECTING)

            when (val result = transport.connect(device)) {
                is HardwareResult.Success -> {
                    _lastError.value = null
                    setConnected(type, device)
                    setState(type, HardwareConnectionState.CONNECTED)
                    observeTransportState(type, transport, device)
                    startReader(type, transport)
                }

                is HardwareResult.Failure -> {
                    _lastError.value = result.message
                    transports.remove(type)
                    clearConnected(type)
                    setState(type, HardwareConnectionState.ERROR)
                }
            }
        }
    }

    fun connectUsb(
        deviceId: Int,
        name: String,
        type: HardwareDeviceType,
        settings: SerialConnectionSettings = SerialConnectionSettings()
    ) {
        scope.launch {
            disconnect(type)

            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val usbDevice = usbManager.deviceList.values.firstOrNull { it.deviceId == deviceId }
            if (usbDevice == null) {
                _lastError.value = "دستگاه USB پیدا نشد."
                setState(type, HardwareConnectionState.ERROR)
                return@launch
            }

            setState(type, HardwareConnectionState.CONNECTING)

            if (!UsbPermissionBroker.ensurePermission(context, usbDevice)) {
                _lastError.value = "مجوز دسترسی به دستگاه USB صادر نشد."
                setState(type, HardwareConnectionState.ERROR)
                return@launch
            }

            val device = HardwareDevice(
                id = deviceId.toString(),
                name = name,
                type = type,
                transport = HardwareTransportType.USB_SERIAL,
                vendorId = usbDevice.vendorId,
                productId = usbDevice.productId
            )
            val transport = UsbSerialTransport(context, settings)
            transports[type] = transport

            when (val result = transport.connect(device)) {
                is HardwareResult.Success -> {
                    _lastError.value = null
                    setConnected(type, device)
                    setState(type, HardwareConnectionState.CONNECTED)
                    observeTransportState(type, transport, device)
                    startReader(type, transport)
                }

                is HardwareResult.Failure -> {
                    _lastError.value = result.message
                    transports.remove(type)
                    clearConnected(type)
                    setState(type, HardwareConnectionState.ERROR)
                }
            }
        }
    }

    fun writeFor(
        expectedType: HardwareDeviceType,
        bytes: ByteArray,
        onResult: (HardwareResult<Unit>) -> Unit = {}
    ) {
        val active = transports[expectedType]
        val device = _connectedDevices.value[expectedType]

        if (active == null || device == null) {
            onResult(HardwareResult.Failure("دستگاه مناسب برای این عملیات متصل نیست."))
            return
        }

        scope.launch {
            val result = active.write(bytes)
            if (result is HardwareResult.Failure) {
                _lastError.value = result.message
            }
            onResult(result)
        }
    }

    fun disconnect(type: HardwareDeviceType) {
        readerJobs.remove(type)?.cancel()
        stateJobs.remove(type)?.cancel()

        transports.remove(type)?.disconnect()

        if (type == HardwareDeviceType.SCALE) {
            scaleWatchdogJob?.cancel()
            scaleWatchdogJob = null
            weightDetector.reset()
            inputBuffer.clear()
            _latestStableWeight.value = null
        }

        clearConnected(type)
        setState(type, HardwareConnectionState.DISCONNECTED)
    }

    fun disconnect() {
        transports.keys.toList().forEach(::disconnect)
    }

    private fun setState(
        type: HardwareDeviceType,
        state: HardwareConnectionState
    ) {
        _connectionStates.value = _connectionStates.value.toMutableMap().apply {
            this[type] = state
        }
    }

    private fun setConnected(
        type: HardwareDeviceType,
        device: HardwareDevice
    ) {
        _connectedDevices.value = _connectedDevices.value.toMutableMap().apply {
            this[type] = device
        }
    }

    private fun clearConnected(type: HardwareDeviceType) {
        _connectedDevices.value = _connectedDevices.value.toMutableMap().apply {
            remove(type)
        }
    }

    private fun observeTransportState(
        type: HardwareDeviceType,
        active: HardwareTransport,
        device: HardwareDevice
    ) {
        stateJobs[type]?.cancel()
        stateJobs[type] = scope.launch {
            active.state.collect { state ->
                if (transports[type] !== active) return@collect

                setState(type, state)

                if (state == HardwareConnectionState.ERROR) {
                    _lastError.value = "ارتباط با «${device.name}» قطع یا دچار خطا شد."
                    transports.remove(type)
                    clearConnected(type)
                    readerJobs.remove(type)?.cancel()
                    active.disconnect()
                }
            }
        }
    }

    private fun startReader(
        type: HardwareDeviceType,
        active: HardwareTransport
    ) {
        readerJobs[type]?.cancel()
        if (type != HardwareDeviceType.SCALE) return

        readerJobs[type] = scope.launch(Dispatchers.IO) {
            active.incomingBytes().collect { bytes ->
                decodeScaleBytes(bytes)
            }
        }
    }

    private fun decodeScaleBytes(bytes: ByteArray) {
        inputBuffer.append(bytes.toString(Charsets.UTF_8).replace('\u0000', ' '))

        while (true) {
            val lineEnd = inputBuffer.indexOfAny(charArrayOf('\r', '\n'))
            if (lineEnd < 0) break

            val line = inputBuffer.substring(0, lineEnd).trim()
            if (line.isNotBlank()) feedScaleValue(line)

            var deleteCount = lineEnd + 1
            while (
                deleteCount < inputBuffer.length &&
                (inputBuffer[deleteCount] == '\r' || inputBuffer[deleteCount] == '\n')
            ) {
                deleteCount++
            }
            inputBuffer.delete(0, deleteCount)
        }

        if (inputBuffer.length > 128) {
            val snapshot = inputBuffer.toString().trim()
            inputBuffer.clear()
            if (snapshot.isNotBlank()) feedScaleValue(snapshot)
        }
    }

    private fun feedScaleValue(line: String) {
        val parsed = ScaleWeightParser.parse(line) ?: return
        val normalized = ScaleWeightParser.normalizeGrams(parsed, detectUnit(line)) ?: return
        if (normalized <= BigDecimal.ZERO) return

        _latestStableWeight.value = weightDetector.addSample(normalized)

        scaleWatchdogJob?.cancel()
        scaleWatchdogJob = scope.launch {
            kotlinx.coroutines.delay(1500L)
            _latestStableWeight.value = null
            weightDetector.reset()
        }
    }

    private fun detectUnit(text: String): String =
        when {
            Regex("""\bkg\b""").containsMatchIn(text.lowercase()) -> "kg"
            Regex("""\bmg\b""").containsMatchIn(text.lowercase()) -> "mg"
            "کیلوگرم" in text.lowercase() -> "کیلوگرم"
            "گرم" in text.lowercase() -> "گرم"
            else -> "g"
        }
}
