package com.example.kisisel_asistan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF2B1800),
    primaryContainer = Color(0xFF7A4A00),
    onPrimaryContainer = Color(0xFFFFDFB0),
    secondary = Color(0xFFFFCC80),
    onSecondary = Color(0xFF2B1800),
    tertiary = Color(0xFFFFE0B2),
    background = Color(0xFF0A0807),
    onBackground = Color(0xFFFFF3E0),
    surface = Color(0xFF15110C),
    onSurface = Color(0xFFFFF3E0),
    surfaceVariant = Color(0xFF241B12),
    onSurfaceVariant = Color(0xFFD8C3AE),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0000)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF8C00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF4A2800),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFD54F),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF2B1D0E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B1D0E),
    surfaceVariant = Color(0xFFFFEFDD),
    onSurfaceVariant = Color(0xFF6B5847),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun KisiselasistanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
