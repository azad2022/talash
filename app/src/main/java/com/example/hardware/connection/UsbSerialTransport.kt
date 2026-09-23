package com.example.hardware.connection

import android.content.Context
import android.hardware.usb.UsbManager
import com.example.hardware.core.HardwareConnectionState
import com.example.hardware.core.HardwareDevice
import com.example.hardware.core.HardwareResult
import com.example.hardware.core.HardwareTransport
import com.example.hardware.core.SerialConnectionSettings
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsbSerialTransport(
    private val context: Context,
    private val settings: SerialConnectionSettings = SerialConnectionSettings()
) : HardwareTransport {

    private val _state = MutableStateFlow(HardwareConnectionState.DISCONNECTED)
    override val state: StateFlow<HardwareConnectionState> = _state
    private val incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
    private var port: UsbSerialPort? = null

    override suspend fun connect(device: HardwareDevice): HardwareResult<Unit> {
        val deviceId = device.id.toIntOrNull()
            ?: return HardwareResult.Failure("شناسه USB دستگاه نامعتبر است.")

        _state.value = HardwareConnectionState.CONNECTING
        return withContext(Dispatchers.IO) {
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val usbDevice = usbManager.deviceList.values.firstOrNull { it.deviceId == deviceId }
                    ?: return@withContext HardwareResult.Failure("دستگاه USB پیدا نشد.")

                if (!usbManager.hasPermission(usbDevice)) {
                    _state.value = HardwareConnectionState.ERROR
                    return@withContext HardwareResult.Failure("مجوز USB این دستگاه هنوز صادر نشده است.")
                }

                val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                    .firstOrNull { it.device.deviceId == deviceId }
                    ?: return@withContext HardwareResult.Failure("درایور USB Serial سازگار پیدا نشد.")

                val connection = usbManager.openDevice(driver.device)
                    ?: return@withContext HardwareResult.Failure("امکان بازکردن ارتباط USB وجود ندارد.")

                val serialPort = driver.ports.firstOrNull()
                    ?: return@withContext HardwareResult.Failure("پورت Serial در دستگاه USB یافت نشد.")

                serialPort.open(connection)
                serialPort.setParameters(settings.baudRate, settings.dataBits, settings.stopBits, settings.parity)
                port = serialPort
                _state.value = HardwareConnectionState.CONNECTED
                startReader(serialPort)
                HardwareResult.Success(Unit)
            } catch (e: Exception) {
                closeQuietly()
                _state.value = HardwareConnectionState.ERROR
                HardwareResult.Failure(
                    "اتصال USB Serial ناموفق بود: ${{e.localizedMessage ?: e.message ?: "خطای ناشناخته"}",
                    e
                )
            }
        }
    }

    override suspend fun disconnect() {
        closeQuietly()
        _state.value = HardwareConnectionState.DISCONNECTED
    }

    override suspend fun write(bytes: ByteArray): HardwareResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val active = port ?: return@withContext HardwareResult.Failure("دستگاه USB متصل نیست.")
                active.write(bytes, 2000)
                HardwareResult.Success(Unit)
            } catch (e: Exception) {
                closeQuietly()
                _state.value = HardwareConnectionState.ERROR
                HardwareResult.Failure("ارسال اطلاعات به USB ناموفق بود.", e)
            }
        }
    }

    override fun incomingBytes(): Flow<ByteArray> = incoming

    private fun startReader(activePort: UsbSerialPort) {
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(4096)
            try {
                while (currentCoroutineContext().isActive && _state.value == HardwareConnectionState.CONNECTED) {
                    val read = activePort.read(buffer, 1000)
                    if (read > 0) incoming.emit(buffer.copyOf(read))
                }
            } catch (_: Exception) {
                if (_state.value == HardwareConnectionState.CONNECTED) _state.value = HardwareConnectionState.ERROR
            }
        }
    }

    private fun closeQuietly() {
        try { port?.close() } catch (_: Exception) {}
        port = null
    }
}
