package com.example.kisisel_asistan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class WhatsAppGorevi(
    val kisiAdi: String,
    val mesaj: String
)

object OtomasyonKuyrugu {
    var bekleyenWhatsAppGorevi: WhatsAppGorevi? = null
    var durum: String by mutableStateOf("")
}
