package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val SAGLIK_IZINLERI = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class)
)

fun healthConnectDurumKodu(context: Context): Int {
    return HealthConnectClient.getSdkStatus(context)
}

fun healthConnectKurulumuVarMi(context: Context): Boolean {
    return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
}

fun healthConnectUygulamasiniAc(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(playStoreIntent)
        } catch (e: Exception) {
            // Play Store da yoksa sessizce geç
        }
    }
}

data class SaglikVerisi(val adimSayisi: Long?, val uykuSuresi: Duration?)

sealed class SaglikSonucu {
    data class Basarili(val veri: SaglikVerisi) : SaglikSonucu()
    data class Hata(val mesaj: String) : SaglikSonucu()
}

suspend fun bugunkuSaglikVerisiniGetirDetayli(context: Context): SaglikSonucu {
    return try {
        val client = HealthConnectClient.getOrCreate(context)
        val gunBaslangic = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val simdi = Instant.now()

        val adimSonuc = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(gunBaslangic, simdi)
            )
        )
        val adim = adimSonuc[StepsRecord.COUNT_TOTAL]

        val dunBaslangic = gunBaslangic.minusSeconds(12 * 3600)
        val uykuKayitlari = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(dunBaslangic, simdi)
            )
        ).records

        val toplamUyku = uykuKayitlari.fold(Duration.ZERO) { toplam, kayit ->
            toplam.plus(Duration.between(kayit.startTime, kayit.endTime))
        }

        SaglikSonucu.Basarili(SaglikVerisi(adim, if (toplamUyku.isZero) null else toplamUyku))
    } catch (e: Exception) {
        SaglikSonucu.Hata("${e.javaClass.simpleName}: ${e.message ?: "bilinmeyen hata"}")
    }
}
