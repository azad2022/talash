package com.example.domain

import com.example.data.model.Product
import com.example.data.model.StockTakeItem
import com.example.domain.util.BarcodeResolver
import org.junit.Assert.*
import org.junit.Test

class BarcodeResolverTest {

    @Test
    fun `resolveProduct finds by exact custom barcode`() {
        val p1 = Product(id = 1, name = "انگشتر زمرد", category = "طلا", weightGram = java.math.BigDecimal.ONE, wagePrice = java.math.BigDecimal.ZERO, wageType = "FIXED", customBarcode = "BAR-100", stock = 2)
        val p2 = Product(id = 2, name = "النگو تراش", category = "طلا", weightGram = java.math.BigDecimal.ONE, wagePrice = java.math.BigDecimal.ZERO, wageType = "FIXED", customBarcode = "BAR-200", stock = 5)
        val products = listOf(p1, p2)

        val resolved = BarcodeResolver.resolveProduct("BAR-100", products)
        assertNotNull(resolved)
        assertEquals(1, resolved?.id)
        assertEquals("انگشتر زمرد", resolved?.name)
    }

    @Test
    fun `resolveProduct resolves standard G-prefixed barcode format`() {
        val p1 = Product(id = 42, name = "دستبند کارتیه", category = "طلا", weightGram = java.math.BigDecimal.ONE, wagePrice = java.math.BigDecimal.ZERO, wageType = "FIXED", stock = 3)
        val products = listOf(p1)

        val resolvedWithPrefix = BarcodeResolver.resolveProduct("G-000042", products)
        assertNotNull(resolvedWithPrefix)
        assertEquals(42, resolvedWithPrefix?.id)

        val resolvedRawNumber = BarcodeResolver.resolveProduct("42", products)
        assertNotNull(resolvedRawNumber)
        assertEquals(42, resolvedRawNumber?.id)
    }

    @Test
    fun `resolveProduct handles case and whitespace trimming safely`() {
        val p1 = Product(id = 1, name = "گردنبند", category = "طلا", weightGram = java.math.BigDecimal.ONE, wagePrice = java.math.BigDecimal.ZERO, wageType = "FIXED", customBarcode = "MyGold123", stock = 1)
        val products = listOf(p1)

        val resolved = BarcodeResolver.resolveProduct("  mygold123  ", products)
        assertNotNull(resolved)
        assertEquals(1, resolved?.id)
    }

    @Test
    fun `resolveStockTakeItem finds correct item by custom or generated barcode`() {
        val item1 = StockTakeItem(
            id = 10,
            sessionId = 1,
            productId = 5,
            productName = "نیم ست",
            productCategory = "طلا",
            productBarcode = "NIM-SET-5",
            expectedStockAtStart = 2,
            countedStock = 0,
            isCounted = false
        )
        val item2 = StockTakeItem(
            id = 11,
            sessionId = 1,
            productId = 8,
            productName = "سکه امامی",
            productCategory = "سکه",
            productBarcode = "",
            expectedStockAtStart = 10,
            countedStock = 0,
            isCounted = false
        )
        val items = listOf(item1, item2)

        val res1 = BarcodeResolver.resolveStockTakeItem("NIM-SET-5", items)
        assertNotNull(res1)
        assertEquals(5, res1?.productId)

        val res2 = BarcodeResolver.resolveStockTakeItem("G-000008", items)
        assertNotNull(res2)
        assertEquals(8, res2?.productId)
    }

    @Test
    fun `findDuplicateCustomBarcodes correctly identifies duplicate active barcodes`() {
        val p1 = Product(id = 1, name = "طلا ۱", category = "طلا", weightGram = java.math.BigDecimal.ONE, wagePrice = java.math.BigDecimal.ZERO, wageType = "FIXED", customBarcode = "DUP-999")
        val p2 = Product(id = 2, name = "طلا ۲", category = "طلا", weightGram = java.math.BigDecimal.ONE, wagePrice = java.math.BigDecimal.ZERO, wageType = "FIXED", customBarcode = "dup-999")
        val p3 = Product(id = 3, name = "طلا ۳", category = "طلا", weightGram = java.math.BigDecimal.ONE, wagePrice = java.math.BigDecimal.ZERO, wageType = "FIXED", customBarcode = "UNIQUE-1")

        val duplicates = BarcodeResolver.findDuplicateCustomBarcodes(listOf(p1, p2, p3))
        assertEquals(1, duplicates.size)
        assertEquals("dup-999", duplicates.first())
    }
}
