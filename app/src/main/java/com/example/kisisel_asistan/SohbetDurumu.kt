package com.example.kisisel_asistan

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class SohbetOturumu(
    val id: Long,
    val mesajlar: List<ChatMesaj>,
    val zamanDamgasi: Long
)

object SohbetDurumu {
    val aktifMesajlar = mutableStateListOf<ChatMesaj>()
    val gecmisOturumlar = mutableStateListOf<SohbetOturumu>()

    private var appContext: Context? = null
    private var baslatildiMi = false

    private const val PREFS_ADI = "asistan_sohbet"
    private const val ANAHTAR_GECMIS = "gecmis_oturumlar"
    private const val ANAHTAR_AKTIF = "aktif_mesajlar"

    fun baslat(context: Context) {
        if (baslatildiMi) return
        baslatildiMi = true
        appContext = context.applicationContext

        val prefs = appContext!!.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE)

        prefs.getString(ANAHTAR_GECMIS, null)?.let { json ->
            gecmisOturumlar.addAll(oturumlariCoz(json))
        }

        prefs.getString(ANAHTAR_AKTIF, null)?.let { json ->
            val kurtarilan = mesajlariCoz(json)
            if (kurtarilan.isNotEmpty()) {
                gecmisOturumlar.add(
                    0,
                    SohbetOturumu(System.currentTimeMillis(), kurtarilan, System.currentTimeMillis())
                )
            }
        }

        prefs.edit().remove(ANAHTAR_AKTIF).apply()
        kaydetGecmis()
    }

    fun yeniSohbetBaslat() {
        if (aktifMesajlar.isNotEmpty()) {
            gecmisOturumlar.add(
                0,
                SohbetOturumu(System.currentTimeMillis(), aktifMesajlar.toList(), System.currentTimeMillis())
            )
            kaydetGecmis()
        }
        aktifMesajlar.clear()
        kaydetAktif()
    }

    fun oturumuYukle(oturum: SohbetOturumu) {
        if (aktifMesajlar.isNotEmpty()) {
            gecmisOturumlar.removeAll { it.id == oturum.id }
            gecmisOturumlar.add(
                0,
                SohbetOturumu(System.currentTimeMillis(), aktifMesajlar.toList(), System.currentTimeMillis())
            )
        } else {
            gecmisOturumlar.removeAll { it.id == oturum.id }
        }
        aktifMesajlar.clear()
        aktifMesajlar.addAll(oturum.mesajlar)
        kaydetGecmis()
        kaydetAktif()
    }

    fun mesajEkle(mesaj: ChatMesaj) {
        aktifMesajlar.add(mesaj)
        kaydetAktif()
    }

    private fun kaydetAktif() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE)
        if (aktifMesajlar.isEmpty()) {
            prefs.edit().remove(ANAHTAR_AKTIF).apply()
        } else {
            prefs.edit().putString(ANAHTAR_AKTIF, mesajlariJsonYap(aktifMesajlar)).apply()
        }
    }

    private fun kaydetGecmis() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE)
        prefs.edit().putString(ANAHTAR_GECMIS, oturumlariJsonYap(gecmisOturumlar)).apply()
    }

    private fun mesajlariJsonYap(mesajlar: List<ChatMesaj>): String {
        val dizi = JSONArray()
        for (m in mesajlar) {
            val obj = JSONObject()
            obj.put("icerik", m.icerik)
            obj.put("benMi", m.benMi)
            obj.put("baglamaDahilMi", m.baglamaDahilMi)
            dizi.put(obj)
        }
        return dizi.toString()
    }

    private fun mesajlariCoz(json: String): List<ChatMesaj> {
        return try {
            val dizi = JSONArray(json)
            val liste = mutableListOf<ChatMesaj>()
            for (i in 0 until dizi.length()) {
                val obj = dizi.getJSONObject(i)
                liste.add(
                    ChatMesaj(
                        obj.getString("icerik"),
                        obj.getBoolean("benMi"),
                        obj.optBoolean("baglamaDahilMi", true)
                    )
                )
            }
            liste
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun oturumlariJsonYap(oturumlar: List<SohbetOturumu>): String {
        val dizi = JSONArray()
        for (o in oturumlar) {
            val obj = JSONObject()
            obj.put("id", o.id)
            obj.put("zamanDamgasi", o.zamanDamgasi)
            obj.put("mesajlar", JSONArray(mesajlariJsonYap(o.mesajlar)))
            dizi.put(obj)
        }
        return dizi.toString()
    }

    private fun oturumlariCoz(json: String): List<SohbetOturumu> {
        return try {
            val dizi = JSONArray(json)
            val liste = mutableListOf<SohbetOturumu>()
            for (i in 0 until dizi.length()) {
                val obj = dizi.getJSONObject(i)
                val mesajlarDizi = obj.getJSONArray("mesajlar")
                val mesajlar = mutableListOf<ChatMesaj>()
                for (j in 0 until mesajlarDizi.length()) {
                    val mObj = mesajlarDizi.getJSONObject(j)
                    mesajlar.add(
                        ChatMesaj(
                            mObj.getString("icerik"),
                            mObj.getBoolean("benMi"),
                            mObj.optBoolean("baglamaDahilMi", true)
                        )
                    )
                }
                liste.add(SohbetOturumu(obj.getLong("id"), mesajlar, obj.getLong("zamanDamgasi")))
            }
            liste
        } catch (e: Exception) {
            emptyList()
        }
    }
}
