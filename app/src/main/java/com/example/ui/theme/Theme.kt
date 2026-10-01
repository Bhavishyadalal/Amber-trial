package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AmberThemeConfig(
    val accent: Color = AmberAccentDefault,
    val accentLight: Color = AmberAccentLightDefault,
    val accentDark: Color = AmberAccentDarkDefault,
    val isAmbientGlow: Boolean = true,
    val isLowPower: Boolean = false,
    val isPerformance: Boolean = false
)

val LocalAmberTheme = staticCompositionLocalOf { AmberThemeConfig() }

@Composable
fun AmberTheme(
    accentColor: Color = AmberAccentDefault,
    isAmbientGlow: Boolean = true,
    isLowPower: Boolean = false,
    isPerformance: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkColors = darkColorScheme(
        primary = accentColor,
        onPrimary = Color(0xFF1D110B),
        primaryContainer = AmberAccentDarkDefault,
        onPrimaryContainer = AmberText,
        secondary = AmberAccentLightDefault,
        onSecondary = Color(0xFF1D110B),
        background = AmberBg,
        onBackground = AmberText,
        surface = AmberGlassCardColor,
        onSurface = AmberText,
        surfaceVariant = AmberGlassRaised,
        onSurfaceVariant = AmberDim,
        outline = AmberLine,
        error = AmberEmber
    )

    val amberConfig = AmberThemeConfig(
        accent = accentColor,
        accentLight = AmberAccentLightDefault,
        accentDark = AmberAccentDarkDefault,
        isAmbientGlow = isAmbientGlow && !isPerformance && !isLowPower,
        isLowPower = isLowPower || isPerformance,
        isPerformance = isPerformance
    )

    CompositionLocalProvider(LocalAmberTheme provides amberConfig) {
        MaterialTheme(
            colorScheme = darkColors,
            typography = Typography,
            content = content
        )
    }
}
