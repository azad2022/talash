package com.example.hardware

import com.example.data.model.InvoiceWithDetails
import com.example.data.model.Product
import com.example.data.model.SaleInvoice
import com.example.data.model.SaleItem
import com.example.hardware.print.ReceiptFormatter
import com.example.hardware.scale.ScaleWeightParser
import com.example.hardware.scale.StableWeightDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ScaleAndReceiptEngineTest {
    @Test
    fun parser_normalizes_decimal_and_unit() {
        assertEquals(BigDecimal("3.42"), ScaleWeightParser.parse("ST,GS,3.42 g"))
        assertEquals(BigDecimal("3420"), ScaleWeightParser.normalizeGrams(BigDecimal("3.42"), "kg"))
    }

    @Test
    fun detector_requires_consecutive_stable_samples() {
        val detector = StableWeightDetector(BigDecimal("0.005"), 4, 300L)
        assertEquals(null, detector.addSample(BigDecimal("3.420"), 0L))
        assertEquals(null, detector.addSample(BigDecimal("3.423"), 100L))
        assertEquals(null, detector.addSample(BigDecimal("3.421"), 200L))
        assertEquals(null, detector.addSample(BigDecimal("3.422"), 250L))
        val stable = detector.addSample(BigDecimal("3.422"), 300L)
        assertNotNull(stable)
        assertEquals(4, stable!!.samples)
        assertTrue(stable.grams.subtract(BigDecimal("3.4215")).abs() < BigDecimal("0.001"))
    }

    @Test
    fun receipt_uses_product_karat_and_item_weight() {
        val product = Product(
            id = 42,
            name = "انگشتر ویژه",
            category = "انگشتر",
            weightGram = BigDecimal("3.27"),
            karat = 22,
            wagePrice = BigDecimal.ZERO,
            wageType = "FIXED"
        )
        val invoice = InvoiceWithDetails(
            invoice = SaleInvoice(
                id = 10,
                customerId = 0,
                totalAmount = BigDecimal("50000000"),
                discount = BigDecimal.ZERO,
                tax = BigDecimal.ZERO,
                paidAmount = BigDecimal("50000000"),
                paymentType = "CASH"
            ),
            customer = null,
            items = listOf(
                SaleItem(
                    invoiceId = 10,
                    productId = 42,
                    quantity = 1,
                    unitPrice = BigDecimal("50000000"),
                    total = BigDecimal("50000000"),
                    customWeight = BigDecimal("3.27")
                )
            ),
            installments = emptyList()
        )

        val receipt = ReceiptFormatter.format(invoice, mapOf(42 to product))
        assertTrue(receipt.contains("عیار: 22"))
        assertTrue(receipt.contains("3.27"))
        assertTrue(receipt.contains("مشتری متفرقه"))
    }
}
