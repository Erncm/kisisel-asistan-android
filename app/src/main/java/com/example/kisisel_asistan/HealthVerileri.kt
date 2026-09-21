package com.example.kisisel_asistan

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val HEALTH_CONNECT_IZINLERI = setOf(
    HealthPermission.getReadPermission(StepsRecord::class)
)

fun healthConnectKurulumuVarMi(context: Context): Boolean {
    return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
}

suspend fun bugunkuAdimSayisi(context: Context): Long? {
    return try {
        val client = HealthConnectClient.getOrCreate(context)
        val bugunBaslangic = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val simdi = Instant.now()
        val sonuc = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(bugunBaslangic, simdi)
            )
        )
        sonuc[StepsRecord.COUNT_TOTAL]
    } catch (e: Exception) {
        null
    }
}
