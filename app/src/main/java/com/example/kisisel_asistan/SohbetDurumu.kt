package com.example.kisisel_asistan

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

data class SohbetOturumu(
    val id: Long,
    val mesajlar: List<ChatMesaj>,
    val zamanDamgasi: Long
)

object SohbetDurumu {
    val aktifMesajlar = mutableStateListOf<ChatMesaj>()
    val gecmisOturumlar = mutableStateListOf<SohbetOturumu>()

    fun yeniSohbetBaslat() {
        if (aktifMesajlar.isNotEmpty()) {
            gecmisOturumlar.add(
                0,
                SohbetOturumu(
                    id = System.currentTimeMillis(),
                    mesajlar = aktifMesajlar.toList(),
                    zamanDamgasi = System.currentTimeMillis()
                )
            )
        }
        aktifMesajlar.clear()
    }

    fun oturumuYukle(oturum: SohbetOturumu) {
        if (aktifMesajlar.isNotEmpty()) {
            gecmisOturumlar.removeAll { it.id == oturum.id }
            gecmisOturumlar.add(
                0,
                SohbetOturumu(
                    id = System.currentTimeMillis(),
                    mesajlar = aktifMesajlar.toList(),
                    zamanDamgasi = System.currentTimeMillis()
                )
            )
        } else {
            gecmisOturumlar.removeAll { it.id == oturum.id }
        }
        aktifMesajlar.clear()
        aktifMesajlar.addAll(oturum.mesajlar)
    }
}
