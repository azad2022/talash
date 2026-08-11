package com.example.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testValidConversion() {
        val original = BigDecimal("12345678.901")
        val asString = converters.fromBigDecimal(original)
        assertEquals("12345678.901", asString)

        val restored = converters.toBigDecimal(asString)
        assertEquals(original, restored)

        // Null and blank handling
        assertNull(converters.fromBigDecimal(null))
        assertNull(converters.toBigDecimal(null))
        assertNull(converters.toBigDecimal(""))
        assertNull(converters.toBigDecimal("   "))
    }

    @Test
    fun testInvalidCorruptedValue() {
        val corruptedValues = listOf("invalid_number", "12.34.56", "abc", "100_000_toman")

        for (corrupted in corruptedValues) {
            try {
                converters.toBigDecimal(corrupted)
                fail("Expected Exception when parsing corrupted value: '$corrupted'")
            } catch (e: Exception) {
                // Must fail safely instead of returning BigDecimal.ZERO or null
                assert(e is NumberFormatException || e is IllegalArgumentException)
            }
        }
    }
}
