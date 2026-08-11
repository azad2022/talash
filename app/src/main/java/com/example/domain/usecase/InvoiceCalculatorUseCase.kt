package com.example.domain.usecase

import com.example.data.model.SaleItem
import java.math.BigDecimal
import java.math.RoundingMode

data class InvoiceSummary(
    val subtotal: Double,
    val totalDiscount: Double,
    val totalTax: Double,
    val finalPayable: Double,
    val prepayment: Double,
    val remainingBalance: Double
)

class InvoiceCalculatorUseCase {

    fun roundMoney(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toDouble()
    }

    fun roundWeight(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Back-calculates tax from tax-inclusive total amount using BigDecimal:
     * Tax = totalAmount * taxPercent / (100 + taxPercent)
     */
    fun calculateBackTax(totalAmount: Double, taxPercent: Double): Double {
        if (totalAmount <= 0.0 || taxPercent <= 0.0) return 0.0
        val totalBd = BigDecimal.valueOf(totalAmount).setScale(0, RoundingMode.HALF_UP)
        val taxRateBd = BigDecimal.valueOf(taxPercent)
        val divisorBd = BigDecimal.valueOf(100.0).add(taxRateBd)

        val taxBd = totalBd.multiply(taxRateBd).divide(divisorBd, 0, RoundingMode.HALF_UP)
        return taxBd.toDouble()
    }

    /**
     * Calculates invoice summary deterministically with BigDecimal math.
     */
    fun calculateInvoiceSummary(
        items: List<SaleItem>,
        discountAmount: Double = 0.0,
        taxPercent: Double = 9.0,
        prepayment: Double = 0.0
    ): InvoiceSummary {
        val subtotalBd = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(BigDecimal.valueOf(item.total).setScale(0, RoundingMode.HALF_UP))
        }

        val discountBd = BigDecimal.valueOf(discountAmount).setScale(0, RoundingMode.HALF_UP)
        val discountedSubtotalBd = subtotalBd.subtract(discountBd).max(BigDecimal.ZERO)

        val taxRateBd = BigDecimal.valueOf(taxPercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val taxBd = discountedSubtotalBd.multiply(taxRateBd).setScale(0, RoundingMode.HALF_UP)

        val finalPayableBd = discountedSubtotalBd.add(taxBd)
        val prepaymentBd = BigDecimal.valueOf(prepayment).setScale(0, RoundingMode.HALF_UP)
        val remainingBalanceBd = finalPayableBd.subtract(prepaymentBd).max(BigDecimal.ZERO)

        return InvoiceSummary(
            subtotal = subtotalBd.toDouble(),
            totalDiscount = discountBd.toDouble(),
            totalTax = taxBd.toDouble(),
            finalPayable = finalPayableBd.toDouble(),
            prepayment = prepaymentBd.toDouble(),
            remainingBalance = remainingBalanceBd.toDouble()
        )
    }

    /**
     * Splits remaining balance among N installments deterministically using integer BigDecimal division.
     * Guarantees that sum(installments) == remainingAmount EXACTLY by adding any remainder to the last installment.
     */
    fun calculateInstallments(remainingAmount: Double, installmentsCount: Int): List<Double> {
        if (installmentsCount <= 0 || remainingAmount <= 0.0) return emptyList()

        val totalRemBd = BigDecimal.valueOf(remainingAmount).setScale(0, RoundingMode.HALF_UP)
        val countBd = BigDecimal.valueOf(installmentsCount.toLong())

        val baseAmountBd = totalRemBd.divide(countBd, 0, RoundingMode.DOWN)
        val sumBaseBd = baseAmountBd.multiply(countBd)
        val remainderBd = totalRemBd.subtract(sumBaseBd)

        val result = MutableList(installmentsCount) { baseAmountBd.toDouble() }
        if (remainderBd > BigDecimal.ZERO) {
            val lastIdx = installmentsCount - 1
            result[lastIdx] = baseAmountBd.add(remainderBd).toDouble()
        }
        return result
    }
}

