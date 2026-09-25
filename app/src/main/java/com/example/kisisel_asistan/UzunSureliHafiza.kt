package com.example.kisisel_asistan

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class HafizaKaydi(
    val kod: String,
    val ozet: String,
    val icerik: String,
    val zamanDamgasi: Long
)

private val harfKoduTablosu: Map<Char, Int> = mapOf(
    'a' to 1, 'e' to 1, 'ı' to 1,
    'b' to 2, 'c' to 2, 'ç' to 2,
    'd' to 3, 'f' to 3, 'g' to 3,
    'ğ' to 4, 'h' to 4, 'i' to 4,
    'j' to 5, 'k' to 5, 'l' to 5,
    'm' to 6, 'n' to 6, 'o' to 6,
    'ö' to 7, 'p' to 7, 'r' to 7,
    's' to 8, 'ş' to 8, 't' to 8,
    'u' to 9, 'ü' to 9, 'v' to 9, 'y' to 9, 'z' to 9
)

object HafizaDeposu {
    val kayitlar = mutableStateListOf<HafizaKaydi>()

    private var appContext: Context? = null
    private var baslatildiMi = false

    private const val PREFS_ADI = "asistan_hafiza"
    private const val ANAHTAR_KAYITLAR = "uzun_sureli_hafiza"

    fun baslat(context: Context) {
        if (baslatildiMi) return
        baslatildiMi = true
        appContext = context.applicationContext

        val prefs = appContext!!.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE)
        val json = prefs.getString(ANAHTAR_KAYITLAR, null) ?: return
        try {
            val dizi = JSONArray(json)
            for (i in 0 until dizi.length()) {
                val obj = dizi.getJSONObject(i)
                kayitlar.add(
                    HafizaKaydi(
                        kod = obj.getString("kod"),
                        ozet = obj.getString("ozet"),
                        icerik = obj.getString("icerik"),
                        zamanDamgasi = obj.getLong("zamanDamgasi")
                    )
                )
            }
        } catch (e: Exception) {
            // bozuk veri, yok say
        }
    }

    fun ekle(icerik: String): HafizaKaydi {
        val kod = kodUret(icerik)
        val ozet = icerik.trim().take(40)
        val kayit = HafizaKaydi(kod, ozet, icerik.trim(), System.currentTimeMillis())
        kayitlar.add(0, kayit)
        kaydet()
        return kayit
    }

    fun sil(kod: String) {
        kayitlar.removeAll { it.kod == kod }
        kaydet()
    }

    fun hepsiniOzetGetir(): String {
        if (kayitlar.isEmpty()) return ""
        return kayitlar.joinToString("\n") { "${it.kod}: ${it.icerik}" }
    }

    private fun kodUret(icerik: String): String {
        val harfler = icerik.lowercase().filter { it.isLetter() }
        val ilkDort = harfler.take(4)
        val rakamlar = ilkDort.map { harfKoduTablosu[it] ?: 0 }.joinToString("")
        var sira = 1
        var adayKod = "$rakamlar-${sira.toString().padStart(2, '0')}"
        while (kayitlar.any { it.kod == adayKod }) {
            sira++
            adayKod = "$rakamlar-${sira.toString().padStart(2, '0')}"
        }
        return adayKod
    }

    private fun kaydet() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE)
        val dizi = JSONArray()
        for (k in kayitlar) {
            val obj = JSONObject()
            obj.put("kod", k.kod)
            obj.put("ozet", k.ozet)
            obj.put("icerik", k.icerik)
            obj.put("zamanDamgasi", k.zamanDamgasi)
            dizi.put(obj)
        }
        prefs.edit().putString(ANAHTAR_KAYITLAR, dizi.toString()).apply()
    }
}

private val TETIKLEYICILER = listOf(
    "hafızana yaz:", "hafızana yaz",
    "bunu hatırla:", "bunu hatırla",
    "hatırla:", "hatırla"
)

fun hafizaKomutuMu(mesaj: String): Boolean {
    val kucuk = mesaj.trim().lowercase()
    return TETIKLEYICILER.any { kucuk.startsWith(it) }
}

fun hafizaIcerigiCikar(mesaj: String, oncekiMesaj: String?): String {
    val kucuk = mesaj.trim().lowercase()
    val eslesen = TETIKLEYICILER.firstOrNull { kucuk.startsWith(it) } ?: return mesaj
    val kalan = mesaj.trim().substring(eslesen.length).trim(':', ' ', '-')
    return if (kalan.isNotBlank()) kalan else (oncekiMesaj ?: mesaj)
}
