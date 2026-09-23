package com.example.hardware.scale

import com.example.hardware.core.StableWeight
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object ScaleWeightParser {
    private val numberRegex = Regex("""[-+]?\d+(?:[.,]\d+)?""")

    fun parse(text: String): BigDecimal? {
        val normalized = text.replace(',', '.').replace("٫", '.').trim()
        val matches = numberRegex.findAll(normalized).toList()
        if (matches.isEmpty()) return null
        return matches.last().value.toBigDecimalOrNull()
    }

    fun parseBytes(bytes: ByteArray, charset: java.nio.charset.Charset = Charsets.UTF_8): BigDecimal? =
        parse(bytes.toString(charset))

    fun normalizeGrams(value: BigDecimal, unit: String?): BigDecimal? =
        when (unit?.trim()?.lowercase(Locale.US)) {
            null, "", "g", "gram", "grams", "گرم" -> value
            "kg", "kilogram", "kilograms", "کیلو", "کیلوگرم" -> value.multiply(BigDecimal("1000"))
            "mg", "milligram", "milligrams" -> value.divide(BigDecimal("1000"), 6, RoundingMode.HALF_UP)
            else -> null
        }
}

class StableWeightDetector(
    private val toleranceGrams: BigDecimal = BigDecimal("0.005"),
    private val requiredStableSamples: Int = 4
) {
    private val recent = ArrayDeque<BigDecimal>()

    fun reset() { recent.clear() }

    fun addSample(value: BigDecimal): StableWeight? {
        recent.addLast(value)
        while (recent.size > requiredStableSamples) recent.removeFirst()
        if (recent.size < requiredStableSamples) return null

        val min = recent.minOrNull() ?: return null
        val max = recent.maxOrNull() ?: return null
        if (max.subtract(min).abs() > toleranceGrams) return null

        val average = recent.fold(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(recent.size.toLong()), 3, RoundingMode.HALF_UP)

        return StableWeight(average, 0L, recent.size)
    }
}
