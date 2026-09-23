package com.example.hardware.barcode

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object HardwareBarcodeBus {
    @Volatile
    var enabled: Boolean = false

    fun setEnabled(value: Boolean) { enabled = value }

    private const val MIN_LENGTH = 4
    private const val MAX_INTER_KEY_DELAY_MS = 120L

    private val _scans = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val scans = _scans.asSharedFlow()

    private val buffer = StringBuilder()
    private var lastCharAt = 0L

    @Synchronized
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled) return false
        if (event.action != KeyEvent.ACTION_UP) return true
        val now = System.currentTimeMillis()

        if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_TAB) {
            if (buffer.length >= MIN_LENGTH && now - lastCharAt <= MAX_INTER_KEY_DELAY_MS) {
                _scans.tryEmit(buffer.toString())
            }
            buffer.clear()
            lastCharAt = 0L
            enabled = false
            return true
        }

        val codePoint = event.unicodeChar
        if (codePoint == 0) return true
        if (lastCharAt > 0L && now - lastCharAt > MAX_INTER_KEY_DELAY_MS) buffer.clear()
        buffer.appendCodePoint(codePoint)
        lastCharAt = now
        return true
    }
}
