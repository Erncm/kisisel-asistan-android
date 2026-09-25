package com.example.kisisel_asistan

data class WhatsAppGorevi(
    val kisiAdi: String,
    val mesaj: String
)

object OtomasyonKuyrugu {
    var bekleyenWhatsAppGorevi: WhatsAppGorevi? = null
    var durum: String = ""
}
