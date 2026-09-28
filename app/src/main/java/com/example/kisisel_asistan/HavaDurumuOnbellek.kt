package com.example.kisisel_asistan

import android.content.Context

data class HavaOnbellekVerisi(
    val sehir: String,
    val sicaklik: Double,
    val aciklama: String
)

private const val PREFS_ADI = "hava_durumu_onbellek"

fun havaDurumuOnbellekKaydet(context: Context, veri: HavaOnbellekVerisi) {
    context.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE).edit()
        .putString("sehir", veri.sehir)
        .putFloat("sicaklik", veri.sicaklik.toFloat())
        .putString("aciklama", veri.aciklama)
        .apply()
}

fun havaDurumuOnbellekOku(context: Context): HavaOnbellekVerisi? {
    val prefs = context.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE)
    val sehir = prefs.getString("sehir", null) ?: return null
    val aciklama = prefs.getString("aciklama", null) ?: return null
    return HavaOnbellekVerisi(sehir, prefs.getFloat("sicaklik", 0f).toDouble(), aciklama)
}
