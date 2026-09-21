package com.example.kisisel_asistan

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class HavaDurumuVerisi(
    val sicaklik: Double,
    val aciklama: String,
    val sehir: String,
    val zamanDamgasi: Long = System.currentTimeMillis()
)

fun havaKoduAciklama(kod: Int): String = when (kod) {
    0 -> "Açık"
    1, 2 -> "Parçalı Bulutlu"
    3 -> "Kapalı"
    45, 48 -> "Sisli"
    51, 53, 55 -> "Çiseleme"
    61, 63, 65 -> "Yağmurlu"
    71, 73, 75 -> "Karlı"
    80, 81, 82 -> "Sağanak Yağış"
    95 -> "Gök Gürültülü"
    else -> "Bilinmiyor"
}

suspend fun havaDurumuGetir(context: Context, lat: Double, lon: Double): HavaDurumuVerisi? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
            val baglanti = url.openConnection() as HttpURLConnection
            baglanti.requestMethod = "GET"
            baglanti.connectTimeout = 8000
            baglanti.readTimeout = 8000

            val yanit = baglanti.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(yanit)
            val guncelHava = json.getJSONObject("current_weather")
            val sicaklik = guncelHava.getDouble("temperature")
            val kod = guncelHava.getInt("weathercode")

            HavaDurumuVerisi(
                sicaklik = sicaklik,
                aciklama = havaKoduAciklama(kod),
                sehir = sehirAdiGetir(context, lat, lon)
            )
        } catch (e: Exception) {
            null
        }
    }
}

fun sehirAdiGetir(context: Context, lat: Double, lon: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale("tr", "TR"))
        @Suppress("DEPRECATION")
        val sonuclar = geocoder.getFromLocation(lat, lon, 1)
        val adres = sonuclar?.firstOrNull()
        adres?.subAdminArea ?: adres?.locality ?: adres?.adminArea ?: "Konumunuz"
    } catch (e: Exception) {
        "Konumunuz"
    }
}

fun havaDurumuOnbellekKaydet(context: Context, veri: HavaDurumuVerisi) {
    val prefs = context.getSharedPreferences("asistan_onbellek", Context.MODE_PRIVATE)
    prefs.edit()
        .putFloat("sicaklik", veri.sicaklik.toFloat())
        .putString("aciklama", veri.aciklama)
        .putString("sehir", veri.sehir)
        .putLong("zaman", veri.zamanDamgasi)
        .apply()
}

fun havaDurumuOnbellekOku(context: Context): HavaDurumuVerisi? {
    val prefs = context.getSharedPreferences("asistan_onbellek", Context.MODE_PRIVATE)
    if (!prefs.contains("sicaklik")) return null
    return HavaDurumuVerisi(
        sicaklik = prefs.getFloat("sicaklik", 0f).toDouble(),
        aciklama = prefs.getString("aciklama", "") ?: "",
        sehir = prefs.getString("sehir", "") ?: "",
        zamanDamgasi = prefs.getLong("zaman", 0L)
    )
}
