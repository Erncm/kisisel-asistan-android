package com.example.kisisel_asistan

import android.content.Context

object SabahOzetiAyarlari {
    private const val PREFS = "asistan_sabah_ozeti"
    private const val ANAHTAR_AKTIF = "aktif"
    private const val ANAHTAR_SAAT = "saat"
    private const val ANAHTAR_DAKIKA = "dakika"

    fun aktifMi(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ANAHTAR_AKTIF, false)

    fun saatOku(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(ANAHTAR_SAAT, 8)

    fun dakikaOku(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(ANAHTAR_DAKIKA, 0)

    fun kaydet(context: Context, aktif: Boolean, saat: Int, dakika: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ANAHTAR_AKTIF, aktif)
            .putInt(ANAHTAR_SAAT, saat)
            .putInt(ANAHTAR_DAKIKA, dakika)
            .apply()
    }
}
