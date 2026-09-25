package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent

private val WHATSAPP_DESENI = Regex(
    "whatsap\\S*\\s+(\\S+)['’]?(e|a|ye|ya)\\s+(.+?)\\s+yaz",
    RegexOption.IGNORE_CASE
)

fun whatsAppKomutuMu(mesaj: String): Boolean {
    return WHATSAPP_DESENI.containsMatchIn(mesaj)
}

fun whatsAppKomutunuCalistir(context: Context, mesaj: String): String? {
    val eslesme = WHATSAPP_DESENI.find(mesaj) ?: return null
    val kisiAdi = eslesme.groupValues[1].trim().replaceFirstChar { it.uppercase() }
    val mesajIcerigi = eslesme.groupValues[3].trim()

    OtomasyonKuyrugu.bekleyenWhatsAppGorevi = WhatsAppGorevi(kisiAdi, mesajIcerigi)
    OtomasyonKuyrugu.durum = "WhatsApp açılıyor..."

    val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "$kisiAdi kişisine WhatsApp'tan \"$mesajIcerigi\" mesajı gönderiliyor..."
    }
    return "WhatsApp telefonda kurulu değil"
}
