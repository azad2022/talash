package com.example.hardware

import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.example.hardware.barcode.HardwareBarcodeBus
import kotlinx.coroutines.async
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
        val nextScan = async { HardwareBarcodeBus.scans.first() }
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents("1234")
            ?: error("Could not create virtual keyboard events")
        events.forEach { event ->
            HardwareBarcodeBus.onKeyEvent(event)
        }
        HardwareBarcodeBus.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        assertEquals("1234", nextScan.await())
        HardwareBarcodeBus.setEnabled(false)
    }
}
