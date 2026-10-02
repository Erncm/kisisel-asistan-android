package com.example.kisisel_asistan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.kisisel_asistan.ui.theme.KisiselasistanTheme

object DisaridanGelenMetin {
    val bekleyenMetin = mutableStateOf<String?>(null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TTSYoneticisi.baslat(this)
        SohbetDurumu.baslat(this)
        HafizaDeposu.baslat(this)
        UygulamaKatalogu.yukle(this)
        ApplicationContextTutucu.context = applicationContext
        disaridanGelenMetniIsle(intent)
        setContent {
            KisiselasistanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SamanthaTheme.bg,
                    contentColor = SamanthaTheme.ink
                ) {
                    AnaEkranIskelet()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        disaridanGelenMetniIsle(intent)
    }

    private fun disaridanGelenMetniIsle(intent: Intent?) {
        if (intent?.action == Intent.ACTION_PROCESS_TEXT) {
            val secilenMetin = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            if (!secilenMetin.isNullOrBlank()) {
                DisaridanGelenMetin.bekleyenMetin.value = "Şunu özetle: \"$secilenMetin\""
            }
        }
    }

    override fun onDestroy() {
        TTSYoneticisi.kapat()
        super.onDestroy()
    }
}
