package com.example.kisisel_asistan

private val TETIKLEYICI_KELIMELER = listOf(
    "aç", "gir", "yap", "et", "yaz", "ara", "bul", "başlat", "oynat", "gönder", "ekle"
)

fun genelKomutMu(mesaj: String): YuklenmisUygulama? {
    val kucukMesaj = mesaj.lowercase()
    val tetikleyiciVar = TETIKLEYICI_KELIMELER.any { kucukMesaj.contains(it) }
    if (!tetikleyiciVar) return null
    return UygulamaKatalogu.eslesenUygulamaBul(mesaj)
}

fun genelKomutuCalistir(context: android.content.Context, mesaj: String, uygulama: YuklenmisUygulama): String {
    OtomasyonBeyni.gorevBaslat(context, uygulama.paketAdi, mesaj)
    return "${uygulama.ad} açılıyor, hedef: \"$mesaj\""
}
