package com.example.kisisel_asistan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlin.math.hypot
import kotlinx.coroutines.launch

@Composable
fun AnaEkranIskelet() {
    var seciliSekme by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = SamanthaTheme.bg,
        bottomBar = {
            LiquidBottomBar(selected = seciliSekme, onSelect = { seciliSekme = it })
        }
    ) { icPadding ->
        Crossfade(targetState = seciliSekme, label = "tab-content") { tab ->
            Box(modifier = Modifier.padding(icPadding).fillMaxSize()) {
                when (tab) {
                    0 -> AnaSayfaIcerik()
                    1 -> SohbetEkrani()
                    2 -> Text("Sağlık ekranı", modifier = Modifier.padding(16.dp), color = SamanthaTheme.ink)
                    3 -> Text("Ara ekranı", modifier = Modifier.padding(16.dp), color = SamanthaTheme.ink)
                    4 -> AyarlarEkrani()
                }
            }
        }
    }
}

@Composable
fun SohbetEkrani(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val mesajlar = SohbetDurumu.aktifMesajlar
    var girdi by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(false) }
    var dusunmeMetni by remember { mutableStateOf("") }
    var durumMesaji by remember { mutableStateOf<String?>(null) }
    var gecmisAcik by remember { mutableStateOf(false) }
    var sesliOkumaAcik by remember { mutableStateOf(false) }
    var secilenGorselUri by remember { mutableStateOf<Uri?>(null) }
    var dinliyorMu by remember { mutableStateOf(false) }
    var sesDurumu by remember { mutableStateOf("") }
    val listeDurumu = rememberLazyListState()

    val gorselSeciciLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> secilenGorselUri = uri }

    fun sesTanimayiBaslat() {
        dinliyorMu = true
        SesliGirisYoneticisi.dinlemeyeBasla(
            context = context,
            onSonuc = { metin -> girdi = if (girdi.isBlank()) metin else "$girdi $metin" },
            onDurum = { durum -> sesDurumu = durum },
            onBitti = { dinliyorMu = false; sesDurumu = "" }
        )
    }

    val mikrofonIzinIstegi = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi -> if (verildi) sesTanimayiBaslat() }

    fun sesleGirdiBaslat() {
        val izinVarMi = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (izinVarMi) sesTanimayiBaslat() else mikrofonIzinIstegi.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        DisaridanGelenMetin.bekleyenMetin.value?.let { metin ->
            girdi = metin
            DisaridanGelenMetin.bekleyenMetin.value = null
        }
    }

    LaunchedEffect(mesajlar.size) {
        if (mesajlar.isNotEmpty()) {
            listeDurumu.animateScrollToItem(mesajlar.size - 1)
        }
    }

    fun asistanYanitiEkle(metin: String) {
        SohbetDurumu.mesajEkle(ChatMesaj(metin, benMi = false))
        if (sesliOkumaAcik) TTSYoneticisi.oku(metin)
    }

    fun gonder() {
        val girdiTrim = girdi.trim()
        if (girdiTrim.isBlank()) return

        if (whatsAppKomutuMu(girdiTrim)) {
            SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
            val sonucMesaji = whatsAppKomutunuCalistir(context, girdiTrim)
            SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji ?: "Komut anlaşılamadı", benMi = false, baglamaDahilMi = false))
            girdi = ""
            return
        }

        if (youtubeKomutuMu(girdiTrim)) {
            SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
            val sonucMesaji = youtubeKomutunuCalistir(context, girdiTrim)
            SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji ?: "Komut anlaşılamadı", benMi = false, baglamaDahilMi = false))
            girdi = ""
            return
        }

        val eslesenUygulama = genelKomutMu(girdiTrim)
        if (eslesenUygulama != null) {
            SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
            val sonucMesaji = genelKomutuCalistir(context, girdiTrim, eslesenUygulama)
            SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji, benMi = false, baglamaDahilMi = false))
            girdi = ""
            return
        }

        if (hafizaKomutuMu(girdiTrim)) {
            val oncekiMesaj = mesajlar.lastOrNull()?.icerik
            val icerik = hafizaIcerigiCikar(girdiTrim, oncekiMesaj)
            SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
            val kayit = HafizaDeposu.ekle(icerik)
            SohbetDurumu.mesajEkle(ChatMesaj("Not edildi ✓ [${kayit.kod}]: ${kayit.ozet}", benMi = false, baglamaDahilMi = false))
            girdi = ""
            return
        }

        var gorselBase64: String? = null
        var gorselMime: String? = null
        secilenGorselUri?.let { uri ->
            gorseliBase64eCevir(context, uri)?.let { (base64, mime) ->
                gorselBase64 = base64
                gorselMime = mime
            }
        }
        val kullaniciMesaji = ChatMesaj(girdiTrim, benMi = true, gorselBase64 = gorselBase64, gorselMimeTipi = gorselMime)
        SohbetDurumu.mesajEkle(kullaniciMesaji)
        girdi = ""
        secilenGorselUri = null
        durumMesaji = null
        yukleniyor = true
        dusunmeMetni = "Düşünüyor"

        kapsam.launch {
            val anahtar = apiAnahtariOku(context)
            var gemeniDenendi = false
            val hafizaOzeti = HafizaDeposu.hepsiniOzetGetir()
            val baglamGecmisi = mesajlar.filter { it.baglamaDahilMi }

            if (anahtar.isNotBlank()) {
                gemeniDenendi = true
                dusunmeMetni = "Gemini'ye soruluyor"
                val gonderilecekListe = if (hafizaOzeti.isNotBlank()) {
                    listOf(
                        ChatMesaj("Kullanıcı hakkında bildiğim notlar:\n$hafizaOzeti", benMi = true),
                        ChatMesaj("Anladım, bu bilgileri göz önünde bulunduracağım.", benMi = false)
                    ) + baglamGecmisi
                } else {
                    baglamGecmisi
                }
                val sonuc = geminiYanitAl(anahtar, gonderilecekListe)
                if (sonuc.isSuccess) {
                    asistanYanitiEkle(sonuc.getOrDefault(""))
                    yukleniyor = false
                    return@launch
                }
            }

            if (modelVarMi(context)) {
                durumMesaji = if (gemeniDenendi) "İnternet yok, yerel modele geçiliyor..." else "Yerel model kullanılıyor..."
                dusunmeMetni = "Yerel model yükleniyor"
                val hazir = YerelModel.hazirla(context)
                if (hazir) {
                    dusunmeMetni = "Yerel model düşünüyor"
                    val girdiMetniSon = if (hafizaOzeti.isNotBlank()) {
                        "[Hafıza notların]\n$hafizaOzeti\n\nKullanıcı: $girdiTrim"
                    } else girdiTrim
                    val yanit = YerelModel.yanitAl(girdiMetniSon)
                    asistanYanitiEkle(yanit)
                    durumMesaji = null
                } else {
                    durumMesaji = "Yerel model yüklenemedi"
                }
            } else {
                durumMesaji = if (gemeniDenendi) "İnternet yok ve yerel model kurulu değil (Ayarlar'dan indirebilirsin)" else "Önce Ayarlar'dan Gemini anahtarı gir ya da yerel modeli indir"
            }

            yukleniyor = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sohbet", style = MaterialTheme.typography.titleMedium, color = SamanthaTheme.ink)
            Row {
                IconButton(onClick = {
                    sesliOkumaAcik = !sesliOkumaAcik
                    if (!sesliOkumaAcik) TTSYoneticisi.durdur()
                }) {
                    Icon(
                        if (sesliOkumaAcik) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                        contentDescription = "Sesli okuma",
                        tint = SamanthaTheme.ink
                    )
                }
                IconButton(onClick = { gecmisAcik = true }) {
                    Icon(Icons.Outlined.History, contentDescription = "Geçmiş", tint = SamanthaTheme.ink)
                }
                IconButton(onClick = { SohbetDurumu.yeniSohbetBaslat() }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Yeni sohbet", tint = SamanthaTheme.ink)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            state = listeDurumu
        ) {
            items(mesajlar) { mesaj ->
                MesajBalonu(mesaj)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        AsistanDusunuyorGostergesi(
            visible = yukleniyor,
            text = dusunmeMetni,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        AsistanDusunuyorGostergesi(
            visible = OtomasyonBeyni.aktifGorev != null,
            text = OtomasyonKuyrugu.durum,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (durumMesaji != null) {
            Text(
                text = durumMesaji ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (dinliyorMu && sesDurumu.isNotBlank()) {
            Text(
                text = sesDurumu,
                color = SamanthaTheme.muted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        secilenGorselUri?.let {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📎 Görsel eklendi", style = MaterialTheme.typography.bodySmall, color = SamanthaTheme.muted)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { secilenGorselUri = null }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Görseli kaldır", tint = SamanthaTheme.muted)
                }
            }
        }

        VoiceAssistantBar(
            modifier = Modifier.padding(16.dp),
            audioVolume = if (dinliyorMu) 0.9f else if (yukleniyor) 0.6f else 0.15f,
            onSendMessage = { metin ->
                girdi = metin
                gonder()
            },
            onAttachClick = { gorselSeciciLauncher.launch("image/*") },
            onOrbLongPress = { sesleGirdiBaslat() }
        )
    }

    if (gecmisAcik) {
        AlertDialog(
            onDismissRequest = { gecmisAcik = false },
            confirmButton = {
                Button(onClick = { gecmisAcik = false }) { Text("Kapat") }
            },
            title = { Text("Sohbet Geçmişi") },
            text = {
                if (SohbetDurumu.gecmisOturumlar.isEmpty()) {
                    Text("Henüz geçmiş sohbet yok")
                } else {
                    Column {
                        SohbetDurumu.gecmisOturumlar.forEach { oturum ->
                            val ozet = oturum.mesajlar.firstOrNull()?.icerik?.take(40) ?: "Boş sohbet"
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(ozet, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = {
                                        SohbetDurumu.oturumuYukle(oturum)
                                        gecmisAcik = false
                                    }) {
                                        Text("Bu sohbete dön")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MesajBalonu(mesaj: ChatMesaj) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mesaj.benMi) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (mesaj.benMi) SamanthaTheme.pill else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (mesaj.gorselBase64 != null) {
                    Text("📎 Görsel", style = MaterialTheme.typography.labelSmall, color = SamanthaTheme.muted)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(text = mesaj.icerik, color = SamanthaTheme.ink)
            }
        }
        if (!mesaj.benMi) {
            IconButton(onClick = { TTSYoneticisi.oku(mesaj.icerik) }) {
                Icon(Icons.Outlined.VolumeUp, contentDescription = "Sesli oku", modifier = Modifier.width(20.dp), tint = SamanthaTheme.muted)
            }
        }
    }
}

@Composable
fun GorunumAyarBolumu(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var revealColor by remember { mutableStateOf<Color?>(null) }
    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    var maxRadius by remember { mutableStateOf(0f) }
    val revealRadius = remember { Animatable(0f) }

    fun applyWithReveal(center: Offset, previewColor: Color, apply: () -> Unit) {
        scope.launch {
            revealCenter = center
            revealColor = previewColor
            revealRadius.snapTo(0f)
            revealRadius.animateTo(maxRadius, tween(450))
            apply()
            revealColor = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                val s = it.size
                maxRadius = hypot(s.width.toFloat(), s.height.toFloat())
            }
    ) {
        Column {
            Text("Görünüm", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Vurgu rengini seç — değişiklik katmanlı bir geçişle yayılır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ACCENT_PALETTE.forEach { color ->
                    val isActive = SamanthaTheme.accent == color
                    var swatchCenter by remember { mutableStateOf(Offset.Zero) }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(lerp(color, Color.White, 0.45f), color)))
                            .then(
                                if (isActive) Modifier.border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                else Modifier
                            )
                            .onGloballyPositioned { swatchCenter = it.boundsInRoot().center }
                            .clickable {
                                applyWithReveal(swatchCenter, color) { SamanthaTheme.accent = color }
                            }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Karanlık mod")
                var switchCenter by remember { mutableStateOf(Offset.Zero) }
                Switch(
                    checked = SamanthaTheme.isDark,
                    onCheckedChange = { checked ->
                        val target = if (checked) Color(0xFF151223) else Color(0xFFFAF8FF)
                        applyWithReveal(switchCenter, target) { SamanthaTheme.isDark = checked }
                    },
                    modifier = Modifier.onGloballyPositioned { switchCenter = it.boundsInRoot().center }
                )
            }
        }

        revealColor?.let { color ->
            Canvas(modifier = Modifier.fillMaxWidth()) {
                drawCircle(color = color, radius = revealRadius.value, center = revealCenter)
            }
        }
    }
}

@Composable
fun AyarlarEkrani(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val kapsam = rememberCoroutineScope()

    var anahtar by remember { mutableStateOf(apiAnahtariOku(context)) }
    var kaydedildi by remember { mutableStateOf(false) }

    var modelMevcut by remember { mutableStateOf(modelVarMi(context)) }
    var indiriliyor by remember { mutableStateOf(false) }
    var ilerlemeYuzdesi by remember { mutableStateOf(0) }
    var modelHata by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GorunumAyarBolumu()

        Spacer(modifier = Modifier.height(32.dp))
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

        Spacer(modifier = Modifier.height(32.dp))
        Text("Yerel AI Modeli (Çevrimdışı)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                when {
                    modelMevcut -> {
                        Text("Model telefonda kayıtlı ✓", color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            modelSil(context)
                            modelMevcut = false
                        }) {
                            Text("Modeli Sil")
                        }
                    }
                    indiriliyor -> {
                        Text("İndiriliyor: %$ilerlemeYuzdesi")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ilerlemeYuzdesi / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        Text("Model telefonda yok (~1GB, Wi-Fi önerilir)")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            indiriliyor = true
                            modelHata = null
                            kapsam.launch {
                                val sonuc = modelIndir(context) { yuzde -> ilerlemeYuzdesi = yuzde }
                                indiriliyor = false
                                when (sonuc) {
                                    is IndirmeSonucu.Basarili -> modelMevcut = true
                                    is IndirmeSonucu.Hata -> modelHata = sonuc.mesaj
                                }
                            }
                        }) {
                            Text("Modeli İndir")
                        }
                    }
                }
                if (modelHata != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hata: $modelHata", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Uzun Süreli Hafıza", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (HafizaDeposu.kayitlar.isEmpty()) {
            Text("Henüz kayıtlı bir bilgi yok — sohbette \"hatırla: ...\" yazarak ekleyebilirsin", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column {
                HafizaDeposu.kayitlar.forEach { kayit ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("[${kayit.kod}]", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(kayit.ozet, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { HafizaDeposu.sil(kayit.kod) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Sil")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        OtomasyonAyarBolumu()

        Spacer(modifier = Modifier.height(32.dp))
        EkranOkumaTestBolumu()

        Spacer(modifier = Modifier.height(32.dp))
        UyanmaAyarBolumu()

        Spacer(modifier = Modifier.height(32.dp))
        SabahOzetiAyarBolumu()
    }
}

@Composable
fun OtomasyonAyarBolumu(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text("Uygulama Otomasyonu", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sohbette \"whatsaptan Ahmet'e ... yaz\" ya da \"hesap makinesine gir 99*11911 yap\" gibi herhangi bir kurulu uygulama için komut verebilirsin. Önce aşağıdaki izni açman gerekiyor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) {
            Text("Erişilebilirlik İznini Aç")
        }
        if (OtomasyonKuyrugu.durum.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Son durum: ${OtomasyonKuyrugu.durum}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EkranOkumaTestBolumu(modifier: Modifier = Modifier) {
    var sonuc by remember { mutableStateOf("Henüz taranmadı") }

    Column(modifier = modifier) {
        Text("Ekran Okuma Testi", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val servis = AsistanErisilebilirlikServisi.aktifOrnek
            sonuc = if (servis == null) {
                "Servis aktif değil - önce erişilebilirlik iznini aç"
            } else {
                EkranOkuyucu.ekraniListele(servis.ekranKokunuGetir())
            }
        }) {
            Text("Şu Anki Ekranı Tara")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(12.dp)) {
            Text(sonuc, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun UyanmaAyarBolumu(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var kelime by remember { mutableStateOf(UyanmaAyarlari.kelimeyiOku(context)) }
    var aktif by remember { mutableStateOf(UyanmaAyarlari.aktifMi(context)) }

    val mikrofonIzinIstegi = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi ->
        if (verildi) {
            aktif = true
            UyanmaAyarlari.aktifligiKaydet(context, true)
            context.startForegroundService(Intent(context, UyanmaKelimesiServisi::class.java))
        }
    }

    Column(modifier = modifier) {
        Text("Uyanma Kelimesi (Deneysel)", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sesini tanıyan kişiye özel bir sistem DEĞİL — söylediğin cümlede seçtiğin kelimeyi metin olarak arıyor. 2-3 kelimelik farklı bir ifade seç.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = kelime,
            onValueChange = { kelime = it; UyanmaAyarlari.kelimeyiKaydet(context, it.trim()) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("örn: asistanım hazır mısın") }
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (aktif) "Dinleme açık" else "Dinleme kapalı")
            Switch(
                checked = aktif,
                onCheckedChange = { yeniDurum ->
                    if (kelime.isBlank()) return@Switch
                    if (yeniDurum) {
                        val izinVarMi = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (izinVarMi) {
                            aktif = true
                            UyanmaAyarlari.aktifligiKaydet(context, true)
                            context.startForegroundService(Intent(context, UyanmaKelimesiServisi::class.java))
                        } else {
                            mikrofonIzinIstegi.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        aktif = false
                        UyanmaAyarlari.aktifligiKaydet(context, false)
                        context.stopService(Intent(context, UyanmaKelimesiServisi::class.java))
                    }
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val i = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            i.data = Uri.parse("package:${context.packageName}")
            context.startActivity(i)
        }) {
            Text("Pil Optimizasyonunu Kapat")
        }
        Text(
            "Servisin ekran kapalıyken de çalışması için bu adım gerekiyor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SabahOzetiAyarBolumu(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var aktif by remember { mutableStateOf(SabahOzetiAyarlari.aktifMi(context)) }
    var saat by remember { mutableStateOf(SabahOzetiAyarlari.saatOku(context)) }
    var dakika by remember { mutableStateOf(SabahOzetiAyarlari.dakikaOku(context)) }

    val bildirimIzinIstegi = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi ->
        if (verildi) {
            aktif = true
            SabahOzetiAyarlari.kaydet(context, true, saat, dakika)
            SabahOzetiZamanlayici.zamanlaGunluk(context, saat, dakika)
        }
    }

    Column(modifier = modifier) {
        Text("Sabah Özeti Bildirimi", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Her gün seçtiğin saatte hava durumu ve hafızandaki notlardan kısa bir özet bildirim olarak gelir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = saat.toString(),
                onValueChange = { it.toIntOrNull()?.let { s -> if (s in 0..23) saat = s } },
                modifier = Modifier.width(70.dp),
                label = { Text("Saat") }
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = dakika.toString(),
                onValueChange = { it.toIntOrNull()?.let { d -> if (d in 0..59) dakika = d } },
                modifier = Modifier.width(70.dp),
                label = { Text("Dakika") }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (aktif) "Bildirim açık" else "Bildirim kapalı")
            Switch(
                checked = aktif,
                onCheckedChange = { yeniDurum ->
                    if (yeniDurum) {
                        val izinGerekliMi = android.os.Build.VERSION.SDK_INT >= 33
                        val izinVarMi = !izinGerekliMi || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        if (izinVarMi) {
                            aktif = true
                            SabahOzetiAyarlari.kaydet(context, true, saat, dakika)
                            SabahOzetiZamanlayici.zamanlaGunluk(context, saat, dakika)
                        } else {
                            bildirimIzinIstegi.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        aktif = false
                        SabahOzetiAyarlari.kaydet(context, false, saat, dakika)
                        SabahOzetiZamanlayici.iptalEt(context)
                    }
                }
            )
        }
    }
}

@Composable
fun AnaSayfaIcerik(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var havaDurumu by remember { mutableStateOf(havaDurumuOnbellekOku(context)) }
    var konumDurumu by remember { mutableStateOf("") }

    var adimSayisi by remember { mutableStateOf<Long?>(null) }
    var uykuSuresi by remember { mutableStateOf<java.time.Duration?>(null) }
    var saglikDurumu by remember { mutableStateOf("") }

    fun konumuIsle(konum: android.location.Location?) {
        if (konum == null) {
            konumDurumu = "Konum bulunamadı, GPS açık mı?"
            return
        }
        konumDurumu = "Konum alınıyor..."
        kapsam.launch {
            val veri = havaDurumuGetir(context, konum.latitude, konum.longitude)
            if (veri != null) {
                havaDurumu = veri
                havaDurumuOnbellekKaydet(context, veri)
                konumDurumu = ""
            } else {
                konumDurumu = "Hava durumu alınamadı"
            }
        }
    }

    val konumIzinIstegi = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi -> if (verildi) konumuIsle(sonBilinenKonumuAl(context)) else konumDurumu = "Konum izni verilmedi" }

    fun konumuGuncelle() {
        val izinVarMi = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (izinVarMi) konumuIsle(sonBilinenKonumuAl(context)) else konumIzinIstegi.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val saglikIzinIstegi = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { verilenIzinler ->
        if (verilenIzinler.containsAll(SAGLIK_IZINLERI)) {
            kapsam.launch {
                saglikDurumu = "Yükleniyor..."
                when (val sonuc = bugunkuSaglikVerisiniGetirDetayli(context)) {
                    is SaglikSonucu.Basarili -> {
                        adimSayisi = sonuc.veri.adimSayisi
                        uykuSuresi = sonuc.veri.uykuSuresi
                        saglikDurumu = ""
                    }
                    is SaglikSonucu.Hata -> saglikDurumu = sonuc.mesaj
                }
            }
        } else {
            saglikDurumu = "Sağlık izni verilmedi (Health Connect'ten manuel açabilirsin)"
        }
    }

    fun saglikVerisiniIsteVeGetir() {
        val durumKodu = healthConnectDurumKodu(context)
        if (durumKodu != HealthConnectClient.SDK_AVAILABLE) {
            saglikDurumu = when (durumKodu) {
                HealthConnectClient.SDK_UNAVAILABLE -> "Bu cihaz Health Connect'i desteklemiyor"
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect güncellenmeli"
                else -> "Health Connect kurulu değil"
            }
            return
        }
        kapsam.launch {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val mevcutIzinler = client.permissionController.getGrantedPermissions()
                if (mevcutIzinler.containsAll(SAGLIK_IZINLERI)) {
                    saglikDurumu = "Yükleniyor..."
                    when (val sonuc = bugunkuSaglikVerisiniGetirDetayli(context)) {
                        is SaglikSonucu.Basarili -> {
                            adimSayisi = sonuc.veri.adimSayisi
                            uykuSuresi = sonuc.veri.uykuSuresi
                            saglikDurumu = ""
                        }
                        is SaglikSonucu.Hata -> saglikDurumu = sonuc.mesaj
                    }
                } else {
                    saglikDurumu = "Sağlık izni isteniyor..."
                    saglikIzinIstegi.launch(SAGLIK_IZINLERI)
                }
            } catch (e: Exception) {
                saglikDurumu = "${e.javaClass.simpleName}: ${e.message ?: "bilinmeyen hata"}"
            }
        }
    }

    LaunchedEffect(Unit) { saglikVerisiniIsteVeGetir() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Merhaba", style = MaterialTheme.typography.headlineSmall, color = SamanthaTheme.ink)
        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(havaDurumu?.sehir ?: "Konumunuz", style = MaterialTheme.typography.titleMedium)
                        Text(
                            havaDurumu?.aciklama ?: "Hava durumu için konumu güncelle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (havaDurumu != null) "${havaDurumu!!.sicaklik.toInt()}°C" else "--°C",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { konumuGuncelle() }) {
                    Text("Konumu Güncelle")
                }
                if (konumDurumu.isNotBlank()) {
                    Text(konumDurumu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sağlık", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(adimSayisi?.toString() ?: "--", style = MaterialTheme.typography.headlineSmall)
                        Text("Adım", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        val uykuMetni = uykuSuresi?.let { "${it.toHours()}s ${it.toMinutes() % 60}dk" } ?: "--"
                        Text(uykuMetni, style = MaterialTheme.typography.headlineSmall)
                        Text("Uyku", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = { saglikVerisiniIsteVeGetir() }) {
                        Text("Sağlık İznini İste / Yenile")
                    }
                    TextButton(onClick = { healthConnectUygulamasiniAc(context) }) {
                        Text("Health Connect'i Aç")
                    }
                }
                if (saglikDurumu.isNotBlank()) {
                    Text(saglikDurumu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("İpucu", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                val notSayisi = HafizaDeposu.kayitlar.size
                Text(
                    if (notSayisi > 0) "Şu an $notSayisi kayıtlı notun var. Sohbette \"hatırla: ...\" yazarak yenisini ekleyebilirsin."
                    else "Sohbette \"hatırla: ...\" yazarak asistanının seni hatırlamasını sağlayabilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
