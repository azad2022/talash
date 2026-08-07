package com.example.domain.usecase

import com.example.data.model.SaleItem

data class InvoiceSummary(
    val subtotal: Double,
    val totalDiscount: Double,
    val totalTax: Double,
    val finalPayable: Double,
    val prepayment: Double,
    val remainingBalance: Double
)

class InvoiceCalculatorUseCase {

    fun calculateInvoiceSummary(
        items: List<SaleItem>,
        discountAmount: Double = 0.0,
        taxPercent: Double = 9.0,
        prepayment: Double = 0.0
    ): InvoiceSummary {
        val subtotal = items.sumOf { it.total }
        val discountedSubtotal = (subtotal - discountAmount).coerceAtLeast(0.0)
        val taxAmount = discountedSubtotal * (taxPercent / 100.0)
        val finalPayable = discountedSubtotal + taxAmount
        val remainingBalance = (finalPayable - prepayment).coerceAtLeast(0.0)

        return InvoiceSummary(
            subtotal = subtotal,
            totalDiscount = discountAmount,
            totalTax = taxAmount,
            finalPayable = finalPayable,
            prepayment = prepayment,
            remainingBalance = remainingBalance
        )
    }
}
