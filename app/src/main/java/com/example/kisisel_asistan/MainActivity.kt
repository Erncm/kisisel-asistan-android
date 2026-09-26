package com.example.kisisel_asistan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.kisisel_asistan.ui.theme.KisiselasistanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TTSYoneticisi.baslat(this)
        SohbetDurumu.baslat(this)
        HafizaDeposu.baslat(this)
        UygulamaKatalogu.yukle(this)
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

    override fun onDestroy() {
        TTSYoneticisi.kapat()
        super.onDestroy()
    }
}
