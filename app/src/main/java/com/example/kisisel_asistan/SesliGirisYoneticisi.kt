package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

object SesliGirisYoneticisi {
    private var tanimlayici: SpeechRecognizer? = null

    fun dinlemeyeBasla(
        context: Context,
        onSonuc: (String) -> Unit,
        onDurum: (String) -> Unit,
        onBitti: () -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onDurum("Ses tanıma bu cihazda kullanılamıyor")
            onBitti()
            return
        }

        tanimlayici?.destroy()
        tanimlayici = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val metinler = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val tanininMetin = metinler?.firstOrNull()
                    if (!tanininMetin.isNullOrBlank()) {
                        onSonuc(tanininMetin)
                    }
                    onBitti()
                }

                override fun onError(error: Int) {
                    onDurum("Anlaşılamadı")
                    onBitti()
                }

                override fun onReadyForSpeech(params: Bundle?) { onDurum("Dinliyor...") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onDurum("İşleniyor...") }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val niyet = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            startListening(niyet)
        }
    }

    fun durdur() {
        tanimlayici?.stopListening()
        tanimlayici?.destroy()
        tanimlayici = null
    }
}
