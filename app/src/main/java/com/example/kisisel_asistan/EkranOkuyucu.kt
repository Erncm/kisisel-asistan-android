package com.example.kisisel_asistan

import android.view.accessibility.AccessibilityNodeInfo

data class EkranElemani(
    val numara: Int,
    val metin: String,
    val node: AccessibilityNodeInfo
)

object EkranOkuyucu {
    var sonListelenenElemanlar: List<EkranElemani> = emptyList()

    fun ekraniListele(kok: AccessibilityNodeInfo?): String {
        if (kok == null) return "[Ekran okunamadı]"

        val elemanlar = mutableListOf<EkranElemani>()
        topla(kok, elemanlar)

        sonListelenenElemanlar = elemanlar

        if (elemanlar.isEmpty()) return "[Ekranda okunabilir eleman bulunamadı]"

        return elemanlar.joinToString("\n") { "${it.numara}: ${it.metin}" }
    }

    private fun topla(node: AccessibilityNodeInfo, liste: MutableList<EkranElemani>) {
        val metin = node.text?.toString()?.trim()
        val aciklama = node.contentDescription?.toString()?.trim()
        val gosterilecekMetin = when {
            !metin.isNullOrBlank() -> metin
            !aciklama.isNullOrBlank() -> aciklama
            else -> null
        }

        if (gosterilecekMetin != null && (node.isClickable || node.isVisibleToUser) && gosterilecekMetin.length in 1..80) {
            liste.add(EkranElemani(liste.size + 1, gosterilecekMetin, node))
        }

        for (i in 0 until node.childCount) {
            val cocuk = node.getChild(i) ?: continue
            topla(cocuk, liste)
        }
    }

    fun numaraylaTikla(numara: Int): Boolean {
        val eleman = sonListelenenElemanlar.firstOrNull { it.numara == numara } ?: return false
        var hedefNode: AccessibilityNodeInfo? = eleman.node
        var denemeSayisi = 0
        while (hedefNode != null && !hedefNode.isClickable && denemeSayisi < 5) {
            hedefNode = hedefNode.parent
            denemeSayisi++
        }
        return hedefNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
}
