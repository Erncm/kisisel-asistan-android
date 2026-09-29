package com.example.kisisel_asistan

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

@SuppressLint("MissingPermission")
fun sonBilinenKonumuAl(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val saglayicilar = try { lm.getProviders(true) } catch (e: Exception) { emptyList<String>() }
    for (saglayici in saglayicilar) {
        try {
            val konum = lm.getLastKnownLocation(saglayici)
            if (konum != null) return konum
        } catch (e: SecurityException) {
            return null
        }
    }
    return null
}
