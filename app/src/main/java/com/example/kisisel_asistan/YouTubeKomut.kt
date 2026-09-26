package com.example.kisisel_asistan

import android.content.Context

private val YOUTUBE_DESENI = Regex(
    "(youtube|yt)('?d[ae]n?|'?y[ae])?\\s+(.+)",
    RegexOption.IGNORE_CASE
)

fun youtubeKomutuMu(mesaj: String): Boolean {
    return YOUTUBE_DESENI.containsMatchIn(mesaj.trim())
}

fun youtubeKomutunuCalistir(context: Context, mesaj: String): String? {
    val eslesme = YOUTUBE_DESENI.find(mesaj.trim()) ?: return null
    val talimat = eslesme.groupValues[3].trim()
    if (talimat.isBlank()) return null

    OtomasyonBeyni.gorevBaslat(context, "com.google.android.youtube", talimat)
    return "YouTube açılıyor, hedef: \"$talimat\""
}
