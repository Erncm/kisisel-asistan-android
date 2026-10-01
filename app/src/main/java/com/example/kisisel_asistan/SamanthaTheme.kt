package com.example.kisisel_asistan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object SamanthaTheme {
    var accent by mutableStateOf(Color(0xFFFF8C00))
    var isDark by mutableStateOf(true)

    val bg: Color get() = if (isDark) Color(0xFF0A0807) else Color(0xFFFFFBF5)
    val bar: Color get() = if (isDark) Color(0xFF17120D) else Color(0xFFFFEDD8)
    val ink: Color get() = if (isDark) Color(0xFFFFF3E0) else Color(0xFF2B1D0E)
    val muted: Color = Color(0xFFB08968)

    val accent2: Color get() = lerp(accent, Color.Black, 0.4f)
    val pill: Color get() = lerp(bg, accent, 0.22f)
}

val ACCENT_PALETTE = listOf(
    Color(0xFFFF8C00),
    Color(0xFFF59E0B),
    Color(0xFFFFD54F),
    Color(0xFFFF5722),
    Color(0xFFFFAB40),
    Color(0xFFE65100),
)
