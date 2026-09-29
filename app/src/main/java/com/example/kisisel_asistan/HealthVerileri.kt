package com.example.kisisel_asistan

import android.content.Context
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

fun healthConnectKurulumuVarMi(context: Context): Boolean {
    return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
}

data class SaglikVerisi(val adimSayisi: Long?, val uykuSuresi: Duration?)

suspend fun bugunkuSaglikVerisiniGetir(context: Context): SaglikVerisi {
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

        SaglikVerisi(adim, if (toplamUyku.isZero) null else toplamUyku)
    } catch (e: Exception) {
        SaglikVerisi(null, null)
    }
}
