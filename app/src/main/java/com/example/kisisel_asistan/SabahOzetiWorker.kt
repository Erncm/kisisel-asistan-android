package com.example.kisisel_asistan

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SabahOzetiWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KANAL_ID = "sabah_ozeti_kanali"
        const val BILDIRIM_ID = 601
        const val IS_ADI = "sabah_ozeti_isi"
    }

    override suspend fun doWork(): Result {
        val ozetMetni = ozetOlustur()
        bildirimGoster(ozetMetni)
        return Result.success()
    }

    private fun ozetOlustur(): String {
        val onbellekHava = havaDurumuOnbellekOku(applicationContext)
        val havaKismi = if (onbellekHava != null) {
            "${onbellekHava.sehir}: ${onbellekHava.sicaklik.toInt()}°C, ${onbellekHava.aciklama}"
        } else {
            "Hava durumu verisi henüz yok"
        }

        val hafizaOzeti = HafizaDeposu.hepsiniOzetGetir()
        val hafizaKismi = if (hafizaOzeti.isNotBlank()) {
            "\n\nHatırlatma: ${hafizaOzeti.lines().take(2).joinToString(" • ")}"
        } else ""

        return "Günaydın! $havaKismi$hafizaKismi"
    }

    private fun bildirimGoster(metin: String) {
        bildirimKanaliOlustur()

        val acmaIntent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, acmaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, KANAL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
        }

        val bildirim = builder
            .setContentTitle("Günün Özeti")
            .setContentText(metin)
            .setStyle(Notification.BigTextStyle().bigText(metin))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(applicationContext).notify(BILDIRIM_ID, bildirim)
        }
    }

    private fun bildirimKanaliOlustur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(KANAL_ID, "Sabah Özeti", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(kanal)
        }
    }
}

object SabahOzetiZamanlayici {
    fun zamanlaGunluk(context: Context, saat: Int, dakika: Int) {
        val simdi = Calendar.getInstance()
        val hedef = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, saat)
            set(Calendar.MINUTE, dakika)
            set(Calendar.SECOND, 0)
            if (before(simdi)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val gecikmeMs = hedef.timeInMillis - simdi.timeInMillis

        val istek = androidx.work.PeriodicWorkRequestBuilder<SabahOzetiWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(gecikmeMs, TimeUnit.MILLISECONDS)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SabahOzetiWorker.IS_ADI,
            androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            istek
        )
    }

    fun iptalEt(context: Context) {
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork(SabahOzetiWorker.IS_ADI)
    }
}
