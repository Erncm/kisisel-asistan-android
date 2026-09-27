package com.example.kisisel_asistan

import android.content.Context

object UyanmaAyarlari {
    private const val PREFS = "asistan_uyanma"
    private const val ANAHTAR_KELIME = "uyanma_kelimesi"
    private const val ANAHTAR_AKTIF = "uyanma_aktif"

    fun kelimeyiOku(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ANAHTAR_KELIME, "") ?: ""
    }

    fun kelimeyiKaydet(context: Context, kelime: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ANAHTAR_KELIME, kelime).apply()
    }

    fun aktifMi(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ANAHTAR_AKTIF, false)
    }

    fun aktifligiKaydet(context: Context, aktif: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ANAHTAR_AKTIF, aktif).apply()
    }
}
