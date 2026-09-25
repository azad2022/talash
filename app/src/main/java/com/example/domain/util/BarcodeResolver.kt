package com.example.domain.util

import com.example.data.model.Product
import com.example.data.model.StockTakeItem

sealed class ProductResolution {
    data class Single(val product: Product) : ProductResolution()
    data class Ambiguous(val barcode: String, val products: List<Product>) : ProductResolution()
    object NotFound : ProductResolution()
}

sealed class StockTakeItemResolution {
    data class Single(val item: StockTakeItem) : StockTakeItemResolution()
    data class Ambiguous(val barcode: String, val items: List<StockTakeItem>) : StockTakeItemResolution()
    object NotFound : StockTakeItemResolution()
}

object BarcodeResolver {

    /**
     * Returns the canonical barcode string for a product:
     * - Returns customBarcode (trimmed) if non-blank.
     * - Otherwise returns standardized system format: "G-" padded with 6 digits (e.g. G-000042).
     */
    fun getCanonicalBarcode(productId: Int, customBarcode: String?): String {
        val trimmed = customBarcode?.trim().orEmpty()
        return if (trimmed.isNotBlank()) trimmed else "G-${productId.toString().padStart(6, '0')}"
    }

    fun getCanonicalBarcode(product: Product): String {
        return getCanonicalBarcode(product.id, product.customBarcode)
    }

    /**
     * Finds collisions among all active products based on their canonical barcodes.
     * Detects:
     * 1. Duplicate custom barcodes (e.g. BAR1 vs BAR1).
     * 2. Collisions between custom barcode and system generated format (e.g. custom = "G-000042" vs generated for id 42).
     * Returns a map of collision barcode -> list of conflicting products.
     */
    fun findCanonicalBarcodeCollisions(products: List<Product>): Map<String, List<Product>> {
        val aliases = mutableMapOf<String, MutableMap<Int, Product>>()
        products.filter { !it.isDeleted }.forEach { product ->
            barcodeAliases(product).forEach { alias ->
                aliases.getOrPut(alias) { mutableMapOf() }[product.id] = product
            }
        }
        return aliases.mapValues { it.value.values.toList() }.filterValues { it.size > 1 }
    }

    fun findCanonicalBarcodeConflictsForProduct(product: Product, products: List<Product>): Set<String> =
        products.filter { !it.isDeleted && it.id != product.id }
            .flatMap { other -> barcodeAliases(other).filter { alias -> alias in barcodeAliases(product) } }
            .toSet()

    fun findCanonicalBarcodeCollisionsInItems(items: List<StockTakeItem>): Map<String, List<StockTakeItem>> {
        val aliases = mutableMapOf<String, MutableMap<Int, StockTakeItem>>()
        items.forEach { item ->
            stockTakeItemAliases(item).forEach { alias ->
                aliases.getOrPut(alias) { mutableMapOf() }[item.productId] = item
            }
        }
        return aliases.mapValues { it.value.values.toList() }.filterValues { it.size > 1 }
    }

    private fun barcodeAliases(product: Product): Set<String> = buildSet {
        add(getCanonicalBarcode(product).trim().lowercase())
        add(product.id.toString())
        add("g-${product.id.toString().padStart(6, '0')}")
        product.customBarcode.trim().takeIf { it.isNotBlank() }?.let { add(it.lowercase()) }
    }

    private fun stockTakeItemAliases(item: StockTakeItem): Set<String> = buildSet {
        item.productBarcode.trim().takeIf { it.isNotBlank() }?.let { add(it.lowercase()) }
        add(item.productId.toString())
        add("g-${item.productId.toString().padStart(6, '0')}")
    }

    /**
     * Resolves an input barcode string against a list of products with ambiguity detection.
     */
    fun resolveProductExact(barcode: String, products: List<Product>): ProductResolution {
        val clean = barcode.trim()
        if (clean.isEmpty()) return ProductResolution.NotFound
        val active = products.filter { !it.isDeleted }

        val matched = mutableMapOf<Int, Product>()

        // 1. Exact match with customBarcode (case-insensitive) or canonical barcode
        active.filter {
            (it.customBarcode.isNotBlank() && it.customBarcode.trim().equals(clean, ignoreCase = true)) ||
                    getCanonicalBarcode(it).equals(clean, ignoreCase = true)
        }.forEach { matched[it.id] = it }

        // 2. G-xxxxxx pattern
        val gMatch = Regex("^[Gg]-0*(\\d+)$").matchEntire(clean)
        if (gMatch != null) {
            val id = gMatch.groupValues[1].toIntOrNull()
            if (id != null) {
                active.firstOrNull { it.id == id }?.let { matched[it.id] = it }
            }
        }

        // 3. Numeric ID
        val numericId = clean.toIntOrNull()
        if (numericId != null) {
            active.firstOrNull { it.id == numericId }?.let { matched[it.id] = it }
        }

        return when {
            matched.isEmpty() -> ProductResolution.NotFound
            matched.size == 1 -> ProductResolution.Single(matched.values.first())
            else -> ProductResolution.Ambiguous(clean, matched.values.toList())
        }
    }

    fun resolveProduct(barcode: String, products: List<Product>): Product? {
        return when (val res = resolveProductExact(barcode, products)) {
            is ProductResolution.Single -> res.product
            else -> null
        }
    }

    /**
     * Resolves an input barcode string against StockTakeItems with ambiguity detection.
     */
    fun resolveStockTakeItemExact(barcode: String, items: List<StockTakeItem>): StockTakeItemResolution {
        val clean = barcode.trim()
        if (clean.isEmpty()) return StockTakeItemResolution.NotFound

        val matched = mutableMapOf<Int, StockTakeItem>()

        // 1. Exact match with productBarcode (case-insensitive)
        items.filter {
            it.productBarcode.isNotBlank() && it.productBarcode.trim().equals(clean, ignoreCase = true)
        }.forEach { matched[it.productId] = it }

        // 2. G-xxxxxx pattern
        val gMatch = Regex("^[Gg]-0*(\\d+)$").matchEntire(clean)
        if (gMatch != null) {
            val id = gMatch.groupValues[1].toIntOrNull()
            if (id != null) {
                items.firstOrNull { it.productId == id }?.let { matched[it.productId] = it }
            }
        }

        // 3. Direct numeric productId
        val numericId = clean.toIntOrNull()
        if (numericId != null) {
            items.firstOrNull { it.productId == numericId }?.let { matched[it.productId] = it }
        }

        return when {
            matched.isEmpty() -> StockTakeItemResolution.NotFound
            matched.size == 1 -> StockTakeItemResolution.Single(matched.values.first())
            else -> StockTakeItemResolution.Ambiguous(clean, matched.values.toList())
        }
    }

    fun resolveStockTakeItem(barcode: String, items: List<StockTakeItem>): StockTakeItem? {
        return when (val res = resolveStockTakeItemExact(barcode, items)) {
            is StockTakeItemResolution.Single -> res.item
            else -> null
        }
    }

    /**
     * Identifies any duplicate custom barcodes among active products.
     * Kept for backward compatibility.
     */
    fun findDuplicateCustomBarcodes(products: List<Product>): List<String> {
        val collisions = findCanonicalBarcodeCollisions(products)
        return collisions.keys.toList()
    }
}
