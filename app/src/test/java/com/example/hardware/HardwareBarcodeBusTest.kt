package com.example.hardware

import android.view.KeyEvent
import com.example.hardware.barcode.HardwareBarcodeBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareBarcodeBusTest {
    @Test
    fun inactive_bus_does_not_consume_back() {
        HardwareBarcodeBus.setEnabled(false)
        val event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
        assertFalse(HardwareBarcodeBus.onKeyEvent(event))
    }

    @Test
    fun active_bus_does_not_consume_back() {
        HardwareBarcodeBus.setEnabled(true)
        val event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
        assertFalse(HardwareBarcodeBus.onKeyEvent(event))
        HardwareBarcodeBus.setEnabled(false)
    }

    @Test
    fun active_bus_consumes_scan_and_emits_value() = runTest {
        HardwareBarcodeBus.setEnabled(true)
        val events = listOf(
            '1' to KeyEvent.KEYCODE_1,
            '2' to KeyEvent.KEYCODE_2,
            '3' to KeyEvent.KEYCODE_3,
            '4' to KeyEvent.KEYCODE_4
        )
        events.forEach { (char, keyCode) ->
            HardwareBarcodeBus.onKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_UP,
                    keyCode
                )
            )
        }
        HardwareBarcodeBus.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        assertEquals("1234", HardwareBarcodeBus.scans.first())
        HardwareBarcodeBus.setEnabled(false)
    }
}
