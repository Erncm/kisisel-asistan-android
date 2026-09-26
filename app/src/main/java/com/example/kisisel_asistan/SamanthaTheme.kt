package com.example.kisisel_asistan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object SamanthaTheme {
    var accent by mutableStateOf(Color(0xFF8B5CF6))
    var isDark by mutableStateOf(false)

    val bg: Color get() = if (isDark) Color(0xFF151223) else Color(0xFFFAF8FF)
    val bar: Color get() = if (isDark) Color(0xFF1E1A33) else Color(0xFFEFEAFB)
    val ink: Color get() = if (isDark) Color(0xFFEFEAFB) else Color(0xFF2B2440)
    val muted: Color = Color(0xFF8A83A0)

    val accent2: Color get() = lerp(accent, Color.Black, 0.4f)
    val pill: Color get() = lerp(bg, accent, 0.22f)
}

val ACCENT_PALETTE = listOf(
    Color(0xFF8B5CF6),
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFFEF4444),
)
