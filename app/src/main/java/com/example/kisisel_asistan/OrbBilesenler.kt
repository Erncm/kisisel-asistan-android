package com.example.kisisel_asistan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class EtiketTuru { NOTR, OK, BAD }

@Composable
fun OrbKart(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SamanthaTheme.card),
        border = BorderStroke(1.dp, SamanthaTheme.accent.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun OrbTile(baslik: String, deger: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SamanthaTheme.card),
        border = BorderStroke(1.dp, SamanthaTheme.accent.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(deger, style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = SamanthaTheme.ink)
            Text(baslik, style = MaterialTheme.typography.bodySmall, color = SamanthaTheme.muted)
        }
    }
}

@Composable
fun OrbTag(metin: String, tur: EtiketTuru = EtiketTuru.NOTR) {
    val (arkaplan, yazi) = when (tur) {
        EtiketTuru.OK -> SamanthaTheme.tagOk.copy(alpha = 0.16f) to SamanthaTheme.tagOk
        EtiketTuru.BAD -> SamanthaTheme.tagBad.copy(alpha = 0.16f) to SamanthaTheme.tagBad
        EtiketTuru.NOTR -> SamanthaTheme.accent.copy(alpha = 0.16f) to SamanthaTheme.accent
    }
    Text(
        text = metin,
        color = yazi,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(arkaplan)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}
