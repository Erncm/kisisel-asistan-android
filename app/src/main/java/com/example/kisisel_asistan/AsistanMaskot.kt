package com.example.kisisel_asistan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class AsistanModu(val renk: Color) {
    SAKIN(Color(0xFF38BDF8)),
    MUTLU(Color(0xFF22C55E)),
    SASKIN(Color(0xFFEAB308)),
    UTANGAC(Color(0xFFEC4899))
}

@Composable
fun AsistanMaskot(
    modifier: Modifier = Modifier,
    mod: AsistanModu = AsistanModu.SAKIN,
    konusuyor: Boolean = false,
    mesaj: String? = null
) {
    var kirpiyor by remember { mutableStateOf(false) }
    var bakisX by remember { mutableStateOf(0f) }
    var bakisY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 5000))
            kirpiyor = true
            delay(130)
            kirpiyor = false
        }
    }

    val nefesGecisi = rememberInfiniteTransition(label = "nefes")
    val olcek by nefesGecisi.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "olcek"
    )

    val agizGenislik by animateFloatAsState(
        targetValue = if (konusuyor) 34f else 20f,
        animationSpec = tween(200),
        label = "agiz"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = mesaj != null) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = mesaj ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(olcek)
                .clip(RoundedCornerShape(48.dp))
                .background(Color(0xFF1B1B3A))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val merkezX = size.width / 2f
                        val merkezY = size.height / 2f
                        bakisX = ((change.position.x - merkezX) / merkezX).coerceIn(-1f, 1f)
                        bakisY = ((change.position.y - merkezY) / merkezY).coerceIn(-1f, 1f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Goz(kirpiyor, bakisX, bakisY, mod.renk, Modifier.padding(end = 16.dp))
                    Goz(kirpiyor, bakisX, bakisY, mod.renk, Modifier.offset(x = 48.dp))
                }
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .size(width = agizGenislik.dp, height = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(mod.renk)
                )
            }
        }
    }
}

@Composable
private fun Goz(kirpiyor: Boolean, bakisX: Float, bakisY: Float, renk: Color, modifier: Modifier = Modifier) {
    val yukseklik by animateFloatAsState(if (kirpiyor) 6f else 36f, tween(120), label = "gozYuksek")
    Box(
        modifier = modifier
            .size(width = 26.dp, height = yukseklik.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (!kirpiyor) {
            Box(
                modifier = Modifier
                    .offset(x = (bakisX * 5).dp, y = (bakisY * 5).dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(renk)
            )
        }
    }
}
