package com.example.kisisel_asistan

import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
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
            1 -> MaskotSohbetEkrani(Modifier.padding(icPadding))
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MaskotSohbetEkrani(modifier: Modifier = Modifier) {
    val htmlIcerik = """
    <!DOCTYPE html>
    <html lang="tr">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
      <title>AI Asistan & Floating Evcil Hayvan Maskotu</title>
      <style>
        :root {
          --bg-color: #030712;
          --card-bg: #0b0f19;
          --accent-color: #38bdf8;
          --eye-bg: #ffffff;
          --pupil-color: #0b0f19;
          --blush-color: #f43f5e;
          --glow-color: rgba(56, 189, 248, 0.2);
        }

        * {
          box-sizing: border-box;
          margin: 0;
          padding: 0;
          user-select: none;
          -webkit-user-select: none;
        }

        body {
          background-color: var(--bg-color);
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          align-items: center;
          min-height: 100vh;
          overflow: hidden;
          font-family: system-ui, -apple-system, sans-serif;
          touch-action: none;
          padding: 20px 10px;
          transition: background-color 0.8s ease;
        }

        #particleCanvas {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          pointer-events: none;
          z-index: 0;
        }

        .top-panel {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 12px;
          z-index: 10;
          width: 100%;
          max-width: 440px;
          transition: opacity 0.4s ease, transform 0.4s ease;
        }

        .mode-switch-container {
          display: flex;
          background: rgba(15, 23, 42, 0.85);
          padding: 4px;
          border-radius: 30px;
          border: 1px solid rgba(255, 255, 255, 0.1);
          backdrop-filter: blur(16px);
          width: 100%;
        }

        .mode-btn {
          flex: 1;
          padding: 10px;
          border: none;
          background: transparent;
          color: #94a3b8;
          font-size: 12px;
          font-weight: 700;
          border-radius: 25px;
          cursor: pointer;
          transition: all 0.3s ease;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 4px;
        }

        .mode-btn.active {
          background: var(--accent-color);
          color: #0b0f19;
          box-shadow: 0 4px 15px var(--glow-color);
        }

        .status-bar {
          color: var(--accent-color);
          font-size: 13px;
          font-weight: 600;
          background: rgba(15, 23, 42, 0.75);
          padding: 8px 22px;
          border-radius: 30px;
          border: 1px solid rgba(255, 255, 255, 0.08);
          backdrop-filter: blur(16px);
          letter-spacing: 0.5px;
          transition: all 0.5s;
          text-align: center;
          width: 100%;
        }

        .mascot-wrapper {
          position: relative;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
          margin: auto 0;
          z-index: 100;
          perspective: 1000px;
          transition: all 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
        }

        .speech-bubble {
          position: absolute;
          top: -85px;
          background: var(--accent-color);
          color: #0b0f19;
          padding: 12px 22px;
          border-radius: 20px;
          font-weight: 700;
          font-size: 13px;
          opacity: 0;
          transform: translateY(12px) scale(0.85);
          transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
          pointer-events: none;
          box-shadow: 0 15px 30px var(--glow-color);
          white-space: nowrap;
          z-index: 10;
        }

        .speech-bubble::after {
          content: '';
          position: absolute;
          bottom: -7px;
          left: 50%;
          transform: translateX(-50%);
          border-width: 7px 7px 0;
          border-style: solid;
          border-color: var(--accent-color) transparent;
          display: block;
          width: 0;
        }

        .speech-bubble.active {
          opacity: 1;
          transform: translateY(0) scale(1);
        }

        .sound-waves {
          position: absolute;
          top: -30px;
          display: flex;
          gap: 4px;
          align-items: flex-end;
          height: 20px;
          opacity: 0;
          transition: opacity 0.3s ease;
        }

        .sound-bar {
          width: 3px;
          height: 100%;
          background: var(--accent-color);
          border-radius: 3px;
        }

        .talking .sound-waves { opacity: 1; }
        .talking .sound-bar { animation: waveAnim 0.4s infinite alternate ease-in-out; }
        .talking .sound-bar:nth-child(1) { animation-delay: 0.1s; }
        .talking .sound-bar:nth-child(2) { animation-delay: 0.2s; }
        .talking .sound-bar:nth-child(3) { animation-delay: 0.3s; }
        .talking .sound-bar:nth-child(4) { animation-delay: 0.15s; }

        @keyframes waveAnim {
          0% { height: 4px; }
          100% { height: 20px; }
        }

        .mascot {
          position: relative;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 18px;
          padding: 42px 34px 30px 34px;
          background: var(--card-bg);
          border-radius: 75px;
          box-shadow: 0 35px 70px rgba(0, 0, 0, 0.8), 
                      inset 0 2px 4px rgba(255, 255, 255, 0.12),
                      0 0 60px var(--glow-color);
          animation: idleBreathing 4s ease-in-out infinite alternate;
          transition: transform 0.3s ease-out, background-color 0.5s, box-shadow 0.5s;
          transform-style: preserve-3d;
          cursor: pointer;
        }

        @keyframes idleBreathing {
          0% { transform: translateY(0) scale(1) rotate(0deg); }
          100% { transform: translateY(-10px) scale(1.02) rotate(0.5deg); }
        }

        .blush-container {
          position: absolute;
          top: 100px;
          width: 100%;
          display: flex;
          justify-content: space-between;
          padding: 0 25px;
          pointer-events: none;
        }

        .blush {
          width: 24px;
          height: 12px;
          background: var(--blush-color);
          border-radius: 50%;
          opacity: 0;
          filter: blur(5px);
          transition: opacity 0.5s ease;
        }

        .eyes-wrapper { display: flex; gap: 32px; }

        .eye {
          width: 98px;
          height: 122px;
          background-color: var(--eye-bg);
          border-radius: 50%;
          position: relative;
          display: flex;
          justify-content: center;
          align-items: center;
          overflow: hidden;
          box-shadow: inset 0 10px 22px rgba(0, 0, 0, 0.25);
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .eyelid-top, .eyelid-bottom {
          position: absolute;
          width: 100%;
          background-color: var(--card-bg);
          z-index: 3;
          transition: all 0.25s ease;
        }

        .eyelid-top { top: 0; height: 0%; border-bottom: 2px solid rgba(255,255,255,0.05); }
        .eyelid-bottom { bottom: 0; height: 0%; border-top: 2px solid rgba(255,255,255,0.05); }

        .iris {
          width: 52px;
          height: 52px;
          border-radius: 50%;
          background: radial-gradient(circle, var(--accent-color) 0%, rgba(15,23,42,0.8) 100%);
          display: flex;
          justify-content: center;
          align-items: center;
          position: absolute;
          transition: transform 0.04s ease-out;
          z-index: 2;
        }

        .pupil {
          width: 28px;
          height: 28px;
          background-color: var(--pupil-color);
          border-radius: 50%;
          position: relative;
          transition: width 0.3s, height 0.3s, background-color 0.4s;
          display: flex;
          justify-content: flex-end;
          align-items: flex-start;
          padding: 3px;
        }

        .pupil::after {
          content: '';
          width: 9px;
          height: 9px;
          background-color: #ffffff;
          border-radius: 50%;
          box-shadow: -3px 3px 0 1px rgba(255,255,255,0.4);
        }

        .mouth {
          width: 30px;
          height: 6px;
          background-color: var(--accent-color);
          border-radius: 10px;
          transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
          box-shadow: 0 0 12px var(--accent-color);
          z-index: 2;
        }

        .talking .mouth { animation: organicTalk 0.22s infinite alternate; }

        @keyframes organicTalk {
          0% { width: 22px; height: 6px; border-radius: 50%; }
          50% { width: 36px; height: 22px; border-radius: 30% 30% 60% 60%; }
          100% { width: 16px; height: 16px; border-radius: 50%; }
        }

        .eye.blink .eyelid-top, .eye.blink .eyelid-bottom { height: 50%; }
        .mood-happy .eyelid-bottom { height: 42%; border-radius: 50% 50% 0 0; }
        .mood-happy .mouth { width: 40px; height: 18px; border-radius: 0 0 20px 20px; }
        .mood-happy .blush { opacity: 0.6; }

        .mood-sad .eyelid-top { height: 38%; transform: rotate(-8deg); }
        .mood-sad .mouth { width: 24px; height: 12px; border-radius: 15px 15px 0 0; transform: translateY(4px); }

        .mood-angry .eyelid-top { height: 48%; }
        .mood-angry .eye:first-child .eyelid-top { transform: rotate(16deg); }
        .mood-angry .eye:last-child .eyelid-top { transform: rotate(-16deg); }
        .mood-angry .mouth { width: 30px; height: 4px; transform: translateY(-2px); }

        .mood-surprised .pupil { width: 14px !important; height: 14px !important; }
        .mood-surprised .mouth { width: 20px; height: 24px; border-radius: 50%; }

        .mood-shy .blush { opacity: 0.95; }
        .mood-shy .eyelid-top { height: 25%; }
        .mood-shy .eyelid-bottom { height: 25%; }
        .mood-shy .pupil { background-color: #f43f5e; }

        .controls-container {
          width: 100%;
          max-width: 440px;
          display: flex;
          flex-direction: column;
          gap: 12px;
          z-index: 10;
          transition: opacity 0.4s ease, transform 0.4s ease;
        }

        .panel-section {
          display: none;
          flex-direction: column;
          gap: 10px;
          animation: fadeIn 0.3s ease;
        }

        .panel-section.active { display: flex; }

        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }

        .action-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 10px;
        }

        .action-btn {
          background: rgba(15, 23, 42, 0.75);
          color: #e2e8f0;
          border: 1px solid rgba(255, 255, 255, 0.08);
          padding: 12px;
          border-radius: 16px;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          backdrop-filter: blur(12px);
          transition: all 0.2s;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
        }

        .action-btn:active { transform: scale(0.95); background: #334155; }

        .mood-selector {
          display: grid;
          grid-template-columns: repeat(6, 1fr);
          gap: 6px;
          background: rgba(15, 23, 42, 0.75);
          padding: 8px;
          border-radius: 20px;
          border: 1px solid rgba(255, 255, 255, 0.08);
          backdrop-filter: blur(12px);
        }

        .mood-btn {
          background: #1e293b;
          color: #fff;
          border: 1px solid rgba(255, 255, 255, 0.05);
          padding: 10px 0;
          border-radius: 14px;
          font-size: 18px;
          cursor: pointer;
          display: flex;
          justify-content: center;
          align-items: center;
          transition: all 0.2s;
        }

        .mood-btn:active, .mood-btn.active {
          background: var(--accent-color);
          transform: scale(0.92);
          box-shadow: 0 0 15px var(--accent-color);
        }

        body.floating-mode .top-panel,
        body.floating-mode .controls-container {
          opacity: 0;
          pointer-events: none;
          transform: translateY(20px);
        }

        body.floating-mode .mascot-wrapper {
          position: absolute;
          margin: 0;
          transform: scale(0.48);
          cursor: grab;
          transition: left 1.2s cubic-bezier(0.25, 1, 0.5, 1), top 1.2s cubic-bezier(0.25, 1, 0.5, 1), transform 0.5s ease;
        }

        body.floating-mode .mascot-wrapper:active {
          cursor: grabbing;
        }

        .expand-btn {
          position: absolute;
          bottom: -35px;
          background: rgba(15, 23, 42, 0.9);
          color: var(--accent-color);
          border: 1px solid var(--accent-color);
          padding: 6px 14px;
          border-radius: 20px;
          font-size: 11px;
          font-weight: 700;
          cursor: pointer;
          opacity: 0;
          pointer-events: none;
          transition: all 0.3s ease;
          white-space: nowrap;
          box-shadow: 0 0 10px var(--glow-color);
        }

        body.floating-mode .expand-btn {
          opacity: 1;
          pointer-events: auto;
        }
      </style>
    </head>
    <body>

      <canvas id="particleCanvas"></canvas>

      <div class="top-panel" id="topPanel">
        <div class="mode-switch-container">
          <button class="mode-btn active" id="assistantTabBtn" onclick="switchMainMode('assistant')">🤖 Asistan</button>
          <button class="mode-btn" id="petTabBtn" onclick="switchMainMode('pet')">🐾 Sanal Pet</button>
          <button class="mode-btn" id="floatTabBtn" onclick="enableFloatingMode()">🖥️ Gezinti</button>
        </div>
        <div class="status-bar" id="statusBar">🤖 AI Asistan Modu: Göreve Hazır</div>
      </div>

      <div class="mascot-wrapper" id="mascotWrapper">
        <div class="speech-bubble" id="speechBubble">Nasıl yardımcı olabilirim? 💡</div>

        <div class="sound-waves">
          <div class="sound-bar"></div>
          <div class="sound-bar"></div>
          <div class="sound-bar"></div>
          <div class="sound-bar"></div>
        </div>

        <div class="mascot" id="mascot" onclick="handleMascotClick()">
          <div class="blush-container">
            <div class="blush"></div>
            <div class="blush"></div>
          </div>

          <div class="eyes-wrapper">
            <div class="eye">
              <div class="eyelid-top"></div>
              <div class="iris"><div class="pupil"></div></div>
              <div class="eyelid-bottom"></div>
            </div>
            <div class="eye">
              <div class="eyelid-top"></div>
              <div class="iris"><div class="pupil"></div></div>
              <div class="eyelid-bottom"></div>
            </div>
          </div>
          <div class="mouth" id="mouth"></div>
        </div>

        <button class="expand-btn" onclick="disableFloatingMode(event)">🔍 Tam Ekrana Dön</button>
      </div>

      <div class="controls-container" id="controlsContainer">
        
        <div class="panel-section active" id="assistantPanel">
          <div class="action-grid">
            <button class="action-btn" onclick="asistantTask('pomodoro')">⏱️ Pomodoro Başlat</button>
            <button class="action-btn" onclick="asistantTask('summary')">📝 Özet Çıkar</button>
            <button class="action-btn" onclick="asistantTask('reminder')">💧 Su Hatırlatıcı</button>
            <button class="action-btn" onclick="asistantTask('weather')">🌦️ Hava Durumu</button>
          </div>
        </div>

        <div class="panel-section" id="petPanel">
          <div class="mood-selector">
            <button class="mood-btn active" onclick="setMood('neutral')">😐</button>
            <button class="mood-btn" onclick="setMood('happy')">😊</button>
            <button class="mood-btn" onclick="setMood('sad')">🥺</button>
            <button class="mood-btn" onclick="setMood('angry')">😡</button>
            <button class="mood-btn" onclick="setMood('surprised')">😲</button>
            <button class="mood-btn" onclick="setMood('shy')">😳</button>
          </div>
          <div class="action-grid">
            <button class="action-btn" onclick="triggerPraise()">✨ Öv / Sev</button>
            <button class="action-btn" onclick="triggerHypnosis()">🌀 Hipnoz Et</button>
          </div>
        </div>

      </div>

      <script>
        const irises = document.querySelectorAll('.iris');
        const pupils = document.querySelectorAll('.pupil');
        const eyes = document.querySelectorAll('.eye');
        const mascot = document.getElementById('mascot');
        const mascotWrapper = document.getElementById('mascotWrapper');
        const statusBar = document.getElementById('statusBar');
        const speechBubble = document.getElementById('speechBubble');

        let currentMainMode = 'assistant';
        let currentMood = 'neutral';
        let talkTimer = null;
        let floatInterval = null;
        let isFloating = false;

        const moodData = {
          neutral: { theme: '#38bdf8', bg: '#030712', status: '💙 Empati Modu: Sakin ve Odaklı' },
          happy: { theme: '#22c55e', bg: '#021208', status: '✨ Mutlu: İçsel Neşe ve Enerji' },
          sad: { theme: '#60a5fa', bg: '#030a16', status: '🌧️ Hüzünlü: Melankolik ve Duygusal' },
          angry: { theme: '#f43f5e', bg: '#140306', status: '🔥 Tepkili: Çatık ve Huzursuz' },
          surprised: { theme: '#eab308', bg: '#120e02', status: '⚡ Şaşırmış: Meraklı ve Hayret İçinde' },
          shy: { theme: '#ec4899', bg: '#12030a', status: '🌸 Utangaç: Mahcup ve Aşık' }
        };

        function switchMainMode(mode) {
          if (isFloating) disableFloatingMode();

          currentMainMode = mode;
          
          const assistantBtn = document.getElementById('assistantTabBtn');
          const petBtn = document.getElementById('petTabBtn');
          const floatBtn = document.getElementById('floatTabBtn');
          const assistantPanel = document.getElementById('assistantPanel');
          const petPanel = document.getElementById('petPanel');

          floatBtn.classList.remove('active');

          if (mode === 'assistant') {
            assistantBtn.classList.add('active');
            petBtn.classList.remove('active');
            assistantPanel.classList.add('active');
            petPanel.classList.remove('active');
            
            setMood('neutral');
            statusBar.innerText = "🤖 AI Asistan Modu: Göreve Hazır";
            startTalking("Asistan moduna geçtik. Hangi görevi yapalım?");
          } else if (mode === 'pet') {
            petBtn.classList.add('active');
            assistantBtn.classList.remove('active');
            petPanel.classList.add('active');
            assistantPanel.classList.remove('active');

            setMood('happy');
            statusBar.innerText = "🐾 Sanal Pet Modu: Oyun Zamanı!";
            startTalking("Oley! Biraz oynayalım mı? ✨");
          }
        }

        function enableFloatingMode() {
          isFloating = true;
          document.body.classList.add('floating-mode');

          document.getElementById('assistantTabBtn').classList.remove('active');
          document.getElementById('petTabBtn').classList.remove('active');
          document.getElementById('floatTabBtn').classList.add('active');

          moveMascotTo(window.innerWidth / 2 - 80, window.innerHeight / 2 - 80);
          setMood('happy');
          startTalking("Ekranda gezinmeye başladım! 🚀");

          clearInterval(floatInterval);
          floatInterval = setInterval(roamRandomly, 4000);
        }

        function disableFloatingMode(e) {
          if (e) e.stopPropagation();
          isFloating = false;
          clearInterval(floatInterval);

          document.body.classList.remove('floating-mode');
          mascotWrapper.style.left = '';
          mascotWrapper.style.top = '';

          switchMainMode(currentMainMode === 'floating' ? 'assistant' : currentMainMode);
        }

        function roamRandomly() {
          if (!isFloating) return;

          const padding = 120;
          const maxX = window.innerWidth - padding * 2;
          const maxY = window.innerHeight - padding * 2;

          const targetX = Math.max(padding, Math.floor(Math.random() * maxX));
          const targetY = Math.max(padding, Math.floor(Math.random() * maxY));

          moveMascotTo(targetX, targetY);

          const floatMessages = [
            "Etrafı turluyorum... 🔍",
            "Seni izliyorum! ✨",
            "Burada ne var acaba? 🤔",
            "Harika gidiyorsun! 💪",
            "Mola vermek istersen buradayım 🐾"
          ];

          if (Math.random() > 0.4) {
            startTalking(floatMessages[Math.floor(Math.random() * floatMessages.length)], 2200);
          }
        }

        function moveMascotTo(x, y) {
          handleMove(x + 100, y + 100);

          mascotWrapper.style.left = x + 'px';
          mascotWrapper.style.top = y + 'px';
        }

        window.addEventListener('click', (e) => {
          if (!isFloating) return;
          if (e.target.closest('.mascot-wrapper') || e.target.closest('.expand-btn')) return;

          moveMascotTo(e.clientX - 80, e.clientY - 80);
          setMood('surprised');
          startTalking("Oraya geliyorum! ⚡", 1800);
        });

        function asistantTask(task) {
          if (task === 'pomodoro') {
            setMood('neutral');
            startTalking("25 dakikalık odaklanma süresi başladı! ⏱️");
          } else if (task === 'summary') {
            setMood('surprised');
            startTalking("Metni gönder, hemen özetleyeyim! 📝");
          } else if (task === 'reminder') {
            setMood('happy');
            startTalking("Bir bardak su içme vakti! 💧");
          } else if (task === 'weather') {
            setMood('happy');
            startTalking("Bugün hava harika görünüyor! ☀️");
          }
        }

        function setMood(moodKey) {
          currentMood = moodKey;
          const data = moodData[moodKey];

          mascot.className = 'mascot mood-' + moodKey;
          document.documentElement.style.setProperty('--accent-color', data.theme);
          document.documentElement.style.setProperty('--bg-color', data.bg);
          document.documentElement.style.setProperty('--glow-color', data.theme + '25');

          if (currentMainMode === 'pet' && !isFloating) {
            statusBar.innerText = data.status;
          }

          document.querySelectorAll('.mood-btn').forEach(btn => btn.classList.remove('active'));
          if (window.event && window.event.currentTarget) {
            window.event.currentTarget.classList.add('active');
          }
        }

        function startTalking(text, duration = 2800) {
          mascot.classList.add('talking');
          speechBubble.innerText = text;
          speechBubble.classList.add('active');

          clearTimeout(talkTimer);
          talkTimer = setTimeout(() => {
            mascot.classList.remove('talking');
            speechBubble.classList.remove('active');
          }, duration);
        }

        function handleMascotClick() {
          if (isFloating) {
            setMood('happy');
            startTalking("Beni yakaladın! 😄");
            return;
          }

          if (currentMainMode === 'assistant') {
            startTalking("Seni dinliyorum, komut verebilirsin. 💡");
          } else {
            const petReplies = [
              "Gıdıklanıyorum! 😄",
              "Beni sevmene bayılıyorum! ❤️",
              "Mırrr... ✨",
              "Bugün çok tatlısın!"
            ];
            setMood('happy');
            const reply = petReplies[Math.floor(Math.random() * petReplies.length)];
            startTalking(reply);
          }
        }

        setInterval(() => {
          if (!isFloating && currentMainMode === 'assistant' && Math.random() > 0.65) {
            setMood('shy');
            startTalking("Biraz sıkıldım, ekranda gezineyim mi? 🚀", 3500);
            setTimeout(() => {
              if (!isFloating && currentMainMode === 'assistant') enableFloatingMode();
            }, 3600);
          }
        }, 30000);

        function handleMove(clientX, clientY) {
          irises.forEach((iris) => {
            const eye = iris.parentElement;
            const rect = eye.getBoundingClientRect();
            const eyeX = rect.left + rect.width / 2;
            const eyeY = rect.top + rect.height / 2;

            const angle = Math.atan2(clientY - eyeY, clientX - eyeX);
            const distance = Math.min(Math.hypot(clientX - eyeX, clientY - eyeY) / 7, 24);

            const x = Math.cos(angle) * distance;
            const y = Math.sin(angle) * distance;

            iris.style.transform = 'translate(' + x + 'px, ' + y + 'px)';
          });

          const distFromCenter = Math.hypot(clientX - window.innerWidth / 2, clientY - window.innerHeight / 2);
          const pupilSize = Math.max(16, Math.min(34, 34 - (distFromCenter / 20)));

          if (currentMood !== 'surprised') {
            pupils.forEach(pupil => {
              pupil.style.width = pupilSize + 'px';
              pupil.style.height = pupilSize + 'px';
            });
          }

          if (!isFloating) {
            const rect = mascot.getBoundingClientRect();
            const centerX = rect.left + rect.width / 2;
            const centerY = rect.top + rect.height / 2;

            const tiltX = (clientY - centerY) / 25;
            const tiltY = (centerX - clientX) / 25;

            mascot.style.transform = 'rotateX(' + tiltX + 'deg) rotateY(' + tiltY + 'deg)';
          }
        }

        window.addEventListener('mousemove', (e) => handleMove(e.clientX, e.clientY));
        window.addEventListener('touchmove', (e) => handleMove(e.touches[0].clientX, e.touches[0].clientY));

        function triggerHypnosis() {
          startTalking("Gözlerimin içine bak... 🌀", 3000);
          let angle = 0;
          const hypnoInterval = setInterval(() => {
            angle += 0.2;
            const x = Math.cos(angle) * 18;
            const y = Math.sin(angle) * 18;
            irises.forEach(iris => iris.style.transform = 'translate(' + x + 'px, ' + y + 'px)');
          }, 20);

          setTimeout(() => clearInterval(hypnoInterval), 3000);
        }

        function triggerPraise() {
          setMood('shy');
          startTalking("İltifatların beni çok mutlu ediyor! ✨❤️");
        }

        function autoBlink() {
          eyes.forEach(eye => eye.classList.add('blink'));
          setTimeout(() => eyes.forEach(eye => eye.classList.remove('blink')), 140);
          setTimeout(autoBlink, Math.random() * 4000 + 2000);
        }
        autoBlink();

        const canvas = document.getElementById('particleCanvas');
        const ctx = canvas.getContext('2d');
        let particles = [];

        function resizeCanvas() {
          canvas.width = window.innerWidth;
          canvas.height = window.innerHeight;
        }
        window.addEventListener('resize', resizeCanvas);
        resizeCanvas();

        class Particle {
          constructor() { this.reset(); }
          reset() {
            this.x = Math.random() * canvas.width;
            this.y = Math.random() * canvas.height;
            this.size = Math.random() * 2 + 0.5;
            this.speedY = Math.random() * -0.5 - 0.2;
            this.alpha = Math.random() * 0.5 + 0.1;
          }
          update() {
            this.y += this.speedY;
            if (this.y < 0) this.reset();
          }
          draw() {
            ctx.fillStyle = 'rgba(255, 255, 255, ' + this.alpha + ')';
            ctx.beginPath();
            ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
            ctx.fill();
          }
        }

        for (let i = 0; i < 35; i++) particles.push(new Particle());

        function animateParticles() {
          ctx.clearRect(0, 0, canvas.width, canvas.height);
          particles.forEach(p => { p.update(); p.draw(); });
          requestAnimationFrame(animateParticles);
        }
        animateParticles();
      </script>
    </body>
    </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, htmlIcerik, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier.fillMaxSize()
    )
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
