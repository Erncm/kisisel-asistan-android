package com.example.kisisel_asistan

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.kisisel_asistan.ui.theme.KisiselasistanTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    val sonOkunanNfcId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            KisiselasistanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnaEkranIskelet()
                }
            }
        }
        nfcIntentiIsle(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                android.app.PendingIntent.FLAG_MUTABLE
            )
            it.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcIntentiIsle(intent)
    }

    private fun nfcIntentiIsle(intent: Intent?) {
        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val id = tag.id.joinToString("") { "%02X".format(it) }
            sonOkunanNfcId.value = id
        }
    }
}
