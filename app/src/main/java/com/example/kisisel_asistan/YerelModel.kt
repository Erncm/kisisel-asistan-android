package com.example.kisisel_asistan

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YerelModel {
    private var yuklendi = false

    suspend fun hazirla(context: Context): Boolean {
        if (yuklendi) return true
        if (!modelVarMi(context)) return false

        return withContext(Dispatchers.Default) {
            val basarili = NativeBridge.modelYukle(modelDosyasi(context).absolutePath)
            yuklendi = basarili
            basarili
        }
    }

    suspend fun yanitAl(mesaj: String): String {
        return withContext(Dispatchers.Default) {
            NativeBridge.yanitUret(mesaj)
        }
    }

    fun yuklendiMi() = yuklendi
}
