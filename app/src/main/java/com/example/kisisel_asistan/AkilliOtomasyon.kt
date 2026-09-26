package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GenelGorev(
    val hedefPaket: String,
    val talimat: String,
    var adimSayaci: Int = 0
)

object OtomasyonBeyni {
    var aktifGorev: GenelGorev? = null
    private val kapsam = CoroutineScope(Dispatchers.Default)
    private var calisanIs: Job? = null
    private const val MAKS_ADIM = 12

    fun gorevBaslat(context: Context, hedefPaket: String, talimat: String) {
        aktifGorev = GenelGorev(hedefPaket, talimat)
        OtomasyonKuyrugu.durum = "Görev başlıyor: $talimat"
        val intent = context.packageManager.getLaunchIntentForPackage(hedefPaket)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            OtomasyonKuyrugu.durum = "Uygulama telefonda kurulu değil: $hedefPaket"
            aktifGorev = null
        }
    }

    fun ekranDegistiginde(context: Context, servis: AsistanErisilebilirlikServisi) {
        val gorev = aktifGorev ?: return
        if (calisanIs?.isActive == true) return
        calisanIs = kapsam.launch {
            delay(600)
            adimIsle(context, servis, gorev)
        }
    }

    private suspend fun adimIsle(context: Context, servis: AsistanErisilebilirlikServisi, gorev: GenelGorev) {
        if (gorev.adimSayaci >= MAKS_ADIM) {
            OtomasyonKuyrugu.durum = "Görev durduruldu: adım limiti aşıldı"
            aktifGorev = null
            return
        }
        gorev.adimSayaci++

        val kok = servis.ekranKokunuGetir()
        val ekranListesi = EkranOkuyucu.ekraniListele(kok)

        val prompt = """
Sen bir Android otomasyon asistanısın. Kullanıcının hedefi: "${gorev.talimat}"
Şu an ekranda görünen, numaralandırılmış elemanlar:
$ekranListesi

Kurallar:
- Eğer hedef tamamlandıysa sadece "BITTI" yaz.
- Bir elemana dokunman gerekiyorsa sadece "TIKLA: <numara>" yaz.
- Emin değilsen ya da uygun eleman yoksa "BEKLE" yaz.
Başka hiçbir açıklama yazma, sadece yukarıdaki formatlardan birini kullan.
        """.trimIndent()

        val cevap = aiCevapAl(context, prompt)
        if (cevap == null) {
            OtomasyonKuyrugu.durum = "AI'a ulaşılamadı"
            return
        }

        val temiz = cevap.trim()
        OtomasyonKuyrugu.durum = "AI: $temiz"

        when {
            temiz.startsWith("BITTI", ignoreCase = true) -> {
                OtomasyonKuyrugu.durum = "Görev tamamlandı ✓"
                aktifGorev = null
            }
            temiz.startsWith("TIKLA", ignoreCase = true) -> {
                val numara = Regex("\\d+").find(temiz)?.value?.toIntOrNull()
                if (numara != null) {
                    val basarili = EkranOkuyucu.numaraylaTikla(numara)
                    OtomasyonKuyrugu.durum = if (basarili) "Tıklandı: $numara" else "Tıklanamadı: $numara"
                }
            }
            else -> {
                OtomasyonKuyrugu.durum = "Bekleniyor..."
            }
        }
    }

    private suspend fun aiCevapAl(context: Context, prompt: String): String? {
        val anahtar = apiAnahtariOku(context)
        if (anahtar.isNotBlank()) {
            val sonuc = geminiYanitAl(anahtar, listOf(ChatMesaj(prompt, benMi = true)))
            if (sonuc.isSuccess) return sonuc.getOrNull()
        }
        if (modelVarMi(context)) {
            val hazir = YerelModel.hazirla(context)
            if (hazir) return YerelModel.yanitAl(prompt)
        }
        return null
    }

    fun iptalEt() {
        aktifGorev = null
        calisanIs?.cancel()
    }
}
