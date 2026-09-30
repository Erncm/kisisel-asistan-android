package com.example.kisisel_asistan

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GenelGorev(
    val hedefPaket: String,
    val talimat: String,
    var adimSayaci: Int = 0,
    var sonEkranListesi: String = "",
    var degismeyenAdimSayaci: Int = 0,
    val gecmisKararlar: MutableList<String> = mutableListOf()
)

object OtomasyonBeyni {
    var aktifGorev: GenelGorev? by mutableStateOf(null)
    private val kapsam = CoroutineScope(Dispatchers.Default)
    private var calisanIs: Job? = null
    private const val MAKS_ADIM = 12
    private const val MAKS_DEGISMEYEN_ADIM = 3

    fun gorevBaslat(context: Context, hedefPaket: String, talimat: String) {
        aktifGorev = GenelGorev(hedefPaket, talimat)
        OtomasyonKuyrugu.durum = "\"$talimat\" için uygulama açılıyor"
        val intent = context.packageManager.getLaunchIntentForPackage(hedefPaket)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            OtomasyonKuyrugu.durum = "Uygulama telefonda kurulu değil"
            aktifGorev = null
        }
    }

    fun devamGoreviBaslat(hedefPaket: String, talimat: String) {
        aktifGorev = GenelGorev(hedefPaket, talimat)
        OtomasyonKuyrugu.durum = "Sonuçlar bekleniyor..."
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
            OtomasyonKuyrugu.durum = "Görev durduruldu: çok fazla adım denendi, elle devam etmen gerekebilir"
            aktifGorev = null
            return
        }
        gorev.adimSayaci++

        OtomasyonKuyrugu.durum = "Ekran taranıyor (adım ${gorev.adimSayaci})"
        val kok = servis.ekranKokunuGetir()
        val ekranListesi = EkranOkuyucu.ekraniListele(kok)

        val ekranDegisti = ekranListesi != gorev.sonEkranListesi
        if (ekranDegisti) {
            gorev.degismeyenAdimSayaci = 0
        } else {
            gorev.degismeyenAdimSayaci++
        }
        gorev.sonEkranListesi = ekranListesi

        if (gorev.degismeyenAdimSayaci >= MAKS_DEGISMEYEN_ADIM) {
            OtomasyonKuyrugu.durum = "Görev durduruldu: ekran değişmiyor, hedefe ulaşılamadı"
            aktifGorev = null
            return
        }

        val gecmisOzeti = if (gorev.gecmisKararlar.isEmpty()) {
            "Henüz bir şey denenmedi."
        } else {
            "Şimdiye kadar denedikleriniz (sırayla): ${gorev.gecmisKararlar.takeLast(5).joinToString(", ")}"
        }
        val uyari = if (!ekranDegisti && gorev.gecmisKararlar.isNotEmpty()) {
            "\nUYARI: Son kararından sonra ekran değişmedi, o eleman işe yaramamış olabilir. Farklı bir eleman dene ya da hedefe ulaşamıyorsan \"DURDUR\" yaz."
        } else ""

        val prompt = """
Sen bir Android otomasyon asistanısın. Kullanıcının hedefi: "${gorev.talimat}"
Şu an ekranda görünen, numaralandırılmış elemanlar:
$ekranListesi

$gecmisOzeti$uyari

Kurallar:
- Hedef tamamlandıysa sadece "BITTI" yaz.
- Bir elemana dokunman gerekiyorsa sadece "TIKLA: <numara>" yaz.
- Hedefe ulaşamayacağını düşünüyorsan "DURDUR" yaz.
- Emin değilsen "BEKLE" yaz.
Başka hiçbir açıklama yazma, sadece yukarıdaki formatlardan birini kullan.
        """.trimIndent()

        OtomasyonKuyrugu.durum = "AI'a soruluyor (adım ${gorev.adimSayaci})"
        val cevap = aiCevapAl(context, prompt)
        if (cevap == null) {
            OtomasyonKuyrugu.durum = "AI'a ulaşılamadı, görev durduruldu"
            aktifGorev = null
            return
        }

        val temiz = cevap.trim()

        when {
            temiz.startsWith("BITTI", ignoreCase = true) -> {
                OtomasyonKuyrugu.durum = "Tamamlandı ✓"
                aktifGorev = null
            }
            temiz.startsWith("DURDUR", ignoreCase = true) -> {
                OtomasyonKuyrugu.durum = "Görev durduruldu: hedefe ulaşılamadı, elle devam edebilirsin"
                aktifGorev = null
            }
            temiz.startsWith("TIKLA", ignoreCase = true) -> {
                val numara = Regex("\\d+").find(temiz)?.value?.toIntOrNull()
                if (numara != null) {
                    gorev.gecmisKararlar.add("TIKLA:$numara")
                    OtomasyonKuyrugu.durum = "$numara numaralı elemana tıklanıyor"
                    val basarili = EkranOkuyucu.numaraylaTikla(numara)
                    if (!basarili) {
                        OtomasyonKuyrugu.durum = "$numara numaralı elemana tıklanamadı"
                    }
                } else {
                    OtomasyonKuyrugu.durum = "AI'ın cevabı anlaşılamadı, tekrar deneniyor"
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
        OtomasyonKuyrugu.durum = "Görev iptal edildi"
        aktifGorev = null
        calisanIs?.cancel()
    }
}
