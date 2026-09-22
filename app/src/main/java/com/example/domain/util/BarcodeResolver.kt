package com.example.domain.util

import com.example.data.model.Product
import com.example.data.model.StockTakeItem

object BarcodeResolver {

    /**
     * Resolves an input barcode string against a list of products.
     * Priority 1: Exact match with customBarcode (non-blank, case-insensitive).
     * Priority 2: System generated internal format G-xxxxxx -> resolved to productId.
     * Priority 3: Direct productId match if the input is a valid integer.
     */
    fun resolveProduct(barcode: String, products: List<Product>): Product? {
        val clean = barcode.trim()
        if (clean.isEmpty()) return null

        // Priority 1: Match customBarcode
        val customMatch = products.firstOrNull {
            it.customBarcode.isNotBlank() && it.customBarcode.trim().equals(clean, ignoreCase = true)
        }
        if (customMatch != null) return customMatch

        // Priority 2: Match G-xxxxxx pattern (e.g. G-000005, G-5)
        val gMatch = Regex("^[Gg]-0*(\\d+)$").matchEntire(clean)
        if (gMatch != null) {
            val id = gMatch.groupValues[1].toIntOrNull()
            if (id != null) {
                val prod = products.firstOrNull { it.id == id }
                if (prod != null) return prod
            }
        }

        // Priority 3: Direct productId if purely numeric
        val numericId = clean.toIntOrNull()
        if (numericId != null) {
            val prod = products.firstOrNull { it.id == numericId }
            if (prod != null) return prod
        }

        return null
    }

    /**
     * Resolves an input barcode string against StockTakeItems in an audit session.
     * Priority 1: Exact match with productBarcode (non-blank, case-insensitive).
     * Priority 2: System generated internal format G-xxxxxx -> resolved to productId.
     * Priority 3: Direct productId match if the input is a valid integer.
     */
    fun resolveStockTakeItem(barcode: String, items: List<StockTakeItem>): StockTakeItem? {
        val clean = barcode.trim()
        if (clean.isEmpty()) return null

        // Priority 1: Match custom productBarcode
        val barcodeMatch = items.firstOrNull {
            it.productBarcode.isNotBlank() && it.productBarcode.trim().equals(clean, ignoreCase = true)
        }
        if (barcodeMatch != null) return barcodeMatch

        // Priority 2: Match G-xxxxxx pattern
        val gMatch = Regex("^[Gg]-0*(\\d+)$").matchEntire(clean)
        if (gMatch != null) {
            val id = gMatch.groupValues[1].toIntOrNull()
            if (id != null) {
                val item = items.firstOrNull { it.productId == id }
                if (item != null) return item
            }
        }

        // Priority 3: Direct numeric productId
        val numericId = clean.toIntOrNull()
        if (numericId != null) {
            val item = items.firstOrNull { it.productId == numericId }
            if (item != null) return item
        }

        return null
    }

    /**
     * Identifies any duplicate custom barcodes among active products.
     * Returns a list of duplicated barcodes (trimmed) that appear on more than one active product.
     */
    fun findDuplicateCustomBarcodes(products: List<Product>): List<String> {
        val nonBlank = products.filter { !it.isDeleted && it.customBarcode.isNotBlank() }
        val grouped = nonBlank.groupBy { it.customBarcode.trim().lowercase() }
        return grouped.filter { it.value.size > 1 }.keys.toList()
    }
}
