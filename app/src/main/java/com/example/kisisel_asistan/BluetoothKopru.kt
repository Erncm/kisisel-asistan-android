package com.example.kisisel_asistan

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

data class EslesikCihaz(val ad: String, val adres: String)

object BluetoothKopru {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var durum by mutableStateOf("Bağlı değil")
    var baglandiMi by mutableStateOf(false)

    private var soket: BluetoothSocket? = null
    private var dinlemeIsi: Job? = null
    private val kapsam = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun eslesikCihazlariListele(context: Context): List<EslesikCihaz> {
        if (!baglantiIzniVarMi(context)) return emptyList()
        val adaptor = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adaptor.bondedDevices?.map { EslesikCihaz(it.name ?: "Bilinmeyen cihaz", it.address) } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun baglantiIzniVarMi(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("MissingPermission")
    fun cihazaBaglan(context: Context, adres: String) {
        if (!baglantiIzniVarMi(context)) {
            durum = "Bluetooth izni yok"
            return
        }
        kapsam.launch {
            try {
                durum = "Bağlanılıyor..."
                val adaptor = BluetoothAdapter.getDefaultAdapter()
                val cihaz: BluetoothDevice = adaptor.getRemoteDevice(adres)
                adaptor.cancelDiscovery()

                val yeniSoket = cihaz.createRfcommSocketToServiceRecord(SPP_UUID)
                yeniSoket.connect()
                soket = yeniSoket
                baglandiMi = true
                durum = "Bağlandı: ${cihaz.name ?: adres}"
                dinlemeyeBasla()
            } catch (e: IOException) {
                durum = "Bağlantı başarısız: ${e.message ?: "bilinmeyen hata"}"
                baglandiMi = false
            } catch (e: SecurityException) {
                durum = "İzin reddedildi"
                baglandiMi = false
            }
        }
    }

    private fun dinlemeyeBasla() {
        dinlemeIsi?.cancel()
        dinlemeIsi = kapsam.launch {
            val girisAkisi = soket?.inputStream ?: return@launch
            val tampon = ByteArray(4096)
            while (baglandiMi) {
                try {
                    val okunan = girisAkisi.read(tampon)
                    if (okunan <= 0) break
                    val satir = String(tampon, 0, okunan).trim()
                    if (satir.isNotBlank()) {
                        mesajiIsle(satir)
                    }
                } catch (e: IOException) {
                    baglandiMi = false
                    durum = "Bağlantı koptu"
                    break
                }
            }
        }
    }

    private suspend fun mesajiIsle(satir: String) {
        try {
            val json = JSONObject(satir)
            when (json.optString("tip")) {
                "asistan_soru" -> {
                    val soru = json.optString("mesaj")
                    val context = ApplicationContextTutucu.context ?: return
                    val cevap = mesajiIsleVeCevapAl(context, soru)
                    cevapGonder(cevap)
                }
            }
        } catch (e: Exception) {
            // bozuk/anlaşılmaz mesaj, yok say
        }
    }

    fun cevapGonder(mesaj: String) {
        val json = JSONObject().apply {
            put("tip", "asistan_cevap")
            put("mesaj", mesaj)
        }
        veriGonder(json)
    }

    fun saglikOzetiGonder(adim: Long?, uykuDakika: Long?) {
        val json = JSONObject().apply {
            put("tip", "saglik_ozeti")
            put("adim", adim ?: JSONObject.NULL)
            put("uyku_dk", uykuDakika ?: JSONObject.NULL)
        }
        veriGonder(json)
    }

    fun havaDurumuGonder(sicaklik: Int, aciklama: String) {
        val json = JSONObject().apply {
            put("tip", "hava_durumu")
            put("sicaklik", sicaklik)
            put("aciklama", aciklama)
        }
        veriGonder(json)
    }

    private fun veriGonder(json: JSONObject) {
        val mevcutSoket = soket ?: return
        kapsam.launch {
            try {
                val satir = json.toString() + "\n"
                mevcutSoket.outputStream.write(satir.toByteArray())
                mevcutSoket.outputStream.flush()
            } catch (e: IOException) {
                baglandiMi = false
                durum = "Gönderim başarısız, bağlantı koptu"
            }
        }
    }

    fun baglantiyiKes() {
        baglandiMi = false
        dinlemeIsi?.cancel()
        try {
            soket?.close()
        } catch (e: IOException) {
            // yok say
        }
        soket = null
        durum = "Bağlı değil"
    }
}

object ApplicationContextTutucu {
    var context: Context? = null
}
