package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

private val WHATSAPP_DESENI = Regex(
    "whatsap\\S*\\s+(\\S+?)\\s+(.+?)\\s+yaz",
    RegexOption.IGNORE_CASE
)

fun whatsAppKomutuMu(mesaj: String): Boolean {
    return WHATSAPP_DESENI.containsMatchIn(mesaj)
}

fun whatsAppKomutunuCalistir(context: Context, mesaj: String): String? {
    val eslesme = WHATSAPP_DESENI.find(mesaj) ?: return null
    val kisiAdiHam = eslesme.groupValues[1].trim()
    val kisiAdi = kisiAdiHam.trimEnd('e', 'a', 'ı', 'i', 'y', '\'').replaceFirstChar { it.uppercase() }
    val mesajIcerigi = eslesme.groupValues[2].trim()

    val numara = KisiRehberi.telefonNumarasiBul(context, kisiAdi)

    if (numara != null) {
        OtomasyonKuyrugu.bekleyenWhatsAppGorevi = WhatsAppGorevi(kisiAdi, mesajIcerigi, dogrudanAcildiMi = true)
        OtomasyonKuyrugu.durum = "$kisiAdi ile sohbet açılıyor..."
        val kodlanmisMesaj = URLEncoder.encode(mesajIcerigi, "UTF-8").replace("+", "%20")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$numara?text=$kodlanmisMesaj")
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "$kisiAdi kişisine WhatsApp'tan \"$mesajIcerigi\" mesajı gönderiliyor..."
        } catch (e: Exception) {
            OtomasyonKuyrugu.bekleyenWhatsAppGorevi = null
            "WhatsApp açılamadı: ${e.message}"
        }
    }

    OtomasyonKuyrugu.bekleyenWhatsAppGorevi = WhatsAppGorevi(kisiAdi, mesajIcerigi, dogrudanAcildiMi = false)
    OtomasyonKuyrugu.durum = "$kisiAdi rehberde bulunamadı, sohbetlerde aranacak..."

    val whatsappIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
    if (whatsappIntent != null) {
        whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(whatsappIntent)
        return "$kisiAdi kişisine WhatsApp'tan \"$mesajIcerigi\" mesajı gönderiliyor (rehberde yok, sohbetlerde aranıyor)..."
    }
    return "WhatsApp telefonda kurulu değil"
}
