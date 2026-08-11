package com.example.domain.usecase

import com.example.data.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

data class GoldCalculationResult(
    val baseGoldPrice: Double,
    val wageAmount: Double,
    val profitAmount: Double,
    val priceBeforeTax: Double,
    val taxAmount: Double,
    val totalPrice: Double
)

class CalculateGoldPriceUseCase {

    fun roundMoney(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toDouble()
    }

    fun roundWeight(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates the item price using standard Iranian gold market formula with BigDecimal precision:
     * 1. Weight: rounded to 3 decimal places (0.001 g)
     * 2. Base Gold Price = Weight * (18k Gold Price * (Karat / 18))
     * 3. Wage = if PERCENT then (Base Gold Price * wagePrice / 100) else (wagePrice * Weight)
     * 4. Profit = (Base Gold Price + Wage) * (profitPercent / 100)
     * 5. Price Before Tax = Base Gold Price + Wage + Profit
     * 6. Tax = (Wage + Profit) * (taxPercent / 100)
     * 7. Total = Price Before Tax + Tax
     */
    fun execute(
        weightGram: Double,
        karat: Int = 18,
        wagePrice: Double,
        wageType: String, // "FIXED" or "PERCENT"
        goldPricePerGram18k: Double,
        profitPercent: Double = 7.0,
        taxPercent: Double = 9.0
    ): GoldCalculationResult {
        if (weightGram <= 0.0 || goldPricePerGram18k <= 0.0) {
            return GoldCalculationResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val weightBd = BigDecimal.valueOf(weightGram).setScale(3, RoundingMode.HALF_UP)
        val goldPriceBd = BigDecimal.valueOf(goldPricePerGram18k).setScale(0, RoundingMode.HALF_UP)
        val karatCoeffBd = BigDecimal.valueOf(karat.toLong()).divide(BigDecimal.valueOf(18), 10, RoundingMode.HALF_UP)

        // 1. Base Gold Price
        val baseGoldPriceBd = weightBd.multiply(goldPriceBd).multiply(karatCoeffBd).setScale(0, RoundingMode.HALF_UP)

        // 2. Wage
        val wageBd = if (wageType == "PERCENT") {
            val wagePercentBd = BigDecimal.valueOf(wagePrice).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
            baseGoldPriceBd.multiply(wagePercentBd).setScale(0, RoundingMode.HALF_UP)
        } else {
            val fixedWageBd = BigDecimal.valueOf(wagePrice).setScale(0, RoundingMode.HALF_UP)
            fixedWageBd.multiply(weightBd).setScale(0, RoundingMode.HALF_UP)
        }

        // 3. Profit = (Base Gold Price + Wage) * (profitPercent / 100)
        val profitRateBd = BigDecimal.valueOf(profitPercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val profitBd = baseGoldPriceBd.add(wageBd).multiply(profitRateBd).setScale(0, RoundingMode.HALF_UP)

        // 4. Price before tax
        val priceBeforeTaxBd = baseGoldPriceBd.add(wageBd).add(profitBd)

        // 5. Tax = (Wage + Profit) * (taxPercent / 100)
        val taxRateBd = BigDecimal.valueOf(taxPercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val taxBd = wageBd.add(profitBd).multiply(taxRateBd).setScale(0, RoundingMode.HALF_UP)

        // 6. Total
        val totalPriceBd = priceBeforeTaxBd.add(taxBd)

        return GoldCalculationResult(
            baseGoldPrice = baseGoldPriceBd.toDouble(),
            wageAmount = wageBd.toDouble(),
            profitAmount = profitBd.toDouble(),
            priceBeforeTax = priceBeforeTaxBd.toDouble(),
            taxAmount = taxBd.toDouble(),
            totalPrice = totalPriceBd.toDouble()
        )
    }

    fun calculateProductPrice(
        product: Product,
        goldPricePerGram18k: Double,
        profitPercent: Double = 7.0,
        taxPercent: Double = 9.0
    ): GoldCalculationResult {
        return execute(
            weightGram = product.weightGram,
            karat = product.karat,
            wagePrice = product.wagePrice,
            wageType = product.wageType,
            goldPricePerGram18k = goldPricePerGram18k,
            profitPercent = profitPercent,
            taxPercent = taxPercent
        )
    }
}

