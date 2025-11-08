package com.ahmedgamal.aquamemo.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders

// ✅ تعريف ColorScheme للوضع الفاتح
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4385C6),
    onPrimary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1a1a1a),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF666666),
    background = Color(0xFFF5F5F5),
    error = Color(0xFFD32F2F),
    tertiary = Color(0xFFFF9800)
)

// ✅ تعريف ColorScheme للوضع الداكن
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF011627),
    surface = Color(0xFF022B4A),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF022B4A),
    onSurfaceVariant = Color(0xFFCCCCCC),
    background = Color(0xFF011627),
    error = Color(0xFFFFB4AB),
    tertiary = Color(0xFFFFB967)
)

// ✅ الطريقة الصحيحة لاستدعاء ColorProviders
val AppLightGlanceColors = ColorProviders(LightColorScheme)
val AppDarkGlanceColors = ColorProviders(DarkColorScheme)