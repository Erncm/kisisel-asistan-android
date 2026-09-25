package com.example.kisisel_asistan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale
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
    val mesajlar = SohbetDurumu.aktifMesajlar
    var girdi by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(false) }
    var durumMesaji by remember { mutableStateOf<String?>(null) }
    var gecmisAcik by remember { mutableStateOf(false) }
    var sesliOkumaAcik by remember { mutableStateOf(false) }
    val listeDurumu = rememberLazyListState()

    val sesTanimaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { sonuc ->
        val metinler = sonuc.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val tanininMetin = metinler?.firstOrNull()
        if (!tanininMetin.isNullOrBlank()) {
            girdi = if (girdi.isBlank()) tanininMetin else "$girdi $tanininMetin"
        }
    }

    val mikrofonIzinIstegi = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi ->
        if (verildi) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dinliyorum...")
            }
            sesTanimaLauncher.launch(intent)
        }
    }

    fun sesleGirdiBaslat() {
        val izinVarMi = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (izinVarMi) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dinliyorum...")
            }
            sesTanimaLauncher.launch(intent)
        } else {
            mikrofonIzinIstegi.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(mesajlar.size) {
        if (mesajlar.isNotEmpty()) {
            listeDurumu.animateScrollToItem(mesajlar.size - 1)
        }
    }

    fun asistanYanitiEkle(metin: String) {
        SohbetDurumu.mesajEkle(ChatMesaj(metin, benMi = false))
        if (sesliOkumaAcik) {
            TTSYoneticisi.oku(metin)
        }
    }

    fun gonder() {
        val girdiTrim = girdi.trim()
        if (girdiTrim.isBlank()) return

        if (whatsAppKomutuMu(girdiTrim)) {
            SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true))
            val sonucMesaji = whatsAppKomutunuCalistir(context, girdiTrim)
            SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji ?: "Komut anlaşılamadı", benMi = false))
            girdi = ""
            return
        }

        if (hafizaKomutuMu(girdiTrim)) {
            val oncekiMesaj = mesajlar.lastOrNull()?.icerik
            val icerik = hafizaIcerigiCikar(girdiTrim, oncekiMesaj)
            SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true))
            val kayit = HafizaDeposu.ekle(icerik)
            SohbetDurumu.mesajEkle(ChatMesaj("Not edildi ✓ [${kayit.kod}]: ${kayit.ozet}", benMi = false))
            girdi = ""
            return
        }

        val kullaniciMesaji = ChatMesaj(girdiTrim, benMi = true)
        SohbetDurumu.mesajEkle(kullaniciMesaji)
        girdi = ""
        durumMesaji = null
        yukleniyor = true

        kapsam.launch {
            val anahtar = apiAnahtariOku(context)
            var gemeniDenendi = false
            val hafizaOzeti = HafizaDeposu.hepsiniOzetGetir()

            if (anahtar.isNotBlank()) {
                gemeniDenendi = true
                val gonderilecekListe = if (hafizaOzeti.isNotBlank()) {
                    listOf(
                        ChatMesaj("Kullanıcı hakkında bildiğim notlar:\n$hafizaOzeti", benMi = true),
                        ChatMesaj("Anladım, bu bilgileri göz önünde bulunduracağım.", benMi = false)
                    ) + mesajlar.toList()
                } else {
                    mesajlar.toList()
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
                val hazir = YerelModel.hazirla(context)
                if (hazir) {
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
            Text("Sohbet", style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = {
                    sesliOkumaAcik = !sesliOkumaAcik
                    if (!sesliOkumaAcik) TTSYoneticisi.durdur()
                }) {
                    Icon(
                        if (sesliOkumaAcik) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                        contentDescription = "Sesli okuma"
                    )
                }
                IconButton(onClick = { gecmisAcik = true }) {
                    Icon(Icons.Outlined.History, contentDescription = "Geçmiş")
                }
                IconButton(onClick = { SohbetDurumu.yeniSohbetBaslat() }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Yeni sohbet")
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

        if (yukleniyor) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        if (durumMesaji != null) {
            Text(
                text = durumMesaji ?: "",
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
                placeholder = { Text("Bir şeyler yaz... (\"hatırla: ...\" ile not al)") }
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { sesleGirdiBaslat() }) {
                Icon(Icons.Outlined.Mic, contentDescription = "Sesle yaz")
            }
            IconButton(onClick = { gonder() }) {
                Icon(Icons.Outlined.Send, contentDescription = "Gönder")
            }
        }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
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
                containerColor = if (mesaj.benMi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(text = mesaj.icerik, modifier = Modifier.padding(12.dp))
        }
        if (!mesaj.benMi) {
            IconButton(onClick = { TTSYoneticisi.oku(mesaj.icerik) }) {
                Icon(Icons.Outlined.VolumeUp, contentDescription = "Sesli oku", modifier = Modifier.width(20.dp))
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
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
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
                                val sonuc = modelIndir(context) { yuzde ->
                                    ilerlemeYuzdesi = yuzde
                                }
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
    }
}

@Composable
fun OtomasyonAyarBolumu(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text("WhatsApp Otomasyonu", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sohbette \"whatsaptan Ahmet'e ... yaz\" gibi yazarak WhatsApp mesajı gönderebilirsin. Önce aşağıdaki izni açman gerekiyor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) {
            Text("Erişilebilirlik İznini Aç")
        }
        if (OtomasyonKuyrugu.durum.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Son durum: ${OtomasyonKuyrugu.durum}", style = MaterialTheme.typography.bodySmall)
        }
    }
        Spacer(modifier = Modifier.height(32.dp))
        OtomasyonAyarBolumu()
    }
