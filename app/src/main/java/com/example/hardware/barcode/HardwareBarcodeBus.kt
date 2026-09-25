package com.example.hardware.barcode

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object HardwareBarcodeBus {
    @Volatile
    var enabled: Boolean = false
        private set

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            synchronized(this) {
                buffer.clear()
                lastCharAt = 0L
            }
        }
    }

    private const val MIN_LENGTH = 4
    private const val MAX_INTER_KEY_DELAY_MS = 120L

    private val _scans = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val scans = _scans.asSharedFlow()

    private val buffer = StringBuilder()
    private var lastCharAt = 0L

    @Synchronized
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled) return false

        val isTerminator =
            event.keyCode == KeyEvent.KEYCODE_ENTER ||
                event.keyCode == KeyEvent.KEYCODE_TAB
        val codePoint = if (event.unicodeChar != 0) {
            event.unicodeChar
        } else when (event.keyCode) {
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0'.code + (event.keyCode - KeyEvent.KEYCODE_0)
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> (if (event.isShiftPressed) 'A' else 'a').code + (event.keyCode - KeyEvent.KEYCODE_A)
            KeyEvent.KEYCODE_MINUS -> '-'.code
            KeyEvent.KEYCODE_PERIOD -> '.'.code
            KeyEvent.KEYCODE_SLASH -> '/'.code
            else -> 0
        }
        val isPrintable = event.isPrintingKey || codePoint != 0

        if (!isTerminator && !isPrintable) return false

        if (event.action != KeyEvent.ACTION_UP) {
            return true
        }

        val now = System.currentTimeMillis()

        if (isTerminator) {
            if (buffer.length >= MIN_LENGTH && (lastCharAt == 0L || now - lastCharAt <= MAX_INTER_KEY_DELAY_MS)) {
                _scans.tryEmit(buffer.toString())
            }
            buffer.clear()
            lastCharAt = 0L
            return true
        }

        if (codePoint == 0) return true

        if (lastCharAt > 0L && now - lastCharAt > MAX_INTER_KEY_DELAY_MS) {
            buffer.clear()
        }

        buffer.appendCodePoint(codePoint)
        lastCharAt = now
        return true
    }
}
