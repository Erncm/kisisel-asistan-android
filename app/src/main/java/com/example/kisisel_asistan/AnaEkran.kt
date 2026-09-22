package com.example.kisisel_asistan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
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
        Crossfade(
            targetState = seciliSekme,
            animationSpec = tween(durationMillis = 300),
            label = "SekmeGecisAnimasyonu"
        ) { sekmeIndex ->
            when (sekmeIndex) {
                0 -> AnaSayfaIcerik(Modifier.padding(icPadding))
                1 -> GelismisSohbetEkrani(Modifier.padding(icPadding))
                2 -> Text("Sağlık Detay Ekranı", modifier = Modifier.padding(icPadding))
                3 -> Text("Arama Ekranı", modifier = Modifier.padding(icPadding))
                4 -> NfcTestEkrani(Modifier.padding(icPadding))
            }
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

    val saglikIzniniIste: () -> Unit = {
        if (healthConnectKurulumuVarMi(context)) {
            saglikIzinIstegi.launch(HEALTH_CONNECT_IZINLERI)
        } else {
            saglikDurumMesaji = "Health Connect kurulu değil"
        }
    }

    LaunchedEffect(Unit) {
        if (!izinVar) {
            izinIstegi.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (!healthConnectKurulumuVarMi(context)) {
            saglikDurumMesaji = "Health Connect kurulu değil"
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
                    saglikDurumMesaji = "Sağlık izni verilmedi"
                }
            } catch (e: Exception) {
                saglikDurumMesaji = "Bağlantı hatası"
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
                // Konum izni verilmedi
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
        SaglikPaneli(adimSayisi, saglikDurumMesaji, onSaglikKartTiklama = saglikIzniniIste)
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
fun SaglikPaneli(adimSayisi: Long?, durumMesaji: String, onSaglikKartTiklama: () -> Unit) {
    Column {
        Text(text = "Sağlık Özeti", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SaglikKarti(
                Icons.Outlined.DirectionsWalk,
                "Adım",
                adimSayisi?.toString() ?: (if (durumMesaji.isNotEmpty()) durumMesaji else "..."),
                Modifier.weight(1f),
                onClick = onSaglikKartTiklama
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
fun SaglikKarti(
    ikon: ImageVector,
    baslik: String,
    deger: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp)
    ) {
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

// ==================== ANİMASYONLU SOHBET BÖLÜMÜ ====================

data class Mesaj(
    val id: String = java.util.UUID.randomUUID().toString(),
    val metin: String,
    val gonderenKullaniciMi: Boolean
)

@Composable
fun GelismisSohbetEkrani(modifier: Modifier = Modifier) {
    var mesajMetni by remember { mutableStateOf("") }
    val mesajlar = remember {
        mutableStateListOf(
            Mesaj(metin = "Merhaba! Size nasıl yardımcı olabilirim?", gonderenKullaniciMi = false)
        )
    }
    var yukleniyor by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val mesajGonder = { metin: String ->
        if (metin.isNotBlank()) {
            val yeniKullaniciMesaji = Mesaj(metin = metin, gonderenKullaniciMi = true)
            mesajlar.add(yeniKullaniciMesaji)
            mesajMetni = ""
            yukleniyor = true

            scope.launch {
                listState.animateScrollToItem(mesajlar.size - 1)
                delay(1200)

                val yanit = when {
                    metin.contains("adım", ignoreCase = true) -> "Bugünkü adım sayınızı Sağlık Özeti kartından takip edebilirsiniz."
                    metin.contains("hava", ignoreCase = true) -> "Konumunuza göre hava durumu Ana Sayfada görüntülenmektedir."
                    metin.contains("su", ignoreCase = true) -> "Günlük su hedefinize ulaşmak için saat başı bir bardak su içmeyi unutmayın!"
                    else -> "Anladım, bu konuda size yardımcı olmak için arka planda çalışıyorum."
                }

                mesajlar.add(Mesaj(metin = yanit, gonderenKullaniciMi = false))
                yukleniyor = false
                listState.animateScrollToItem(mesajlar.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Üst Başlık
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Kişisel Asistan",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (yukleniyor) "Yazıyor..." else "Çevrimiçi",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (yukleniyor) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }

        // Mesaj Listesi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(mesajlar, key = { it.id }) { mesaj ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300))
                ) {
                    MesajBalon(mesaj = mesaj)
                }
            }
            if (yukleniyor) {
                item {
                    YaziyorGostergesi()
                }
            }
        }

        // Hızlı Kısayol Chip'leri
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val kisayollar = listOf("📊 Adım Sayım", "🌦️ Hava Durumu", "💧 Su İçeceğim", "💡 Öneri Ver")
            items(kisayollar) { kisayol ->
                AssistChip(
                    onClick = { mesajGonder(kisayol) },
                    label = { Text(kisayol, fontSize = 12.sp) }
                )
            }
        }

        // Girdi Alanı
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = mesajMetni,
                    onValueChange = { mesajMetni = it },
                    placeholder = { Text("Bir mesaj yazın...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { mesajGonder(mesajMetni) })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { mesajGonder(mesajMetni) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Gönder",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun YaziyorGostergesi() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Asistan yanıt üretiyor...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MesajBalon(mesaj: Mesaj) {
    val hiza = if (mesaj.gonderenKullaniciMi) Alignment.CenterEnd else Alignment.CenterStart
    val arkaPlanRengi = if (mesaj.gonderenKullaniciMi) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val yaziRengi = if (mesaj.gonderenKullaniciMi) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val koseKavisleri = if (mesaj.gonderenKullaniciMi) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = hiza
    ) {
        Surface(
            color = arkaPlanRengi,
            shape = koseKavisleri,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = mesaj.metin,
                color = yaziRengi,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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
