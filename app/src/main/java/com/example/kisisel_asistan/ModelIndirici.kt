package com.example.kisisel_asistan

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val MODEL_URL = "https://github.com/Erncm/kisisel-asistan-android/releases/download/model-v1/qwen2.5-1.5b-instruct-q4_k_m.gguf"
private const val MODEL_DOSYA_ADI = "qwen2.5-1.5b-instruct-q4_k_m.gguf"

fun modelDosyasi(context: Context): File {
    return File(context.filesDir, MODEL_DOSYA_ADI)
}

fun modelVarMi(context: Context): Boolean {
    val dosya = modelDosyasi(context)
    return dosya.exists() && dosya.length() > 0
}

sealed class IndirmeSonucu {
    object Basarili : IndirmeSonucu()
    data class Hata(val mesaj: String) : IndirmeSonucu()
}

suspend fun modelIndir(context: Context, onIlerleme: (Int) -> Unit): IndirmeSonucu {
    return withContext(Dispatchers.IO) {
        val geciciDosya = File(context.filesDir, "$MODEL_DOSYA_ADI.tmp")
        try {
            val url = URL(MODEL_URL)
            val baglanti = url.openConnection() as HttpURLConnection
            baglanti.requestMethod = "GET"
            baglanti.connectTimeout = 15000
            baglanti.readTimeout = 15000
            baglanti.instanceFollowRedirects = true

            val kod = baglanti.responseCode
            if (kod !in 200..299) {
                return@withContext IndirmeSonucu.Hata("HTTP $kod")
            }

            val toplamBoyut = baglanti.contentLengthLong
            var indirilenBoyut = 0L

            baglanti.inputStream.use { girisAkisi ->
                geciciDosya.outputStream().use { cikisAkisi ->
                    val tampon = ByteArray(8192)
                    var okunan: Int
                    var sonYuzde = -1
                    while (girisAkisi.read(tampon).also { okunan = it } != -1) {
                        cikisAkisi.write(tampon, 0, okunan)
                        indirilenBoyut += okunan
                        if (toplamBoyut > 0) {
                            val yuzde = ((indirilenBoyut * 100) / toplamBoyut).toInt()
                            if (yuzde != sonYuzde) {
                                sonYuzde = yuzde
                                onIlerleme(yuzde)
                            }
                        }
                    }
                }
            }

            val hedefDosya = modelDosyasi(context)
            if (hedefDosya.exists()) hedefDosya.delete()
            geciciDosya.renameTo(hedefDosya)

            IndirmeSonucu.Basarili
        } catch (e: Exception) {
            geciciDosya.delete()
            IndirmeSonucu.Hata(e.message ?: e.javaClass.simpleName)
        }
    }
}

fun modelSil(context: Context) {
    modelDosyasi(context).delete()
}
