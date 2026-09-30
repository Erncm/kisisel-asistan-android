package com.example.kisisel_asistan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class AsistanOrbActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TTSYoneticisi.baslat(this)
        SohbetDurumu.baslat(this)
        HafizaDeposu.baslat(this)
        UygulamaKatalogu.yukle(this)

        setContent {
            var cevapMetni by remember { mutableStateOf<String?>(null) }
            var isleniyor by remember { mutableStateOf(false) }
            val kapsam = rememberCoroutineScope()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xF0060608)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isleniyor) {
                        AsistanDusunuyorGostergesi(
                            visible = true,
                            text = "Düşünüyor",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    cevapMetni?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    VoiceAssistantBar(
                        modifier = Modifier.padding(24.dp),
                        audioVolume = if (isleniyor) 0.6f else 0.15f,
                        onSendMessage = { metin ->
                            isleniyor = true
                            cevapMetni = null
                            kapsam.launch {
                                val cevap = mesajiIsleVeCevapAl(applicationContext, metin)
                                cevapMetni = cevap
                                isleniyor = false
                                TTSYoneticisi.oku(cevap)
                            }
                        },
                        onAttachClick = {},
                        onOrbLongPress = { sesTanimaBaslatBasit(applicationContext) { metin ->
                            isleniyor = true
                            cevapMetni = null
                            kapsam.launch {
                                val cevap = mesajiIsleVeCevapAl(applicationContext, metin)
                                cevapMetni = cevap
                                isleniyor = false
                                TTSYoneticisi.oku(cevap)
                            }
                        } }
                    )
                }
            }
        }
    }
}

private fun sesTanimaBaslatBasit(context: android.content.Context, onSonuc: (String) -> Unit) {
    SesliGirisYoneticisi.dinlemeyeBasla(
        context = context,
        onSonuc = onSonuc,
        onDurum = {},
        onBitti = {}
    )
}
