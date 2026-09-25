package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.FolderOpen

enum class ProductCategory(
    val title: String,
    val color: Color,
    val icon: ImageVector
) {
    RING("انگشتر", Color(0xFFFFD700), Icons.Filled.RadioButtonUnchecked), // Gold
    NECKLACE("گردنبند", Color(0xFFEF4444), Icons.Filled.AllInclusive), // Red
    EARRING("گوشواره", Color(0xFF3B82F6), Icons.Filled.Spa), // Blue
    BRACELET("دستبند", Color(0xFF10B981), Icons.Filled.Link), // Green/Teal
    BANGLE("النگو", Color(0xFF8B5CF6), Icons.Filled.DonutLarge), // Purple
    COIN("سکه", Color(0xFF06B6D4), Icons.Filled.Paid), // Cyan
    BULLION("شمش", Color(0xFFF97316), Icons.Filled.Layers), // Orange
    
    // Dynamic asset categories integrated with online webservice
    GOLD_24K("طلای ۲۴ عیار", Color(0xFFFF9800), Icons.Filled.Layers),
    GOLD_MELTED("طلای آبشده نقدی", Color(0xFFFFC107), Icons.Filled.Layers),
    SCRAP_GOLD("طلای متفرقه و مستعمل", Color(0xFFD97706), Icons.Filled.Layers),
    COIN_GRAM("سکه یک گرمی", Color(0xFF00bcd4), Icons.Filled.Paid),
    COIN_QUARTER("ربع سکه", Color(0xFF009688), Icons.Filled.Paid),
    COIN_HALF("نیم سکه", Color(0xFF4caf50), Icons.Filled.Paid),
    COIN_EMAMI("سکه امامی", Color(0xFF8bc34a), Icons.Filled.Paid),
    COIN_BAHAR("سکه بهار آزادی", Color(0xFF3f51b5), Icons.Filled.Paid),
    
    OTHER("غیره", Color(0xFF94A3B8), Icons.Filled.FolderOpen); // Gray

    companion object {
        fun fromTitle(title: String): ProductCategory {
            return values().firstOrNull { it.title == title || it.name.equals(title, ignoreCase = true) } ?: OTHER
        }

        fun allTitles(): List<String> = values().map { it.title }.filter { it != "غیره" }
    }
}
