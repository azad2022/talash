package com.example.hardware.connection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.hardware.core.HardwareConnectionState
import com.example.hardware.core.HardwareDevice
import com.example.hardware.core.HardwareResult
import com.example.hardware.core.HardwareTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothSppTransport(private val context: Context) : HardwareTransport {
    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val _state = MutableStateFlow(HardwareConnectionState.DISCONNECTED)
    override val state: StateFlow<HardwareConnectionState> = _state
    private val incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
    private var socket: BluetoothSocket? = null

    override suspend fun connect(device: HardwareDevice): HardwareResult<Unit> {
        val address = device.address?.trim().orEmpty()
        if (address.isEmpty()) return HardwareResult.Failure("نشانی Bluetooth دستگاه مشخص نشده است.")
        if (!hasConnectPermission()) return HardwareResult.Failure("مجوز اتصال به دستگاه‌های نزدیک صادر نشده است.")

        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return HardwareResult.Failure("Bluetooth در این دستگاه در دسترس نیست.")

        _state.value = HardwareConnectionState.CONNECTING
        return withContext(Dispatchers.IO) {
            try {
                adapter.cancelDiscovery()
                val newSocket = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(SPP_UUID)
                newSocket.connect()
                socket = newSocket
                _state.value = HardwareConnectionState.CONNECTED
                startReader(newSocket)
                HardwareResult.Success(Unit)
            } catch (e: Exception) {
                closeQuietly()
                _state.value = HardwareConnectionState.ERROR
                HardwareResult.Failure(
                    "اتصال Bluetooth ناموفق بود: ${{e.localizedMessage ?: e.message ?: "خطای ناشناخته"}",
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
                val active = socket ?: return@withContext HardwareResult.Failure("دستگاه Bluetooth متصل نیست.")
                active.outputStream.write(bytes)
                active.outputStream.flush()
                HardwareResult.Success(Unit)
            } catch (e: IOException) {
                closeQuietly()
                _state.value = HardwareConnectionState.ERROR
                HardwareResult.Failure("ارسال اطلاعات به Bluetooth ناموفق بود.", e)
            }
        }
    }

    override fun incomingBytes(): Flow<ByteArray> = incoming.asSharedFlow()

    private fun startReader(activeSocket: BluetoothSocket) {
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(2048)
            try {
                while (currentCoroutineContext().isActive && activeSocket.isConnected) {
                    val read = activeSocket.inputStream.read(buffer)
                    if (read <= 0) break
                    incoming.emit(buffer.copyOf(read))
                }
            } catch (_: Exception) {
                if (_state.value == HardwareConnectionState.CONNECTED) _state.value = HardwareConnectionState.ERROR
            }
        }
    }

    private fun closeQuietly() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    private fun hasConnectPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
}
