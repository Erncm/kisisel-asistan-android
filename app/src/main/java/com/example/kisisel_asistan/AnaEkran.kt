package com.example.kisisel_asistan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
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
            0 -> AnaSayfaIcerik(Modifier.padding(icPadding))
            1 -> Text("Sohbet ekranı", modifier = Modifier.padding(icPadding))
            2 -> Text("Sağlık ekranı", modifier = Modifier.padding(icPadding))
            3 -> Text("Ara ekranı", modifier = Modifier.padding(icPadding))
            4 -> Text("Ayarlar ekranı", modifier = Modifier.padding(icPadding))
        }
    }
}

@Composable
fun AnaSayfaIcerik(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var havaDurumu by remember { mutableStateOf(havaDurumuOnbellekOku(context)) }
    var izinVar by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val kapsam = rememberCoroutineScope()

    val izinIstegi = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi -> izinVar = verildi }

    LaunchedEffect(Unit) {
        if (!izinVar) {
            izinIstegi.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    LaunchedEffect(izinVar) {
        if (izinVar) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { konum ->
                    if (konum != null) {
                        kapsam.launch {
                            val veri = havaDurumuGetir(context, konum.latitude, konum.longitude)
                            if (veri != null) {
                                havaDurumu = veri
                                havaDurumuOnbellekKaydet(context, veri)
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                // izin reddedildi
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        KonumHavaDurumuKarti(havaDurumu, izinVar)
        Spacer(modifier = Modifier.height(16.dp))
        SaglikPaneli()
        Spacer(modifier = Modifier.height(16.dp))
        OneriKarti()
    }
}

@Composable
fun KonumHavaDurumuKarti(veri: HavaDurumuVerisi?, izinVar: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = veri?.sehir ?: "Konumunuz",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when {
                        !izinVar -> "Konum izni bekleniyor"
                        veri == null -> "Yükleniyor..."
                        else -> veri.aciklama
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WbCloudy,
                    contentDescription = "Hava durumu",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (veri != null) "${veri.sicaklik.toInt()}°C" else "--°C",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
fun SaglikPaneli() {
    Column {
        Text(
            text = "Sağlık Özeti",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SaglikKarti(Icons.Outlined.DirectionsWalk, "Adım", "4.230", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            SaglikKarti(Icons.Outlined.LocalDrink, "Su", "1.2L", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SaglikKarti(Icons.Outlined.NightsStay, "Uyku", "6s 45dk", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            SaglikKarti(Icons.Outlined.MonitorHeart, "Nabız", "72 bpm", Modifier.weight(1f))
        }
    }
}

@Composable
fun SaglikKarti(ikon: ImageVector, baslik: String, deger: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(ikon, contentDescription = baslik, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = deger, style = MaterialTheme.typography.titleMedium)
            Text(text = baslik, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OneriKarti() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Bugün için öneri", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Henüz yeterli veri yok, kullanmaya devam ettikçe kişisel öneriler burada görünecek.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnaEkranOnizleme() {
    AnaEkranIskelet()
}
