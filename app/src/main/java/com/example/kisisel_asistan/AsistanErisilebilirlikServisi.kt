package com.example.kisisel_asistan

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AsistanErisilebilirlikServisi : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var asamaNo = 0
    private var calisiyorMu = false

    companion object {
        var aktifOrnek: AsistanErisilebilirlikServisi? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        aktifOrnek = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val paketAdi = event?.packageName?.toString() ?: return

        OtomasyonBeyni.aktifGorev?.let { gorev ->
            if (paketAdi == gorev.hedefPaket) {
                OtomasyonBeyni.ekranDegistiginde(applicationContext, this)
            }
        }

        if (paketAdi != "com.whatsapp") return

        val gorev = OtomasyonKuyrugu.bekleyenWhatsAppGorevi ?: return
        if (calisiyorMu) return

        calisiyorMu = true

        if (gorev.dogrudanAcildiMi) {
            OtomasyonKuyrugu.durum = "Gönder butonu aranıyor..."
            asamaNo = 2
            handler.postDelayed({ denemeYap(gorev, 0) }, 500)
        } else {
            OtomasyonKuyrugu.durum = "Sohbet listesine dönülüyor..."
            asamaNo = 0
            sohbetListesineDon(0) { adimlariBaslat(gorev) }
        }
    }

    private fun sohbetListesineDon(deneme: Int, devamEt: () -> Unit) {
        if (deneme >= 3) {
            devamEt()
            return
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({ sohbetListesineDon(deneme + 1, devamEt) }, 250)
    }

    fun ekranKokunuGetir(): AccessibilityNodeInfo? = rootInActiveWindow

    private fun adimlariBaslat(gorev: WhatsAppGorevi) {
        OtomasyonKuyrugu.durum = "Sohbet listesi aranıyor..."
        denemeYap(gorev, 0)
    }

    private fun denemeYap(gorev: WhatsAppGorevi, deneme: Int) {
        if (deneme > 15) {
            OtomasyonKuyrugu.durum = "Zaman aşımı: kişi/eleman bulunamadı"
            gorevBitir()
            return
        }

        val kok = rootInActiveWindow
        if (kok == null) {
            handler.postDelayed({ denemeYap(gorev, deneme + 1) }, 300)
            return
        }

        when (asamaNo) {
            0 -> {
                val kisiNode = derinAramaMetinle(kok, gorev.kisiAdi)
                if (kisiNode != null) {
                    kisiNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    OtomasyonKuyrugu.durum = "${gorev.kisiAdi} bulundu, sohbet açılıyor..."
                    asamaNo = 1
                    handler.postDelayed({ denemeYap(gorev, 0) }, 600)
                } else {
                    handler.postDelayed({ denemeYap(gorev, deneme + 1) }, 300)
                }
            }
            1 -> {
                val mesajKutusu = editTextBul(kok)
                if (mesajKutusu != null) {
                    val bundle = Bundle()
                    bundle.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        gorev.mesaj
                    )
                    mesajKutusu.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                    OtomasyonKuyrugu.durum = "Mesaj yazıldı, gönderiliyor..."
                    asamaNo = 2
                    handler.postDelayed({ denemeYap(gorev, 0) }, 400)
                } else {
                    handler.postDelayed({ denemeYap(gorev, deneme + 1) }, 300)
                }
            }
            2 -> {
                val gonderButonu = gonderButonuBul(kok)
                if (gonderButonu != null) {
                    gonderButonu.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    OtomasyonKuyrugu.durum = "Mesaj gönderildi ✓"
                    gorevBitir()
                } else {
                    handler.postDelayed({ denemeYap(gorev, deneme + 1) }, 300)
                }
            }
        }
    }

    private fun derinAramaMetinle(kok: AccessibilityNodeInfo, aranan: String): AccessibilityNodeInfo? {
        val bulunanlar = kok.findAccessibilityNodeInfosByText(aranan)
        return bulunanlar?.firstOrNull { it.isClickable || it.isVisibleToUser }
            ?: bulunanlar?.firstOrNull()?.parent
    }

    private fun editTextBul(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText") return node
        for (i in 0 until node.childCount) {
            val cocuk = node.getChild(i) ?: continue
            val sonuc = editTextBul(cocuk)
            if (sonuc != null) return sonuc
        }
        return null
    }

    private fun gonderButonuBul(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val aday = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (!aday.isNullOrEmpty()) return aday.first()

        for (i in 0 until node.childCount) {
            val cocuk = node.getChild(i) ?: continue
            val desc = cocuk.contentDescription?.toString()?.lowercase()
            if (desc != null && (desc.contains("gönder") || desc.contains("send"))) {
                return cocuk
            }
            val sonuc = gonderButonuBul(cocuk)
            if (sonuc != null) return sonuc
        }
        return null
    }

    private fun gorevBitir() {
        OtomasyonKuyrugu.bekleyenWhatsAppGorevi = null
        calisiyorMu = false
        asamaNo = 0
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        aktifOrnek = null
    }
}
