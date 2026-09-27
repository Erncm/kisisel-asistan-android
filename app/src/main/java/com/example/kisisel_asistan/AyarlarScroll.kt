package com.example.kisisel_asistan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Mevcut hiçbir koda müdahale etmeden Ayarlar ekranını dikey kaydırılabilir (scrollable)
 * yapmak için kullanılan yardımcı kapsayıcı bileşen.
 */
@Composable
fun AyarlarKaydirilabilirKapsayici(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        content()
    }
}
