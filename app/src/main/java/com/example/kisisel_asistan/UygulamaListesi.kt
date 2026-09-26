package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class YuklenmisUygulama(val ad: String, val paketAdi: String)

object UygulamaKatalogu {
    private var uygulamalar: List<YuklenmisUygulama> = emptyList()
    private var yuklendiMi = false

    fun yukle(context: Context) {
        if (yuklendiMi) return
        yuklendiMi = true
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val cozumler = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        uygulamalar = cozumler.mapNotNull { bilgi ->
            val ad = bilgi.loadLabel(pm)?.toString()
            val paket = bilgi.activityInfo?.packageName
            if (!ad.isNullOrBlank() && !paket.isNullOrBlank()) YuklenmisUygulama(ad, paket) else null
        }.distinctBy { it.paketAdi }
    }

    fun eslesenUygulamaBul(mesaj: String): YuklenmisUygulama? {
        val kucukMesaj = mesaj.lowercase()
        return uygulamalar
            .filter { kucukMesaj.contains(it.ad.lowercase()) }
            .maxByOrNull { it.ad.length }
    }
}
