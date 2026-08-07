package com.example.domain

import com.example.domain.usecase.CalculateGoldPriceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateGoldPriceUseCaseTest {

    private lateinit var useCase: CalculateGoldPriceUseCase

    @Before
    fun setUp() {
        useCase = CalculateGoldPriceUseCase()
    }

    @Test
    fun execute_fixedWage_calculatesPriceCorrectly() {
        val weightGram = 10.0
        val goldPricePerGram18k = 3_000_000.0 // 3 million toman per gram
        val wagePrice = 100_000.0 // 100,000 toman fixed wage per gram
        val profitPercent = 7.0
        val taxPercent = 9.0

        val result = useCase.execute(
            weightGram = weightGram,
            karat = 18,
            wagePrice = wagePrice,
            wageType = "FIXED",
            goldPricePerGram18k = goldPricePerGram18k,
            profitPercent = profitPercent,
            taxPercent = taxPercent
        )

        // Base gold price = 10 * 3,000,000 = 30,000,000
        assertEquals(30_000_000.0, result.baseGoldPrice, 0.01)

        // Wage amount = 10 * 100,000 = 1,000,000
        assertEquals(1_000_000.0, result.wageAmount, 0.01)

        // Profit = (30,000,000 + 1,000,000) * 0.07 = 2,170,000
        assertEquals(2_170_000.0, result.profitAmount, 0.01)

        // Price before tax = 30m + 1m + 2.17m = 33,170,000
        assertEquals(33_170_000.0, result.priceBeforeTax, 0.01)

        // Tax = (1m + 2.17m) * 0.09 = 3,170,000 * 0.09 = 285,300
        assertEquals(285_300.0, result.taxAmount, 0.01)

        // Total price = 33,170,000 + 285,300 = 33,455,300
        assertEquals(33_455_300.0, result.totalPrice, 0.01)
    }

    @Test
    fun execute_percentWage_calculatesPriceCorrectly() {
        val weightGram = 5.0
        val goldPricePerGram18k = 4_000_000.0
        val wagePricePercent = 10.0 // 10% wage on base gold price
        val profitPercent = 7.0
        val taxPercent = 9.0

        val result = useCase.execute(
            weightGram = weightGram,
            karat = 18,
            wagePrice = wagePricePercent,
            wageType = "PERCENT",
            goldPricePerGram18k = goldPricePerGram18k,
            profitPercent = profitPercent,
            taxPercent = taxPercent
        )

        // Base gold price = 5 * 4,000,000 = 20,000,000
        assertEquals(20_000_000.0, result.baseGoldPrice, 0.01)

        // Wage = 20,000,000 * 0.10 = 2,000,000
        assertEquals(2_000_000.0, result.wageAmount, 0.01)

        assertTrue(result.totalPrice > result.baseGoldPrice)
    }

    @Test
    fun execute_zeroOrNegativeWeight_returnsZero() {
        val result = useCase.execute(
            weightGram = 0.0,
            karat = 18,
            wagePrice = 50_000.0,
            wageType = "FIXED",
            goldPricePerGram18k = 3_500_000.0
        )

        assertEquals(0.0, result.totalPrice, 0.01)
    }
}
