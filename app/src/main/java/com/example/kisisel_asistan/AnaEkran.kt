package com.example.kisisel_asistan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
        when (seciliSekme) {
            0 -> Text("Ana Sayfa ekranı", modifier = Modifier.padding(icPadding).padding(16.dp))
            1 -> SohbetEkrani(Modifier.padding(icPadding))
            2 -> Text("Sağlık ekranı", modifier = Modifier.padding(icPadding).padding(16.dp))
            3 -> Text("Ara ekranı", modifier = Modifier.padding(icPadding).padding(16.dp))
            4 -> AyarlarEkrani(Modifier.padding(icPadding))
        }
    }
}

@Composable
fun SohbetEkrani(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val mesajlar = remember { mutableStateListOf<ChatMesaj>() }
    var girdi by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }
    val listeDurumu = rememberLazyListState()

    LaunchedEffect(mesajlar.size) {
        if (mesajlar.isNotEmpty()) {
            listeDurumu.animateScrollToItem(mesajlar.size - 1)
        }
    }

    fun gonder() {
        val anahtar = apiAnahtariOku(context)
        if (anahtar.isBlank()) {
            hata = "Önce Ayarlar sekmesinden Gemini API anahtarını gir"
            return
        }
        if (girdi.isBlank()) return

        val kullaniciMesaji = ChatMesaj(girdi.trim(), benMi = true)
        mesajlar.add(kullaniciMesaji)
        girdi = ""
        hata = null
        yukleniyor = true

        kapsam.launch {
            val sonuc = geminiYanitAl(anahtar, mesajlar.toList())
            yukleniyor = false
            sonuc.onSuccess { yanit ->
                mesajlar.add(ChatMesaj(yanit, benMi = false))
            }.onFailure { e ->
                hata = e.message ?: "Bilinmeyen hata"
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            state = listeDurumu
        ) {
            items(mesajlar) { mesaj ->
                MesajBalonu(mesaj)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (yukleniyor) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        if (hata != null) {
            Text(
                text = hata ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = girdi,
                onValueChange = { girdi = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Bir şeyler yaz...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { gonder() }) {
                Icon(Icons.Outlined.Send, contentDescription = "Gönder")
            }
        }
    }
}

@Composable
fun MesajBalonu(mesaj: ChatMesaj) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mesaj.benMi) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (mesaj.benMi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(text = mesaj.icerik, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun AyarlarEkrani(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var anahtar by remember { mutableStateOf(apiAnahtariOku(context)) }
    var kaydedildi by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Ayarlar", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gemini API Anahtarı", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = anahtar,
            onValueChange = { anahtar = it; kaydedildi = false },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("AIza...") }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            apiAnahtariKaydet(context, anahtar.trim())
            kaydedildi = true
        }) {
            Text("Kaydet")
        }
        if (kaydedildi) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Kaydedildi ✓", color = MaterialTheme.colorScheme.primary)
        }
    }
}
