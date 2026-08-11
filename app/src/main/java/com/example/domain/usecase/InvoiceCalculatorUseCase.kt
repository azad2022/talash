package com.example.domain.usecase

import com.example.data.model.SaleItem
import java.math.BigDecimal
import java.math.RoundingMode

data class InvoiceSummary(
    val subtotalBd: BigDecimal,
    val totalDiscountBd: BigDecimal,
    val totalTaxBd: BigDecimal,
    val finalPayableBd: BigDecimal,
    val prepaymentBd: BigDecimal,
    val remainingBalanceBd: BigDecimal
) {
    val subtotal: Double get() = subtotalBd.toDouble()
    val totalDiscount: Double get() = totalDiscountBd.toDouble()
    val totalTax: Double get() = totalTaxBd.toDouble()
    val finalPayable: Double get() = finalPayableBd.toDouble()
    val prepayment: Double get() = prepaymentBd.toDouble()
    val remainingBalance: Double get() = remainingBalanceBd.toDouble()

    constructor(
        subtotal: Double,
        totalDiscount: Double,
        totalTax: Double,
        finalPayable: Double,
        prepayment: Double,
        remainingBalance: Double
    ) : this(
        subtotalBd = BigDecimal.valueOf(subtotal),
        totalDiscountBd = BigDecimal.valueOf(totalDiscount),
        totalTaxBd = BigDecimal.valueOf(totalTax),
        finalPayableBd = BigDecimal.valueOf(finalPayable),
        prepaymentBd = BigDecimal.valueOf(prepayment),
        remainingBalanceBd = BigDecimal.valueOf(remainingBalance)
    )
}

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
    fun calculateBackTax(totalAmount: BigDecimal, taxPercent: BigDecimal): BigDecimal {
        if (totalAmount <= BigDecimal.ZERO || taxPercent <= BigDecimal.ZERO) return BigDecimal.ZERO
        val totalBd = totalAmount.setScale(0, RoundingMode.HALF_UP)
        val divisorBd = BigDecimal.valueOf(100).add(taxPercent)

        return totalBd.multiply(taxPercent).divide(divisorBd, 0, RoundingMode.HALF_UP)
    }

    fun calculateBackTax(totalAmount: Double, taxPercent: Double): Double {
        return calculateBackTax(BigDecimal.valueOf(totalAmount), BigDecimal.valueOf(taxPercent)).toDouble()
    }

    /**
     * Calculates invoice summary deterministically with BigDecimal math.
     */
    fun calculateInvoiceSummary(
        items: List<SaleItem>,
        discountAmount: BigDecimal = BigDecimal.ZERO,
        taxPercent: BigDecimal = BigDecimal("9.0"),
        prepayment: BigDecimal = BigDecimal.ZERO
    ): InvoiceSummary {
        val subtotalBd = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.total.setScale(0, RoundingMode.HALF_UP))
        }

        val discountBd = discountAmount.setScale(0, RoundingMode.HALF_UP)
        val discountedSubtotalBd = subtotalBd.subtract(discountBd).max(BigDecimal.ZERO)

        val taxRateBd = taxPercent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val taxBd = discountedSubtotalBd.multiply(taxRateBd).setScale(0, RoundingMode.HALF_UP)

        val finalPayableBd = discountedSubtotalBd.add(taxBd)
        val prepaymentBd = prepayment.setScale(0, RoundingMode.HALF_UP)
        val remainingBalanceBd = finalPayableBd.subtract(prepaymentBd).max(BigDecimal.ZERO)

        return InvoiceSummary(
            subtotalBd = subtotalBd,
            totalDiscountBd = discountBd,
            totalTaxBd = taxBd,
            finalPayableBd = finalPayableBd,
            prepaymentBd = prepaymentBd,
            remainingBalanceBd = remainingBalanceBd
        )
    }

    fun calculateInvoiceSummary(
        items: List<SaleItem>,
        discountAmount: Double = 0.0,
        taxPercent: Double = 9.0,
        prepayment: Double = 0.0
    ): InvoiceSummary {
        return calculateInvoiceSummary(
            items = items,
            discountAmount = BigDecimal.valueOf(discountAmount),
            taxPercent = BigDecimal.valueOf(taxPercent),
            prepayment = BigDecimal.valueOf(prepayment)
        )
    }

    /**
     * Splits remaining balance among N installments deterministically using integer BigDecimal division.
     * Guarantees that sum(installments) == remainingAmount EXACTLY by adding any remainder to the last installment.
     */
    fun calculateInstallments(remainingAmount: BigDecimal, installmentsCount: Int): List<BigDecimal> {
        if (installmentsCount <= 0 || remainingAmount <= BigDecimal.ZERO) return emptyList()

        val totalRemBd = remainingAmount.setScale(0, RoundingMode.HALF_UP)
        val countBd = BigDecimal.valueOf(installmentsCount.toLong())

        val baseAmountBd = totalRemBd.divide(countBd, 0, RoundingMode.DOWN)
        val sumBaseBd = baseAmountBd.multiply(countBd)
        val remainderBd = totalRemBd.subtract(sumBaseBd)

        val result = MutableList(installmentsCount) { baseAmountBd }
        if (remainderBd > BigDecimal.ZERO) {
            val lastIdx = installmentsCount - 1
            result[lastIdx] = baseAmountBd.add(remainderBd)
        }
        return result
    }

    fun calculateInstallments(remainingAmount: Double, installmentsCount: Int): List<Double> {
        return calculateInstallments(BigDecimal.valueOf(remainingAmount), installmentsCount).map { it.toDouble() }
    }
}

