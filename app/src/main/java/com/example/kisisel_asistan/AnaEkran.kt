package com.example.kisisel_asistan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun AnaEkran() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var healthConnectMevcut by remember { mutableStateOf(false) }
    var izinVerildi by remember { mutableStateOf(false) }
    var bugunkuAdimSayisi by remember { mutableStateOf<Long?>(null) }
    var yukleniyor by remember { mutableStateOf(false) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    val healthConnectClient = remember {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            healthConnectMevcut = true
            HealthConnectClient.getOrCreate(context)
        } else {
            healthConnectMevcut = false
            null
        }
    }

    // Gerçek Adım Verisini Okuma Fonksiyonu
    fun adimlariOku() {
        if (healthConnectClient == null) return
        yukleniyor = true
        scope.launch {
            try {
                val now = Instant.now()
                val startOfDay = now.atZone(ZoneId.systemDefault())
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant()

                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )

                val toplamAdim = response.records.sumOf { it.count }
                bugunkuAdimSayisi = toplamAdim
            } catch (e: Exception) {
                e.printStackTrace()
                bugunkuAdimSayisi = 0
            } finally {
                yukleniyor = false
            }
        }
    }

    val requestPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        izinVerildi = granted.containsAll(permissions)
        if (izinVerildi) {
            adimlariOku()
        }
    }

    LaunchedEffect(Unit) {
        if (healthConnectClient != null) {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            izinVerildi = granted.containsAll(permissions)
            if (izinVerildi) {
                adimlariOku()
            }
        }
    }

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

            Spacer(modifier = Modifier.height(32.dp))

            if (!healthConnectMevcut) {
                Text(text = "Health Connect bu cihazda desteklenmiyor.")
            } else if (!izinVerildi) {
                Button(onClick = { requestPermissions.launch(permissions) }) {
                    Text(text = "Adım Verisi İznini Ver")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bugünkü Adım Sayısı",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (yukleniyor) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                text = "${bugunkuAdimSayisi ?: 0}",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { adimlariOku() }) {
                            Text(text = "Verileri Yenile")
                        }
                    }
                }
            }
        }
    }
}
