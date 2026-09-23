package com.example.hardware.core

enum class HardwareDeviceType { SCALE, BARCODE_SCANNER, RECEIPT_PRINTER, LABEL_PRINTER }
enum class HardwareTransportType { BLUETOOTH_SPP, USB_SERIAL, KEYBOARD_HID }
enum class HardwareConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class SerialConnectionSettings(
    val baudRate: Int = 9600,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Int = 0
)

data class HardwareDevice(
    val id: String,
    val name: String,
    val type: HardwareDeviceType,
    val transport: HardwareTransportType,
    val address: String? = null,
    val vendorId: Int? = null,
    val productId: Int? = null
)

data class StableWeight(
    val grams: java.math.BigDecimal,
    val stableForMs: Long,
    val samples: Int
)

sealed interface HardwareResult<out T> {
    data class Success<T>(val value: T) : HardwareResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : HardwareResult<Nothing>
}
