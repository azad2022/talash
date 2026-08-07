package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MetallicGold,
    secondary = SmokyBronze,
    tertiary = GoldAlertOrange,
    background = DarkObsidian,
    surface = SmokyCard,
    onPrimary = DarkObsidian,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    outline = CharcoalBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Mode
    dynamicColor: Boolean = false, // Force custom style
    content: @Composable () -> Unit,
) {
    // We strictly use our custom color scheme, choosing dark/light based on ThemeConfig
    val colorScheme = if (ThemeConfig.isLightMode) {
        lightColorScheme(
            primary = MetallicGold,
            secondary = SmokyBronze,
            tertiary = GoldAlertOrange,
            background = DarkObsidian,
            surface = SmokyCard,
            onPrimary = Color.White,
            onSecondary = TextWhite,
            onBackground = TextWhite,
            onSurface = TextWhite,
            outline = CharcoalBorder
        )
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
