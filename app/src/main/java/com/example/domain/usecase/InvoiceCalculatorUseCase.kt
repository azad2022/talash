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

    private fun roundMoney(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value).setScale(0, RoundingMode.HALF_UP).toDouble()
    }

    fun calculateInvoiceSummary(
        items: List<SaleItem>,
        discountAmount: Double = 0.0,
        taxPercent: Double = 9.0,
        prepayment: Double = 0.0
    ): InvoiceSummary {
        val rawSubtotal = items.sumOf { it.total }
        val subtotal = roundMoney(rawSubtotal)
        val cleanDiscount = roundMoney(discountAmount)
        val discountedSubtotal = (subtotal - cleanDiscount).coerceAtLeast(0.0)
        val taxAmount = roundMoney(discountedSubtotal * (taxPercent / 100.0))
        val finalPayable = roundMoney(discountedSubtotal + taxAmount)
        val cleanPrepayment = roundMoney(prepayment)
        val remainingBalance = (finalPayable - cleanPrepayment).coerceAtLeast(0.0)

        return InvoiceSummary(
            subtotal = subtotal,
            totalDiscount = cleanDiscount,
            totalTax = taxAmount,
            finalPayable = finalPayable,
            prepayment = cleanPrepayment,
            remainingBalance = remainingBalance
        )
    }
}
