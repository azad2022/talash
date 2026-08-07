package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

object ThemeConfig {
    var isLightMode by mutableStateOf(false)
}

// Elegant Premium Luxury Gallery Theme Colors
val DarkObsidian: Color get() = if (ThemeConfig.isLightMode) Color(0xFFF7FBF9) else Color(0xFF080706)   // Rich Obsidian Black-Bronze velvet base (#080706 vs clean jade-tinted white #F7FBF9)
val SmokyCard: Color get() = if (ThemeConfig.isLightMode) Color(0xFFFFFFFF) else Color(0xFF131110)      // Premium glassmorphic gold-tinted obsidian card (#131110)
val DarkGreyCard: Color get() = if (ThemeConfig.isLightMode) Color(0xFFEFF6F3) else Color(0xFF181513)   // Deep velvety dark surface (#181513)
val MetallicGold: Color get() = if (ThemeConfig.isLightMode) Color(0xFF053E33) else Color(0xFFF4B323)   // Dark Jade Green primary (#053E33) vs Radiant Gold crown (#F4B323)
val DarkMetallicGold: Color get() = if (ThemeConfig.isLightMode) Color(0xFF02211A) else Color(0xFFC5911A) // Extra deep jade green vs Burnished ancient gold (#C5911A)
val LightMetallicGold: Color get() = if (ThemeConfig.isLightMode) Color(0xFFDDF2EB) else Color(0xFFFDF2C2) // Pale minty jade green vs Glowing pale champagne gold accent (#FDF2C2)
val SmokyBronze: Color get() = if (ThemeConfig.isLightMode) Color(0xFFEDF6F3) else Color(0xFF1F1B17)    // Clean light sage-jade green vs Deep imperial dark bronze backdrop (#1F1B17)
val GoldAlertOrange: Color get() = if (ThemeConfig.isLightMode) Color(0xFFE0533C) else Color(0xFFFF9800) // Beautiful coral accent vs Warm burning amber alert
val CharcoalBorder: Color get() = if (ThemeConfig.isLightMode) Color(0x2B053E33) else Color(0x1CEAD3A2)  // Soft jade outline vs Soft golden dust perimeter (11% golden-beige opacity vs 14% dark)
val DarkInnerBorder: Color get() = if (ThemeConfig.isLightMode) Color(0xFFD6E6E1) else Color(0x28EAD3A2) // Clean jade-gray outline vs Refined gold-tinted inner bevel outline (15% gold opacity)

val TextWhite: Color get() = if (ThemeConfig.isLightMode) Color(0xFF0A1E1A) else Color(0xFFF8FAFC)     // Deep shade jade, high-contrast vs elegant crisp cream/white
val TextGray: Color get() = if (ThemeConfig.isLightMode) Color(0xFF4A6861) else Color(0xFFA19E95)      // Elegant medium sage-mint text vs warm golden-gray
val LightGrayLine: Color get() = if (ThemeConfig.isLightMode) Color(0x18053E33) else Color(0x10FFEAA0) // Fine jade divider seam
val StatusGreen: Color get() = if (ThemeConfig.isLightMode) Color(0xFF10B981) else Color(0xFF4ADE80) // Vibrant emerald green vs responsive pastel green




