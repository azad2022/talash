package com.example.repository

import com.example.data.repository.ShopRepository
import com.example.domain.usecase.CalculateGoldPriceUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CategoryAndTaxSettingsTest {

    private lateinit var goldCalculator: CalculateGoldPriceUseCase

    @Before
    fun setUp() {
        goldCalculator = CalculateGoldPriceUseCase()
    }

    @Test
    fun testDefaultCategoriesIncludeStandardIranianGoldCategories() {
        val defaults = ShopRepository.DEFAULT_CATEGORIES
        assertTrue(defaults.contains("انگشتر"))
        assertTrue(defaults.contains("گردنبند"))
        assertTrue(defaults.contains("گوشواره"))
        assertTrue(defaults.contains("دستبند"))
        assertTrue(defaults.contains("النگو"))
        assertTrue(defaults.contains("سکه"))
        assertTrue(defaults.contains("شمش"))
        assertTrue(defaults.contains("طلای ۲۴ عیار"))
        assertTrue(defaults.contains("طلای آبشده نقدی"))
        assertTrue(defaults.contains("طلای متفرقه و مستعمل"))
        assertTrue(defaults.contains("سکه امامی"))
        assertTrue(defaults.contains("سکه بهار آزادی"))
        assertEquals(15, defaults.size)
    }

    @Test
    fun testCategoryAdditionAndDeduplication() {
        val list = ShopRepository.DEFAULT_CATEGORIES.toMutableList()
        val newCat = "پلاک و مدال"
        assertFalse(list.contains(newCat))

        // Add category
        list.add(newCat)
        assertTrue(list.contains(newCat))
        assertEquals(16, list.size)

        // Duplicate prevention logic
        val isDuplicate = list.any { it.equals(newCat, ignoreCase = true) }
        assertTrue(isDuplicate)
    }

    @Test
    fun testCategoryEditing() {
        val list = ShopRepository.DEFAULT_CATEGORIES.toMutableList()
        val oldCat = "النگو"
        val newCat = "النگو و تک‌پوش"

        val index = list.indexOfFirst { it.equals(oldCat, ignoreCase = true) }
        assertTrue(index >= 0)

        list[index] = newCat
        assertFalse(list.contains(oldCat))
        assertTrue(list.contains(newCat))
    }

    @Test
    fun testCategoryDeletionAndResetToDefault() {
        val list = ShopRepository.DEFAULT_CATEGORIES.toMutableList()
        assertTrue(list.contains("گوشواره"))

        // Delete "گوشواره"
        list.removeIf { it.equals("گوشواره", ignoreCase = true) }
        assertFalse(list.contains("گوشواره"))
        assertEquals(14, list.size)

        // Reset to factory defaults restores missing category
        val restored = ShopRepository.DEFAULT_CATEGORIES
        assertTrue(restored.contains("گوشواره"))
        assertEquals(15, restored.size)
    }

    @Test
    fun testTaxCalculationWithDefaultAndCustomRates() {
        val weight = 10.0 // 10 grams
        val goldPricePerGram = 4_000_000.0 // 4,000,000 Tomans
        val wagePercent = 15.0 // 15% wage
        val profitPercent = 7.0 // 7% profit

        // 1. Default Iranian legal gold VAT: 9.0%
        val defaultResult = goldCalculator.execute(
            weightGram = weight,
            karat = 18,
            wagePrice = wagePercent,
            wageType = "PERCENT",
            goldPricePerGram18k = goldPricePerGram,
            profitPercent = profitPercent,
            taxPercent = 9.0
        )

        // Gold raw value = 10 * 4,000,000 = 40,000,000
        assertEquals(40_000_000.0, defaultResult.baseGoldPrice, 0.01)
        // Wage = 15% of 40,000,000 = 6,000,000
        assertEquals(6_000_000.0, defaultResult.wageAmount, 0.01)
        // Profit = 7% of (40,000,000 + 6,000,000) = 3,220,000
        assertEquals(3_220_000.0, defaultResult.profitAmount, 0.01)
        // Tax (VAT) applies ONLY to (Wage + Profit) = (6,000,000 + 3,220,000) * 9% = 829,800
        assertEquals(829_800.0, defaultResult.taxAmount, 0.01)

        // 2. Custom tax rate changed by user to 10.0%
        val customResult = goldCalculator.execute(
            weightGram = weight,
            karat = 18,
            wagePrice = wagePercent,
            wageType = "PERCENT",
            goldPricePerGram18k = goldPricePerGram,
            profitPercent = profitPercent,
            taxPercent = 10.0
        )
        // Tax with 10% = 9,220,000 * 10% = 922,000
        assertEquals(922_000.0, customResult.taxAmount, 0.01)

        // 3. User sets tax to 0% (tax-free scenario)
        val zeroTaxResult = goldCalculator.execute(
            weightGram = weight,
            karat = 18,
            wagePrice = wagePercent,
            wageType = "PERCENT",
            goldPricePerGram18k = goldPricePerGram,
            profitPercent = profitPercent,
            taxPercent = 0.0
        )
        assertEquals(0.0, zeroTaxResult.taxAmount, 0.01)
    }
}
