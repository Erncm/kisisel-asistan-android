package com.example.kisisel_asistan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
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

data class SohbetMesaji(
    val metin: String,
    val kullaniciMi: Boolean
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
            1 -> SohbetEkrani(Modifier.padding(icPadding))
            2 -> Text("Sağlık ekranı", modifier = Modifier.padding(icPadding))
            3 -> Text("Ara ekranı", modifier = Modifier.padding(icPadding))
            4 -> NfcTestEkrani(Modifier.padding(icPadding))
        }
    }
}

@Composable
fun AnaSayfaIcerik(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    var havaDurumu by remember { mutableStateOf(havaDurumuOnbellekOku(context)) }
    var izinVar by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var adimSayisi by remember { mutableStateOf<Long?>(null) }
    var saglikDurumMesaji by remember { mutableStateOf("Kontrol ediliyor...") }
    val kapsam = rememberCoroutineScope()

    val izinIstegi = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi -> izinVar = verildi }

    val saglikIzinIstegi = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { verilenIzinler ->
        if (verilenIzinler.containsAll(HEALTH_CONNECT_IZINLERI)) {
            kapsam.launch {
                when (val sonuc = bugunkuAdimSayisiDetayli(context)) {
                    is AdimSonucu.Basarili -> { adimSayisi = sonuc.adim; saglikDurumMesaji = "" }
                    is AdimSonucu.Hata -> saglikDurumMesaji = "Hata: ${sonuc.mesaj}"
                }
            }
        } else {
            saglikDurumMesaji = "Sağlık izni verilmedi"
        }
    }

    LaunchedEffect(Unit) {
        if (!izinVar) {
            izinIstegi.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (!healthConnectKurulumuVarMi(context)) {
            saglikDurumMesaji = "Health Connect uygulaması kurulu değil"
        } else {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val mevcutIzinler = client.permissionController.getGrantedPermissions()
                if (mevcutIzinler.containsAll(HEALTH_CONNECT_IZINLERI)) {
                    when (val sonuc = bugunkuAdimSayisiDetayli(context)) {
                        is AdimSonucu.Basarili -> { adimSayisi = sonuc.adim; saglikDurumMesaji = "" }
                        is AdimSonucu.Hata -> saglikDurumMesaji = "Hata: ${sonuc.mesaj}"
                    }
                } else {
                    saglikDurumMesaji = "Sağlık izni bekleniyor"
                    saglikIzinIstegi.launch(HEALTH_CONNECT_IZINLERI)
                }
            } catch (e: Exception) {
                saglikDurumMesaji = "Bağlantı hatası: ${e.message ?: e.javaClass.simpleName}"
            }
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
        if (activity?.odakModuAktif?.value == true) {
            OdakModuBanner()
            Spacer(modifier = Modifier.height(16.dp))
        }
        KonumHavaDurumuKarti(havaDurumu, izinVar) { izinIstegi.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
        Spacer(modifier = Modifier.height(16.dp))
        SaglikPaneli(adimSayisi, saglikDurumMesaji)
        Spacer(modifier = Modifier.height(16.dp))
        OneriKarti()
    }
}

@Composable
fun OdakModuBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B3A))
    ) {
        Text(
            text = "🎯 Odak Modu Aktif — kartı tekrar okutunca durur",
            modifier = Modifier.padding(16.dp),
            color = Color.White
        )
    }
}

@Composable
fun KonumHavaDurumuKarti(veri: HavaDurumuVerisi?, izinVar: Boolean, izinIste: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = veri?.sehir ?: "Konumunuz", style = MaterialTheme.typography.titleMedium)
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
                    Icon(imageVector = Icons.Outlined.WbCloudy, contentDescription = "Hava durumu", modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (veri != null) "${veri.sicaklik.toInt()}°C" else "--°C",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            if (!izinVar) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = izinIste) { Text("Konum iznini ver") }
            }
        }
    }
}

