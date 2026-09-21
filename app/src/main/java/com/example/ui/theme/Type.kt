package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Vazirmatn font family loaded from local res/font/vazirmatn.ttf
val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Thin),
    Font(R.font.vazirmatn, FontWeight.ExtraLight),
    Font(R.font.vazirmatn, FontWeight.Light),
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.SemiBold),
    Font(R.font.vazirmatn, FontWeight.Bold),
    Font(R.font.vazirmatn, FontWeight.ExtraBold),
    Font(R.font.vazirmatn, FontWeight.Black)
)

// Maintained for backward compatibility, mapped directly to Vazirmatn
val PersianZibaFont = VazirmatnFontFamily

private val defaultTypography = Typography()

// Complete Material 3 typography suite powered exclusively by Vazirmatn
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = VazirmatnFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = VazirmatnFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = VazirmatnFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = VazirmatnFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = VazirmatnFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = VazirmatnFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = VazirmatnFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = VazirmatnFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = VazirmatnFontFamily)
)
