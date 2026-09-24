package com.example.kisisel_asistan

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TTSYoneticisi {
    private var tts: TextToSpeech? = null
    private var hazir = false

    fun baslat(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { durum ->
            if (durum == TextToSpeech.SUCCESS) {
                val sonuc = tts?.setLanguage(Locale("tr", "TR"))
                hazir = sonuc != TextToSpeech.LANG_MISSING_DATA && sonuc != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun oku(metin: String) {
        if (!hazir || metin.isBlank()) return
        tts?.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "asistan_yanit")
    }

    fun durdur() {
        tts?.stop()
    }

    fun kapat() {
        tts?.shutdown()
        tts = null
        hazir = false
    }
}
