package com.example.kisisel_asistan

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMesaj(
    val icerik: String,
    val benMi: Boolean,
    val baglamaDahilMi: Boolean = true,
    val gorselBase64: String? = null,
    val gorselMimeTipi: String? = null
)

fun gorseliBase64eCevir(context: Context, uri: Uri): Pair<String, String>? {
    return try {
        val mimeTipi = context.contentResolver.getType(uri) ?: "image/jpeg"
        val giris = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = giris.use { it.readBytes() }
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        Pair(base64, mimeTipi)
    } catch (e: Exception) {
        null
    }
}

suspend fun geminiYanitAl(apiKey: String, gecmis: List<ChatMesaj>): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey")
            val baglanti = url.openConnection() as HttpURLConnection
            baglanti.requestMethod = "POST"
            baglanti.setRequestProperty("Content-Type", "application/json")
            baglanti.doOutput = true
            baglanti.connectTimeout = 20000
            baglanti.readTimeout = 20000

            val contents = JSONArray()
            for (mesaj in gecmis) {
                val obje = JSONObject()
                obje.put("role", if (mesaj.benMi) "user" else "model")
                val parcalar = JSONArray()
                if (mesaj.icerik.isNotBlank()) {
                    parcalar.put(JSONObject().put("text", mesaj.icerik))
                }
                if (mesaj.gorselBase64 != null && mesaj.gorselMimeTipi != null) {
                    val inlineData = JSONObject()
                    inlineData.put("mime_type", mesaj.gorselMimeTipi)
                    inlineData.put("data", mesaj.gorselBase64)
                    parcalar.put(JSONObject().put("inline_data", inlineData))
                }
                obje.put("parts", parcalar)
                contents.put(obje)
            }

            val govde = JSONObject()
            govde.put("contents", contents)

            baglanti.outputStream.use { it.write(govde.toString().toByteArray()) }

            val kod = baglanti.responseCode
            if (kod in 200..299) {
                val yanit = baglanti.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(yanit)
                val metin = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                Result.success(metin)
            } else {
                val hataMetni = baglanti.errorStream?.bufferedReader()?.use { it.readText() } ?: "Bilinmeyen hata"
                Result.failure(Exception("HTTP $kod: $hataMetni"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

fun apiAnahtariKaydet(context: Context, anahtar: String) {
    val prefs = context.getSharedPreferences("asistan_ayarlar", Context.MODE_PRIVATE)
    prefs.edit().putString("gemini_api_key", anahtar).apply()
}

fun apiAnahtariOku(context: Context): String {
    val prefs = context.getSharedPreferences("asistan_ayarlar", Context.MODE_PRIVATE)
    return prefs.getString("gemini_api_key", "") ?: ""
}
