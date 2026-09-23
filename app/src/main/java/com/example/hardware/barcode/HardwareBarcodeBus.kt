package com.example.hardware.barcode

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object HardwareBarcodeBus {
    private const val MIN_LENGTH = 4
    private const val MAX_INTER_KEY_DELAY_MS = 120L

    private val _scans = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val scans = _scans.asSharedFlow()

    private val buffer = StringBuilder()
    private var lastCharAt = 0L

    @Synchronized
    fun onKeyEvent(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_UP) return
        val now = System.currentTimeMillis()

        if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_TAB) {
            if (buffer.length >= MIN_LENGTH && now - lastCharAt <= MAX_INTER_KEY_DELAY_MS) {
                _scans.tryEmit(buffer.toString())
            }
            buffer.clear()
            lastCharAt = 0L
            return
        }

        val codePoint = event.unicodeChar
        if (codePoint == 0) return
        if (lastCharAt > 0L && now - lastCharAt > MAX_INTER_KEY_DELAY_MS) buffer.clear()
        buffer.appendCodePoint(codePoint)
        lastCharAt = now
    }
}
