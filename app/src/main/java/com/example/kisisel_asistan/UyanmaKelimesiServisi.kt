package com.example.kisisel_asistan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class UyanmaKelimesiServisi : Service() {

    private var tanimlayici: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var calisiyorMu = false

    companion object {
        const val KANAL_ID = "uyanma_kanali"
        const val BILDIRIM_ID = 501
    }

    override fun onCreate() {
        super.onCreate()
        bildirimKanaliOlustur()
        startForeground(BILDIRIM_ID, bildirimOlustur())
        calisiyorMu = true
        dinlemeyeBasla()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun bildirimKanaliOlustur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(KANAL_ID, "Uyanma Kelimesi", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(kanal)
        }
    }

    private fun bildirimOlustur(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, KANAL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Kişisel Asistan dinliyor")
            .setContentText("Uyanma kelimesi bekleniyor")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun dinlemeyeBasla() {
        if (!calisiyorMu) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handler.postDelayed({ dinlemeyeBasla() }, 3000)
            return
        }

        tanimlayici?.destroy()
        tanimlayici = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val metinler = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val tanininMetin = metinler?.firstOrNull()?.lowercase() ?: ""
                    val hedefKelime = UyanmaAyarlari.kelimeyiOku(this@UyanmaKelimesiServisi).lowercase()
                    if (hedefKelime.isNotBlank() && tanininMetin.contains(hedefKelime)) {
                        uyanmaTetiklendi()
                    } else {
                        handler.postDelayed({ dinlemeyeBasla() }, 300)
                    }
                }

                override fun onError(error: Int) {
                    handler.postDelayed({ dinlemeyeBasla() }, 800)
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
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

    private fun uyanmaTetiklendi() {
        tanimlayici?.destroy()
        tanimlayici = null
        TTSYoneticisi.oku("Buyurun")

        val acmaIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(acmaIntent)

        handler.postDelayed({ dinlemeyeBasla() }, 2500)
    }

    override fun onDestroy() {
        calisiyorMu = false
        tanimlayici?.destroy()
        tanimlayici = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
