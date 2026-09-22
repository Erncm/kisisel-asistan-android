package com.example.kisisel_asistan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                4 -> AyarlarEkrani(Modifier.padding(icPadding))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = veri?.sehir ?: "Konumunuz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
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
        Text(text = "Sağlık Özeti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(ikon, contentDescription = baslik, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = deger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = baslik, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OneriKarti() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Bugün için öneri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Henüz yeterli veri yok, kullanmaya devam ettikçe kişisel öneriler burada görünecek.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== ZENGİN SOHBET VE EYLEM BÖLÜMÜ ====================

data class Mesaj(
    val id: String = java.util.UUID.randomUUID().toString(),
    val metin: String,
    val gonderenKullaniciMi: Boolean,
    val saat: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

@Composable
fun GelismisSohbetEkrani(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mesajMetni by remember { mutableStateOf("") }
    val mesajlar = remember {
        mutableStateListOf(
            Mesaj(metin = "Merhaba! Bana seslenebilir veya komut verebilirsin.", gonderenKullaniciMi = false)
        )
    }
    var yukleniyor by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val cihazEylemiIsle: (String) -> String? = { metin ->
        val kucukMetin = metin.lowercase()
        when {
            kucukMetin.contains("whatsapp") && kucukMetin.contains("mesaj") -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(metin))
                    context.startActivity(intent)
                    "WhatsApp yönlendirmesi başlatıldı."
                } catch (e: Exception) {
                    "WhatsApp cihazınızda bulunamadı."
                }
            }
            kucukMetin.contains("diziwatch") || kucukMetin.contains("anime") -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://diziwatch.net"))
                    context.startActivity(intent)
                    "Diziwatch açılıyor, en son izlediğiniz içeriğe yönlendiriliyorsunuz..."
                } catch (e: Exception) {
                    "Web tarayıcı açılamadı."
                }
            }
            else -> null
        }
    }

    val mesajGonder = { metin: String ->
        if (metin.isNotBlank()) {
            val yeniKullaniciMesaji = Mesaj(metin = metin, gonderenKullaniciMi = true)
            mesajlar.add(yeniKullaniciMesaji)
            mesajMetni = ""
            yukleniyor = true

            scope.launch {
                listState.animateScrollToItem(mesajlar.size - 1)
                delay(1000)

                val eylemSonucu = cihazEylemiIsle(metin)
                val yanit = eylemSonucu ?: when {
                    metin.contains("adım", ignoreCase = true) -> "Bugünkü adım verinizi Ana Sayfadaki Sağlık Özeti paneli üzerinden görebilirsiniz."
                    metin.contains("hava", ignoreCase = true) -> "Anlık konum hava durumu kartı Ana Sayfada güncel olarak listelenmektedir."
                    else -> "Komut anlaşıldı, cihaz üzerinde işleniyor."
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
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Kişisel Asistan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (yukleniyor) "İşlem Yapılıyor..." else "Sesli/Yazılı Dinlemede",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (yukleniyor) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(mesajlar, key = { it.id }) { mesaj ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(250))
                ) {
                    MesajBalon(mesaj = mesaj)
                }
            }
            if (yukleniyor) {
                item { YaziyorGostergesi() }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val kisayollar = listOf("💬 WhatsApp'tan yaz", "🎬 Diziwatch Aç", "📊 Adım Sayım", "🌦️ Hava Durumu")
            items(kisayollar) { kisayol ->
                AssistChip(onClick = { mesajGonder(kisayol) }, label = { Text(kisayol, fontSize = 12.sp) })
            }
        }

        Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = mesajMetni,
                    onValueChange = { mesajMetni = it },
                    placeholder = { Text("Bir mesaj veya komut verin...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { mesajGonder(mesajMetni) })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { if (mesajMetni.isNotBlank()) mesajGonder(mesajMetni) },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (mesajMetni.isNotBlank()) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                        contentDescription = "Gönder/Dinle",
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
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        Box(modifier = Modifier.size(10.dp).scale(scale).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Asistan çalışıyor...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun MesajBalon(mesaj: Mesaj) {
    val hiza = if (mesaj.gonderenKullaniciMi) Alignment.CenterEnd else Alignment.CenterStart
    val arkaPlanRengi = if (mesaj.gonderenKullaniciMi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val yaziRengi = if (mesaj.gonderenKullaniciMi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val koseKavisleri = if (mesaj.gonderenKullaniciMi) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = hiza) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = if (mesaj.gonderenKullaniciMi) Arrangement.End else Arrangement.Start) {
            if (!mesaj.gonderenKullaniciMi) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(horizontalAlignment = if (mesaj.gonderenKullaniciMi) Alignment.End else Alignment.Start) {
                Surface(color = arkaPlanRengi, shape = koseKavisleri, modifier = Modifier.widthIn(max = 280.dp)) {
                    Text(text = mesaj.metin, color = yaziRengi, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = mesaj.saat, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

// ==================== YENİ AYARLAR VE YEREL MODEL YÖNETİMİ ====================

@Composable
fun AyarlarEkrani(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pref = remember { context.getSharedPreferences("AsistanAyarlari", Context.MODE_PRIVATE) }
    var tetiklemeKelimesi by remember { mutableStateOf(pref.getString("wake_word", "Hey Asistan") ?: "Hey Asistan") }
    var sesliYanitAktif by remember { mutableStateOf(pref.getBoolean("sesli_yanit", true)) }
    var yukluModelAdi by remember { mutableStateOf(pref.getString("selected_model_name", "Yüklü Model Yok") ?: "Yüklü Model Yok") }

    // Cihazdan Model Dosyası Seçme İşi (.bin veya .task)
    val dosyaSecici = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val dosyaAdi = "local_model.bin"
            val hedefDosya = File(context.filesDir, dosyaAdi)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    hedefDosya.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                pref.edit().putString("selected_model_name", "Cihaz İçi Model (Özel)").apply()
                yukluModelAdi = "Cihaz İçi Model (Özel)"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Asistan Ayarları", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Yerel Yapay Zeka (Local AI) Model Paneli
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Yerel AI Model Yönetimi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Offline çalışacak hafif LLM modelini cihazınıza aktarın veya yükleyin.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Aktif Model: $yukluModelAdi", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { dosyaSecici.launch("*/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dosya Seç (.bin)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tetikleme Kelimesi Kartı
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Özel Tetikleme Kelimesi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Asistanı sesle uyandırmak için kullanacağınız kelime.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tetiklemeKelimesi,
                    onValueChange = {
                        tetiklemeKelimesi = it
                        pref.edit().putString("wake_word", it).apply()
                    },
                    label = { Text("Tetikleme Kelimesi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cihaz Kontrol İzinleri
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cihaz Kontrol İzinleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("WhatsApp ve uygulamalarda otomatik işlem yapabilmek için erişilebilirlik iznini etkinleştirin.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Erişilebilirlik İznini Aç")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sesli Yanıt Ayarı
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sesli Yanıt (TTS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Asistan yanıtları sesli okusun.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Switch(
                    checked = sesliYanitAktif,
                    onCheckedChange = {
                        sesliYanitAktif = it
                        pref.edit().putBoolean("sesli_yanit", it).apply()
                    }
                )
            }
        }
    }
}
