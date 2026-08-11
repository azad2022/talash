package com.example.domain.usecase

import com.example.data.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

data class GoldCalculationResult(
    val baseGoldPriceBd: BigDecimal,
    val wageAmountBd: BigDecimal,
    val profitAmountBd: BigDecimal,
    val priceBeforeTaxBd: BigDecimal,
    val taxAmountBd: BigDecimal,
    val totalPriceBd: BigDecimal
) {
    val baseGoldPrice: Double get() = baseGoldPriceBd.toDouble()
    val wageAmount: Double get() = wageAmountBd.toDouble()
    val profitAmount: Double get() = profitAmountBd.toDouble()
    val priceBeforeTax: Double get() = priceBeforeTaxBd.toDouble()
    val taxAmount: Double get() = taxAmountBd.toDouble()
    val totalPrice: Double get() = totalPriceBd.toDouble()

    constructor(
        baseGoldPrice: Double,
        wageAmount: Double,
        profitAmount: Double,
        priceBeforeTax: Double,
        taxAmount: Double,
        totalPrice: Double
    ) : this(
        baseGoldPriceBd = BigDecimal.valueOf(baseGoldPrice),
        wageAmountBd = BigDecimal.valueOf(wageAmount),
        profitAmountBd = BigDecimal.valueOf(profitAmount),
        priceBeforeTaxBd = BigDecimal.valueOf(priceBeforeTax),
        taxAmountBd = BigDecimal.valueOf(taxAmount),
        totalPriceBd = BigDecimal.valueOf(totalPrice)
    )
}

class CalculateGoldPriceUseCase {

    fun roundMoney(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toDouble()
    }

    fun roundWeight(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).toDouble()
    }

    fun execute(
        weightGram: BigDecimal,
        karat: Int = 18,
        wagePrice: BigDecimal,
        wageType: String,
        goldPricePerGram18k: BigDecimal,
        profitPercent: BigDecimal = BigDecimal("7.0"),
        taxPercent: BigDecimal = BigDecimal("9.0")
    ): GoldCalculationResult {
        if (weightGram <= BigDecimal.ZERO || goldPricePerGram18k <= BigDecimal.ZERO) {
            return GoldCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }

        val weightBd = weightGram.setScale(3, RoundingMode.HALF_UP)
        val goldPriceBd = goldPricePerGram18k.setScale(0, RoundingMode.HALF_UP)
        val karatCoeffBd = BigDecimal.valueOf(karat.toLong()).divide(BigDecimal.valueOf(18), 10, RoundingMode.HALF_UP)

        // 1. Base Gold Price
        val baseGoldPriceBd = weightBd.multiply(goldPriceBd).multiply(karatCoeffBd).setScale(0, RoundingMode.HALF_UP)

        // 2. Wage
        val wageBd = if (wageType == "PERCENT") {
            val wagePercentBd = wagePrice.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
            baseGoldPriceBd.multiply(wagePercentBd).setScale(0, RoundingMode.HALF_UP)
        } else {
            val fixedWageBd = wagePrice.setScale(0, RoundingMode.HALF_UP)
            fixedWageBd.multiply(weightBd).setScale(0, RoundingMode.HALF_UP)
        }

        // 3. Profit = (Base Gold Price + Wage) * (profitPercent / 100)
        val profitRateBd = profitPercent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val profitBd = baseGoldPriceBd.add(wageBd).multiply(profitRateBd).setScale(0, RoundingMode.HALF_UP)

        // 4. Price before tax
        val priceBeforeTaxBd = baseGoldPriceBd.add(wageBd).add(profitBd)

        // 5. Tax = (Wage + Profit) * (taxPercent / 100)
        val taxRateBd = taxPercent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val taxBd = wageBd.add(profitBd).multiply(taxRateBd).setScale(0, RoundingMode.HALF_UP)

        // 6. Total
        val totalPriceBd = priceBeforeTaxBd.add(taxBd)

        return GoldCalculationResult(
            baseGoldPriceBd = baseGoldPriceBd,
            wageAmountBd = wageBd,
            profitAmountBd = profitBd,
            priceBeforeTaxBd = priceBeforeTaxBd,
            taxAmountBd = taxBd,
            totalPriceBd = totalPriceBd
        )
    }

    fun execute(
        weightGram: Double,
        karat: Int = 18,
        wagePrice: Double,
        wageType: String,
        goldPricePerGram18k: Double,
        profitPercent: Double = 7.0,
        taxPercent: Double = 9.0
    ): GoldCalculationResult {
        return execute(
            weightGram = BigDecimal.valueOf(weightGram),
            karat = karat,
            wagePrice = BigDecimal.valueOf(wagePrice),
            wageType = wageType,
            goldPricePerGram18k = BigDecimal.valueOf(goldPricePerGram18k),
            profitPercent = BigDecimal.valueOf(profitPercent),
            taxPercent = BigDecimal.valueOf(taxPercent)
        )
    }

    fun calculateProductPrice(
        product: Product,
        goldPricePerGram18k: BigDecimal,
        profitPercent: BigDecimal = BigDecimal("7.0"),
        taxPercent: BigDecimal = BigDecimal("9.0")
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

    fun calculateProductPrice(
        product: Product,
        goldPricePerGram18k: Double,
        profitPercent: Double = 7.0,
        taxPercent: Double = 9.0
    ): GoldCalculationResult {
        return calculateProductPrice(
            product = product,
            goldPricePerGram18k = BigDecimal.valueOf(goldPricePerGram18k),
            profitPercent = BigDecimal.valueOf(profitPercent),
            taxPercent = BigDecimal.valueOf(taxPercent)
        )
    }
}

