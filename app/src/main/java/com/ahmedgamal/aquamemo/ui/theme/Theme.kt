package com.ahmedgamal.aquamemo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Dark Color Scheme Definition ---
private val AppDarkColorScheme = darkColorScheme(
    primary = TealAccent, // Use TealAccent defined in Color.kt
    onPrimary = DarkBlueSurface, // Use DarkBlueSurface defined in Color.kt
    primaryContainer = DarkBluePrimary,
    onPrimaryContainer = LightText,
    secondary = DarkBluePrimary,
    onSecondary = LightText,
    secondaryContainer = DarkBlueSurfaceVariant,
    onSecondaryContainer = LightText,
    tertiary = TealAccent,
    onTertiary = DarkBlueSurface,
    tertiaryContainer = DarkBlueSurfaceVariant,
    onTertiaryContainer = LightText,
    error = Color(0xFFFFB4AB), // Standard Material Dark Error
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBlueSurface, // Use DarkBlueSurface defined in Color.kt
    onBackground = LightText,    // Use LightText defined in Color.kt
    surface = DarkBlueSurfaceVariant, // Use DarkBlueSurfaceVariant defined in Color.kt
    onSurface = LightText,
    surfaceVariant = Color(0xFF42474E), // Adjust as needed
    onSurfaceVariant = Color(0xFF102A43), // Adjust as needed
    outline = Color(0xFF8C9199)
)

private val AppLightColorScheme = lightColorScheme(
    primary = AquaPrimary, // Use AquaPrimary defined in Color.kt
    onPrimary = AquaOnPrimary,
    primaryContainer = AquaPrimaryContainer,
    onPrimaryContainer = AquaOnPrimaryContainer,
    secondary = AquaSecondary,
    onSecondary = AquaOnSecondary,
    secondaryContainer = AquaSecondaryContainer,
    onSecondaryContainer = AquaOnSecondaryContainer,
    error = AquaError, // Use AquaError defined in Color.kt
    onError = AquaOnError,
    background = AquaBackground,
    onBackground = AquaOnBackground,
    surface = AquaSurface,
    onSurface = AquaOnSurface,
    surfaceVariant = AquaSurfaceVariant, // Use AquaSurfaceVariant defined in Color.kt
    onSurfaceVariant = AquaOnSurfaceVariant // Use AquaOnSurfaceVariant defined in Color.kt
)
@Composable
fun AquaMemoTheme(
    themePreference: String = "system",
    dynamicColor: Boolean = false,
    fontSize: String = "medium",
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> AppDarkColorScheme
        else -> AppLightColorScheme
    }

    val scaledTypography = when (fontSize) {
        "small" -> FontTheme.getTypography("small")
        "large" -> FontTheme.getTypography("large")
        else -> FontTheme.getTypography("medium")
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect   {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDarkTheme // Light icons on dark status bar, Dark icons on light status bar
            controller.isAppearanceLightNavigationBars = !useDarkTheme // Same logic for navigation bar
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}