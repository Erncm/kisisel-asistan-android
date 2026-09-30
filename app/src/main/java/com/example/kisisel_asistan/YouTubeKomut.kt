package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

private val YOUTUBE_DESENI = Regex(
    "(youtube|yt)('?d[ae]n?|'?y[ae])?\\s+(.+)",
    RegexOption.IGNORE_CASE
)

private val ARAMA_KELIME_DESENI = Regex(
    "^(.+?)\\s*(kanalının|kanalinin|kanalın|kanalin)?\\s*(bir|son|en son|dünkü|güncel)?\\s*(video[a-zçğıöşü]*|videosunu|videosu)\\s*(aç|ac|oynat|başlat)",
    RegexOption.IGNORE_CASE
)

fun youtubeKomutuMu(mesaj: String): Boolean {
    return YOUTUBE_DESENI.containsMatchIn(mesaj.trim())
}

fun youtubeKomutunuCalistir(context: Context, mesaj: String): String? {
    val eslesme = YOUTUBE_DESENI.find(mesaj.trim()) ?: return null
    val talimat = eslesme.groupValues[3].trim()
    if (talimat.isBlank()) return null

    val aramaEslesme = ARAMA_KELIME_DESENI.find(talimat)
    val aramaTerimi = aramaEslesme?.groupValues?.get(1)?.trim() ?: talimat

    return try {
        val kodlanmis = URLEncoder.encode(aramaTerimi, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.youtube.com/results?search_query=$kodlanmis")
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        OtomasyonBeyni.devamGoreviBaslat("com.google.android.youtube", talimat)
        "YouTube'da \"$aramaTerimi\" aranıyor, sonra: \"$talimat\""
    } catch (e: Exception) {
        val genelIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        if (genelIntent != null) {
            genelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(genelIntent)
            OtomasyonBeyni.gorevBaslat(context, "com.google.android.youtube", talimat)
            "YouTube açılıyor, hedef: \"$talimat\""
        } else {
            "YouTube telefonda kurulu değil"
        }
    }
}