@Composable
fun SaglikPaneli(adimSayisi: Long?, durumMesaji: String) {
    Column {
        Text(text = "Sağlık Özeti", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SaglikKarti(
                Icons.Outlined.DirectionsWalk,
                "Adım",
                adimSayisi?.toString() ?: (if (durumMesaji.isNotEmpty()) durumMesaji else "..."),
                Modifier.weight(1f)
            )
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
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(ikon, contentDescription = baslik, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = deger, style = MaterialTheme.typography.titleMedium)
            Text(text = baslik, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OneriKarti() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
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

@Composable
fun SohbetEkrani(modifier: Modifier = Modifier) {
    var mod by remember { mutableStateOf(AsistanModu.MUTLU) }
    var konusuyor by remember { mutableStateOf(false) }
    var metinGirisi by remember { mutableStateOf("") }
    var balondakiMetin by remember { mutableStateOf("Nasıl yardımcı olabilirim? 💡") }
    
    val mesajlar = remember {
        mutableStateListOf(
            SohbetMesaji("Merhaba! Ben senin kişisel asistanınım. Sana nasıl yardımcı olabilirim?", kullaniciMi = false)
        )
    }
    
    val listState = rememberLazyListState()
    val kapsam = rememberCoroutineScope()

    LaunchedEffect(mesajlar.size) {
        listState.animateScrollToItem(mesajlar.size)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HTML'deki Konuşma Balonu & Ses Dalgaları Entegre Edilmiş Maskot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Konuşma Balonu
            AnimatedVisibility(
                visible = konusuyor || balondakiMetin.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF38BDF8), shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = balondakiMetin,
                        color = Color(0xFF0B0F19),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ses Dalgaları (Talking Waves)
            AnimatedVisibility(visible = konusuyor) {
                SesDalgalari()
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Maskot Komponenti
            AsistanMaskot(
                mod = mod,
                konusuyor = konusuyor,
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mesaj Listesi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mesajlar) { mesaj ->
                MesajBalonu(mesaj)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mesaj Gönderme Alanı
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = metinGirisi,
                onValueChange = { metinGirisi = it },
                placeholder = { Text("Bir mesaj yazın...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (metinGirisi.isNotBlank()) {
                        val gonderilen = metinGirisi
                        mesajlar.add(SohbetMesaji(gonderilen, kullaniciMi = true))
                        metinGirisi = ""
                        
                        kapsam.launch {
                            mod = AsistanModu.SASKIN
                            balondakiMetin = "Düşünüyorum... 🤔"
                            delay(1200)
                            
                            mod = AsistanModu.MUTLU
                            konusuyor = true
                            val cevap = "Harika! '$gonderilen' hakkında çalışıyorum."
                            balondakiMetin = cevap
                            mesajlar.add(SohbetMesaji(cevap, kullaniciMi = false))
                            
                            delay(3000)
                            konusuyor = false
                            mod = AsistanModu.SAKIN
                            balondakiMetin = "Başka bir şey var mı? 💡"
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gönder",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SesDalgalari() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    val height1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp)
    ) {
        Box(Modifier.width(3.dp).height(height1.dp).background(Color(0xFF38BDF8), CircleShape))
        Box(Modifier.width(3.dp).height(height2.dp).background(Color(0xFF38BDF8), CircleShape))
        Box(Modifier.width(3.dp).height(height3.dp).background(Color(0xFF38BDF8), CircleShape))
    }
}

@Composable
fun MesajBalonu(mesaj: SohbetMesaji) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (mesaj.kullaniciMi) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (mesaj.kullaniciMi) 16.dp else 4.dp,
                        bottomEnd = if (mesaj.kullaniciMi) 4.dp else 16.dp
                    )
                )
                .background(
                    if (mesaj.kullaniciMi) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = mesaj.metin,
                color = if (mesaj.kullaniciMi) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun NfcTestEkrani(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val okunanId = activity?.sonOkunanNfcId?.value

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("NFC Kart Okuma Testi", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Kartını telefonun arkasına dokundur.")
        Spacer(modifier = Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp)) {
            Text(
                text = okunanId ?: "Henüz kart okunmadı",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnaEkranOnizleme() {
    AnaEkranIskelet()
}
