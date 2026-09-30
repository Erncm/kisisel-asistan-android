package com.example.kisisel_asistan

import android.content.Context

suspend fun mesajiIsleVeCevapAl(context: Context, girdiHam: String): String {
    val girdiTrim = girdiHam.trim()
    if (girdiTrim.isBlank()) return ""

    if (whatsAppKomutuMu(girdiTrim)) {
        SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
        val sonucMesaji = whatsAppKomutunuCalistir(context, girdiTrim) ?: "Komut anlaşılamadı"
        SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji, benMi = false, baglamaDahilMi = false))
        return sonucMesaji
    }

    if (youtubeKomutuMu(girdiTrim)) {
        SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
        val sonucMesaji = youtubeKomutunuCalistir(context, girdiTrim) ?: "Komut anlaşılamadı"
        SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji, benMi = false, baglamaDahilMi = false))
        return sonucMesaji
    }

    val eslesenUygulama = genelKomutMu(girdiTrim)
    if (eslesenUygulama != null) {
        SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
        val sonucMesaji = genelKomutuCalistir(context, girdiTrim, eslesenUygulama)
        SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji, benMi = false, baglamaDahilMi = false))
        return sonucMesaji
    }

    if (hafizaKomutuMu(girdiTrim)) {
        val oncekiMesaj = SohbetDurumu.aktifMesajlar.lastOrNull()?.icerik
        val icerik = hafizaIcerigiCikar(girdiTrim, oncekiMesaj)
        SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true, baglamaDahilMi = false))
        val kayit = HafizaDeposu.ekle(icerik)
        val sonucMesaji = "Not edildi ✓ [${kayit.kod}]: ${kayit.ozet}"
        SohbetDurumu.mesajEkle(ChatMesaj(sonucMesaji, benMi = false, baglamaDahilMi = false))
        return sonucMesaji
    }

    SohbetDurumu.mesajEkle(ChatMesaj(girdiTrim, benMi = true))
    val anahtar = apiAnahtariOku(context)
    var gemeniDenendi = false
    val hafizaOzeti = HafizaDeposu.hepsiniOzetGetir()
    val baglamGecmisi = SohbetDurumu.aktifMesajlar.filter { it.baglamaDahilMi }

    if (anahtar.isNotBlank()) {
        gemeniDenendi = true
        val gonderilecekListe = if (hafizaOzeti.isNotBlank()) {
            listOf(
                ChatMesaj("Kullanıcı hakkında bildiğim notlar:\n$hafizaOzeti", benMi = true),
                ChatMesaj("Anladım, bu bilgileri göz önünde bulunduracağım.", benMi = false)
            ) + baglamGecmisi
        } else baglamGecmisi
        val sonuc = geminiYanitAl(anahtar, gonderilecekListe)
        if (sonuc.isSuccess) {
            val cevap = sonuc.getOrDefault("")
            SohbetDurumu.mesajEkle(ChatMesaj(cevap, benMi = false))
            return cevap
        }
    }

    if (modelVarMi(context)) {
        val hazir = YerelModel.hazirla(context)
        if (hazir) {
            val girdiMetniSon = if (hafizaOzeti.isNotBlank()) {
                "[Hafıza notların]\n$hafizaOzeti\n\nKullanıcı: $girdiTrim"
            } else girdiTrim
            val yanit = YerelModel.yanitAl(girdiMetniSon)
            SohbetDurumu.mesajEkle(ChatMesaj(yanit, benMi = false))
            return yanit
        }
    }

    val hataMesaji = if (gemeniDenendi) "İnternet yok ve yerel model kurulu değil" else "Önce Ayarlar'dan Gemini anahtarı gir ya da yerel modeli indir"
    SohbetDurumu.mesajEkle(ChatMesaj(hataMesaji, benMi = false))
    return hataMesaji
}
