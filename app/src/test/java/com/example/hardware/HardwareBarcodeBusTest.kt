package com.example.hardware

import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.example.hardware.barcode.HardwareBarcodeBus
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
        val emitted = mutableListOf<String>()
        backgroundScope.launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            HardwareBarcodeBus.scans.collect { emitted.add(it) }
        }
        val events = arrayOf(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_1),
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_2),
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_3),
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_4),
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
        )
        events.forEach { event ->
            HardwareBarcodeBus.onKeyEvent(event)
        }
        assertEquals(listOf("1234"), emitted)
        HardwareBarcodeBus.setEnabled(false)
    }
}
