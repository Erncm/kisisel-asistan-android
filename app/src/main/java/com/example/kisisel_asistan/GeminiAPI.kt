package com.example.kisisel_asistan

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMesaj(val icerik: String, val benMi: Boolean)

suspend fun geminiYanitAl(apiKey: String, gecmis: List<ChatMesaj>): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey")
            val baglanti = url.openConnection() as HttpURLConnection
            baglanti.requestMethod = "POST"
            baglanti.setRequestProperty("Content-Type", "application/json")
            baglanti.doOutput = true
            baglanti.connectTimeout = 15000
            baglanti.readTimeout = 15000

            val contents = JSONArray()
            for (mesaj in gecmis) {
                val obje = JSONObject()
                obje.put("role", if (mesaj.benMi) "user" else "model")
                val parcalar = JSONArray()
                parcalar.put(JSONObject().put("text", mesaj.icerik))
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
