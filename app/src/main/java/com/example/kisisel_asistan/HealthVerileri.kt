package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val SAGLIK_IZINLERI = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class)
)

fun healthConnectDurumKodu(context: Context): Int = HealthConnectClient.getSdkStatus(context)

fun healthConnectKurulumuVarMi(context: Context): Boolean =
    HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

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
        try { context.startActivity(playStoreIntent) } catch (e: Exception) { }
    }
}

data class AntrenmanOzeti(val baslik: String, val sure: Duration, val baslangic: Instant)

data class SaglikVerisi(
    val adimSayisi: Long? = null,
    val uykuSuresi: Duration? = null,
    val nabizOrtalama: Long? = null,
    val nabizMaks: Long? = null,
    val oksijenYuzdesi: Double? = null,
    val kaloriToplam: Double? = null,
    val hareketSuresi: Duration? = null,
    val antrenmanlar: List<AntrenmanOzeti> = emptyList()
)

sealed class SaglikSonucu {
    data class Basarili(val veri: SaglikVerisi) : SaglikSonucu()
    data class Hata(val mesaj: String) : SaglikSonucu()
}

suspend fun bugunkuSaglikVerisiniGetirDetayli(context: Context): SaglikSonucu {
    val client = try {
        HealthConnectClient.getOrCreate(context)
    } catch (e: Exception) {
        return SaglikSonucu.Hata("${e.javaClass.simpleName}: ${e.message ?: "bağlantı kurulamadı"}")
    }

    val gunBaslangic = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
    val simdi = Instant.now()
    val araliq = TimeRangeFilter.between(gunBaslangic, simdi)

    var adim: Long? = null
    var nabizOrt: Long? = null
    var nabizMaks: Long? = null
    var oksijen: Double? = null
    var kalori: Double? = null
    var uyku: Duration? = null
    var hareket: Duration? = null
    val antrenmanlar = mutableListOf<AntrenmanOzeti>()

    try {
        val sonuc = client.aggregate(AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = araliq))
        adim = sonuc[StepsRecord.COUNT_TOTAL]
    } catch (e: Exception) { }

    try {
        val sonuc = client.aggregate(
            AggregateRequest(metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX), timeRangeFilter = araliq)
        )
        nabizOrt = sonuc[HeartRateRecord.BPM_AVG]
        nabizMaks = sonuc[HeartRateRecord.BPM_MAX]
    } catch (e: Exception) { }

    try {
        val kayitlar = client.readRecords(
            ReadRecordsRequest(recordType = OxygenSaturationRecord::class, timeRangeFilter = araliq)
        ).records
        oksijen = kayitlar.maxByOrNull { it.time }?.percentage?.value
    } catch (e: Exception) { }

    try {
        val sonuc = client.aggregate(AggregateRequest(metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), timeRangeFilter = araliq))
        kalori = sonuc[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
    } catch (e: Exception) { }

    try {
        val dunBaslangic = gunBaslangic.minusSeconds(12 * 3600)
        val uykuKayitlari = client.readRecords(
            ReadRecordsRequest(recordType = SleepSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(dunBaslangic, simdi))
        ).records
        val toplam = uykuKayitlari.fold(Duration.ZERO) { t, k -> t.plus(Duration.between(k.startTime, k.endTime)) }
        uyku = if (toplam.isZero) null else toplam
    } catch (e: Exception) { }

    try {
        val kayitlar = client.readRecords(
            ReadRecordsRequest(recordType = ExerciseSessionRecord::class, timeRangeFilter = araliq)
        ).records
        var toplamHareket = Duration.ZERO
        for (kayit in kayitlar) {
            val sure = Duration.between(kayit.startTime, kayit.endTime)
            toplamHareket = toplamHareket.plus(sure)
            antrenmanlar.add(
                AntrenmanOzeti(
                    baslik = kayit.title?.takeIf { it.isNotBlank() } ?: "Antrenman",
                    sure = sure,
                    baslangic = kayit.startTime
                )
            )
        }
        hareket = if (toplamHareket.isZero) null else toplamHareket
    } catch (e: Exception) { }

    return SaglikSonucu.Basarili(
        SaglikVerisi(
            adimSayisi = adim,
            uykuSuresi = uyku,
            nabizOrtalama = nabizOrt,
            nabizMaks = nabizMaks,
            oksijenYuzdesi = oksijen,
            kaloriToplam = kalori,
            hareketSuresi = hareket,
            antrenmanlar = antrenmanlar.sortedByDescending { it.baslangic }
        )
    )
}
