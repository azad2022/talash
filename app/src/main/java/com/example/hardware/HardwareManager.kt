package com.example.hardware

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.example.hardware.connection.UsbPermissionBroker
import com.example.hardware.connection.BluetoothSppTransport
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PairedBluetoothDevice(
    val name: String,
    val address: String
)

class HardwareManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _connectionState = MutableStateFlow(HardwareConnectionState.DISCONNECTED)
    val connectionState: StateFlow<HardwareConnectionState> = _connectionState

    private val _connectedDevice = MutableStateFlow<HardwareDevice?>(null)
    val connectedDevice: StateFlow<HardwareDevice?> = _connectedDevice

    private val _latestStableWeight = MutableStateFlow<StableWeight?>(null)
    val latestStableWeight: StateFlow<StableWeight?> = _latestStableWeight

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var transport: HardwareTransport? = null
    private var readerJob: Job? = null
    private var transportStateJob: Job? = null
    private val weightDetector = StableWeightDetector()
    private val inputBuffer = StringBuilder()

    fun pairedBluetoothDevices(): List<PairedBluetoothDevice> {
        if (android.os.Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices
            .map { PairedBluetoothDevice(it.name ?: "Bluetooth", it.address) }
            .sortedBy { it.name.lowercase() }
    }

    fun connectBluetooth(
        name: String,
        address: String,
        type: HardwareDeviceType
    ) {
        scope.launch {
            disconnect()
            val device = HardwareDevice(
                id = address,
                name = name,
                type = type,
                transport = HardwareTransportType.BLUETOOTH_SPP,
                address = address
            )
            val bt = BluetoothSppTransport(context)
            transport = bt
            val result = bt.connect(device)
            when (result) {
                is HardwareResult.Success -> {
                    _lastError.value = null
                    _connectedDevice.value = device
                    _connectionState.value = HardwareConnectionState.CONNECTED
                    observeTransportState(bt)
                    startReader(bt, type)
                }
                is HardwareResult.Failure -> {
                    _lastError.value = result.message
                    _connectionState.value = HardwareConnectionState.ERROR
                    _connectedDevice.value = null
                }
            }
        }
    }

    fun usbSerialDevices(): List<HardwareDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).map { driver ->
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

    fun connectUsb(
        deviceId: Int,
        name: String,
        type: HardwareDeviceType,
        settings: SerialConnectionSettings = SerialConnectionSettings()
    ) {
        scope.launch {
            disconnect()
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val usbDevice = usbManager.deviceList.values.firstOrNull { it.deviceId == deviceId }
            if (usbDevice == null) {
                _lastError.value = "دستگاه USB پیدا نشد."
                _connectionState.value = HardwareConnectionState.ERROR
                return@launch
            }
            if (!UsbPermissionBroker.ensurePermission(context, usbDevice)) {
                _lastError.value = "مجوز دسترسی به دستگاه USB صادر نشد."
                _connectionState.value = HardwareConnectionState.ERROR
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
            val usb = UsbSerialTransport(context, settings)
            transport = usb
            when (val result = usb.connect(device)) {
                is HardwareResult.Success -> {
                    _lastError.value = null
                    _connectedDevice.value = device
                    _connectionState.value = HardwareConnectionState.CONNECTED
                    observeTransportState(usb)
                    startReader(usb, type)
                }
                is HardwareResult.Failure -> {
                    _lastError.value = result.message
                    _connectionState.value = HardwareConnectionState.ERROR
                    _connectedDevice.value = null
                }
            }
        }
    }

    fun writeFor(
        expectedType: HardwareDeviceType,
        bytes: ByteArray,
        onResult: (HardwareResult<Unit>) -> Unit = {}
    ) {
        val active = transport
        val device = _connectedDevice.value
        if (active == null || device == null) {
            onResult(HardwareResult.Failure("هیچ دستگاه سخت‌افزاری متصل نیست."))
            return
        }
        if (device.type != expectedType) {
            val failure = HardwareResult.Failure("دستگاه متصل برای این عملیات مناسب نیست.")
            _lastError.value = failure.message
            onResult(failure)
            return
        }
        scope.launch {
            val result = active.write(bytes)
            if (result is HardwareResult.Failure) _lastError.value = result.message
            onResult(result)
        }
    }

    suspend fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        transportStateJob?.cancel()
        transportStateJob = null
        weightDetector.reset()
        inputBuffer.clear()
        _latestStableWeight.value = null

        val active = transport
        transport = null
        active?.disconnect()
        _connectedDevice.value = null
        _connectionState.value = HardwareConnectionState.DISCONNECTED
    }

    private fun observeTransportState(active: HardwareTransport) {
        transportStateJob?.cancel()
        transportStateJob = scope.launch {
            active.state.collect { state ->
                _connectionState.value = state
                if (state == HardwareConnectionState.ERROR && _lastError.value.isNullOrBlank()) {
                    _lastError.value = "ارتباط با دستگاه قطع یا دچار خطا شد."
                }
            }
        }
    }

    private fun startReader(active: HardwareTransport, type: HardwareDeviceType) {
        readerJob?.cancel()
        if (type != HardwareDeviceType.SCALE) return

        readerJob = scope.launch(Dispatchers.IO) {
            active.incomingBytes().collect { bytes ->
                decodeScaleBytes(bytes)
            }
        }
    }

    private fun decodeScaleBytes(bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
            .replace('\u0000', ' ')

        inputBuffer.append(text)
        var consumed = 0

        while (true) {
            val lineEnd = inputBuffer.indexOfAny(charArrayOf('\r', '\n'))
            if (lineEnd < 0) break

            val line = inputBuffer.substring(consumed, lineEnd).trim()
            consumed = lineEnd + 1
            if (line.isNotBlank()) feedScaleValue(line)

            while (consumed < inputBuffer.length &&
                (inputBuffer[consumed] == '\r' || inputBuffer[consumed] == '\n')
            ) {
                consumed++
            }

            inputBuffer.delete(0, consumed)
            consumed = 0
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
        val stable = weightDetector.addSample(normalized) ?: return
        _latestStableWeight.value = stable
    }

    private fun detectUnit(text: String): String? {
        val lowered = text.lowercase()
        return when {
            Regex("""\bkg\b""").containsMatchIn(lowered) -> "kg"
            Regex("""\bmg\b""").containsMatchIn(lowered) -> "mg"
            "کیلوگرم" in lowered -> "کیلوگرم"
            "گرم" in lowered -> "گرم"
            else -> "g"
        }
    }
}
