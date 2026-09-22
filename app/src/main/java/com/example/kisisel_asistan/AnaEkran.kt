package com.example.kisisel_asistan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnaEkran() {
    var odakModuAktif by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Kişisel Asistan",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Odak Modu: ")
                Switch(
                    checked = odakModuAktif,
                    onCheckedChange = { odakModuAktif = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (odakModuAktif) {
                Text(text = "Odak modu şu anda aktif.")
            } else {
                Text(text = "Odak modu kapalı.")
            }
        }
    }
}
