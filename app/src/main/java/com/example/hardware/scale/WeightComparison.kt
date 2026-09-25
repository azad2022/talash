package com.example.hardware.scale

import java.math.BigDecimal

data class WeightComparison(
    val expectedGrams: BigDecimal,
    val measuredGrams: BigDecimal,
    val differenceGrams: BigDecimal,
    val withinTolerance: Boolean
)

object WeightComparisonEngine {
    fun compare(expected: BigDecimal, measured: BigDecimal, tolerance: BigDecimal = BigDecimal("0.005")): WeightComparison {
        val difference = measured.subtract(expected)
        return WeightComparison(expected, measured, difference, difference.abs() <= tolerance)
    }
}