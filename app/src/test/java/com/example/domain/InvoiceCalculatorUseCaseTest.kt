package com.example.domain

import com.example.data.model.SaleItem
import com.example.domain.usecase.InvoiceCalculatorUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InvoiceCalculatorUseCaseTest {

    private lateinit var useCase: InvoiceCalculatorUseCase

    @Before
    fun setUp() {
        useCase = InvoiceCalculatorUseCase()
    }

    @Test
    fun calculateInvoiceSummary_correctSubtotalDiscountAndTax() {
        val items = listOf(
            SaleItem(id = 1, invoiceId = 0, productId = 101, quantity = 1, unitPrice = 10_000_000.0, total = 10_000_000.0),
            SaleItem(id = 2, invoiceId = 0, productId = 102, quantity = 2, unitPrice = 5_000_000.0, total = 10_000_000.0)
        )

        val summary = useCase.calculateInvoiceSummary(
            items = items,
            discountAmount = 1_000_000.0, // 1 million discount
            taxPercent = 9.0,
            prepayment = 5_000_000.0
        )

        // Subtotal = 10m + 10m = 20,000,000
        assertEquals(20_000_000.0, summary.subtotal, 0.01)

        // Total Discount = 1,000,000
        assertEquals(1_000_000.0, summary.totalDiscount, 0.01)

        // Discounted = 19,000,000 -> Tax (9%) = 1,710,000
        assertEquals(1_710_000.0, summary.totalTax, 0.01)

        // Final Payable = 19,000,000 + 1,710,000 = 20,710,000
        assertEquals(20_710_000.0, summary.finalPayable, 0.01)

        // Remaining = 20,710,000 - 5,000,000 = 15,710,000
        assertEquals(15_710_000.0, summary.remainingBalance, 0.01)
    }
}
