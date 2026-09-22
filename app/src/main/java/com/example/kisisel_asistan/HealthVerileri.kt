package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import android.net.Uri
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

fun healthConnectDurumMesaji(context: Context): String {
    return when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_UNAVAILABLE -> "Bu cihaz Health Connect'i desteklemiyor"
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect güncellenmeli"
        HealthConnectClient.SDK_AVAILABLE -> "Kullanılabilir"
        else -> "Bilinmiyor"
    }
}

fun healthConnectMagazaIntenti(): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
    }
}

sealed class AdimSonucu {
    data class Basarili(val adim: Long) : AdimSonucu()
    data class Hata(val mesaj: String) : AdimSonucu()
}

suspend fun bugunkuAdimSayisiDetayli(context: Context): AdimSonucu {
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
        val adim = sonuc[StepsRecord.COUNT_TOTAL]
        if (adim != null) {
            AdimSonucu.Basarili(adim)
        } else {
            AdimSonucu.Basarili(0L)
        }
    } catch (e: Exception) {
        AdimSonucu.Hata(e.message ?: e.javaClass.simpleName)
    }
}
