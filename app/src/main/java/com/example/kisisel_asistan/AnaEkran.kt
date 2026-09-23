package com.example.kisisel_asistan

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class Sekme(val baslik: String, val ikon: ImageVector) {
    object AnaSayfa : Sekme("Ana Sayfa", Icons.Outlined.Home)
    object Sohbet : Sekme("Sohbet", Icons.Outlined.Chat)
    object Saglik : Sekme("Sağlık", Icons.Outlined.Favorite)
    object Ara : Sekme("Ara", Icons.Outlined.Search)
    object Ayarlar : Sekme("Ayarlar", Icons.Outlined.Settings)
}

val sekmeler = listOf(
    Sekme.AnaSayfa,
    Sekme.Sohbet,
    Sekme.Saglik,
    Sekme.Ara,
    Sekme.Ayarlar
)

@Composable
fun AnaEkranIskelet() {
    var seciliSekme by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                sekmeler.forEachIndexed { index, sekme ->
                    NavigationBarItem(
                        selected = seciliSekme == index,
                        onClick = { seciliSekme = index },
                        icon = { Icon(sekme.ikon, contentDescription = sekme.baslik) },
                        label = { Text(sekme.baslik) }
                    )
                }
            }
        }
    ) { icPadding ->
        val metin = when (seciliSekme) {
            0 -> "Ana Sayfa"
            1 -> "Sohbet"
            2 -> "Sağlık"
            3 -> "Ara"
            else -> "Ayarlar"
        }
        Text(text = "$metin ekranı", modifier = Modifier.padding(icPadding).padding(16.dp))
    }
}
