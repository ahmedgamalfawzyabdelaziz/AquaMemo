// FontTheme.kt
package com.ahmedgamal.aquamemo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object FontTheme {
    fun getTypography(fontSize: String): Typography {
        val scale = when (fontSize) {
            "small" -> 0.9f
            "large" -> 1.2f
            else -> 1.0f // medium
        }

        return Typography(
            displayLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (57.sp.value * scale).sp,
                lineHeight = (64.sp.value * scale).sp,
                letterSpacing = (-0.25.sp.value * scale).sp
            ),
            displayMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (45.sp.value * scale).sp,
                lineHeight = (52.sp.value * scale).sp,
                letterSpacing = 0.sp
            ),
            displaySmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (36.sp.value * scale).sp,
                lineHeight = (44.sp.value * scale).sp,
                letterSpacing = 0.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (32.sp.value * scale).sp,
                lineHeight = (40.sp.value * scale).sp,
                letterSpacing = 0.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (28.sp.value * scale).sp,
                lineHeight = (36.sp.value * scale).sp,
                letterSpacing = 0.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (24.sp.value * scale).sp,
                lineHeight = (32.sp.value * scale).sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (22.sp.value * scale).sp,
                lineHeight = (28.sp.value * scale).sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (16.sp.value * scale).sp,
                lineHeight = (24.sp.value * scale).sp,
                letterSpacing = (0.15.sp.value * scale).sp
            ),
            titleSmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (14.sp.value * scale).sp,
                lineHeight = (20.sp.value * scale).sp,
                letterSpacing = (0.1.sp.value * scale).sp
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (16.sp.value * scale).sp,
                lineHeight = (24.sp.value * scale).sp,
                letterSpacing = (0.5.sp.value * scale).sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (14.sp.value * scale).sp,
                lineHeight = (20.sp.value * scale).sp,
                letterSpacing = (0.25.sp.value * scale).sp
            ),
            bodySmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = (12.sp.value * scale).sp,
                lineHeight = (16.sp.value * scale).sp,
                letterSpacing = (0.4.sp.value * scale).sp
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (14.sp.value * scale).sp,
                lineHeight = (20.sp.value * scale).sp,
                letterSpacing = (0.1.sp.value * scale).sp
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (12.sp.value * scale).sp,
                lineHeight = (16.sp.value * scale).sp,
                letterSpacing = (0.5.sp.value * scale).sp
            ),
            labelSmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = (11.sp.value * scale).sp,
                lineHeight = (16.sp.value * scale).sp,
                letterSpacing = (0.5.sp.value * scale).sp
            )
        )
    }
}