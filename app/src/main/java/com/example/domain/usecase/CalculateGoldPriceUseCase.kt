package com.example.domain.usecase

import com.example.data.model.Product

data class GoldCalculationResult(
    val baseGoldPrice: Double,
    val wageAmount: Double,
    val profitAmount: Double,
    val priceBeforeTax: Double,
    val taxAmount: Double,
    val totalPrice: Double
)

class CalculateGoldPriceUseCase {

    /**
     * Calculates the item price using standard Iranian gold market formula:
     * 1. Base Gold Price = Weight * (18k Gold Price * (Karat / 18))
     * 2. Wage = if PERCENT then (Base Gold Price * wagePrice / 100) else (wagePrice * Weight)
     * 3. Profit = (Base Gold Price + Wage) * (profitPercent / 100)  [Default standard retailer profit: 7%]
     * 4. Tax = (Wage + Profit) * (taxPercent / 100) or (Price Before Tax * taxPercent / 100)
     * 5. Total = Price Before Tax + Tax
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

        val baseGoldPrice = weightGram * (goldPricePerGram18k * (karat.toDouble() / 18.0))
        val wageAmount = if (wageType == "PERCENT") {
            baseGoldPrice * (wagePrice / 100.0)
        } else {
            wagePrice * weightGram
        }

        val profitAmount = (baseGoldPrice + wageAmount) * (profitPercent / 100.0)
        val priceBeforeTax = baseGoldPrice + wageAmount + profitAmount
        val taxAmount = (wageAmount + profitAmount) * (taxPercent / 100.0)
        val totalPrice = priceBeforeTax + taxAmount

        return GoldCalculationResult(
            baseGoldPrice = baseGoldPrice,
            wageAmount = wageAmount,
            profitAmount = profitAmount,
            priceBeforeTax = priceBeforeTax,
            taxAmount = taxAmount,
            totalPrice = totalPrice
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
