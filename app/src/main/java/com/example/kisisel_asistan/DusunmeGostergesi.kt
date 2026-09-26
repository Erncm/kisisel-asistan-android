package com.example.kisisel_asistan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Amber500 = Color(0xFFF59E0B)
private val Amber400 = Color(0xFFFBBF24)
private val GrayText = Color(0xFFA3A3A3)
private val WhiteText = Color(0xFFFFFFFF)

/**
 * "Asistan bir şey yapıyor" göstergesi. Sabit/döngüsel kurgusal mesaj listesi YOK.
 * Gösterdiği metin çağıran tarafın o an yürüttüğü GERÇEK işlemden geliyor
 * (örn. "Gemini'ye soruluyor", "Yerel model yükleniyor"). Sadece `visible = true`
 * iken, yani gerçekten bir işlem sürerken görünür.
 */
@Composable
fun AsistanDusunuyorGostergesi(visible: Boolean, text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible) {
        DusunmeIcerik(text = text, modifier = modifier)
    }
}

@Composable
private fun DusunmeIcerik(text: String, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "asistan-dusunme")

    val breatheEasing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
    val breatheScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(2250, easing = breatheEasing), RepeatMode.Reverse),
        label = "breathe-scale"
    )
    val shimmerOffset by infinite.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer-move"
    )
    val textShimmer by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "text-shimmer"
    )

    val kapsulRengi = lerp(Color(0xFF1E1830), SamanthaTheme.accent, 0.18f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer { scaleX = breatheScale; scaleY = breatheScale }
            .animateContentSize(animationSpec = tween(450, easing = FastOutSlowInEasing))
            .clip(RoundedCornerShape(20.dp))
            .background(kapsulRengi)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ShimmerBar(offsetFraction = shimmerOffset)
        Box(Modifier.width(12.dp))
        ShimmerText(text = text, shimmerProgress = textShimmer)
    }
}

@Composable
private fun ShimmerBar(offsetFraction: Float) {
    val barWidth = 24.dp
    val beamWidth = barWidth * 0.6f
    Box(
        modifier = Modifier
            .width(barWidth)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Amber500.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(beamWidth)
                .offset(x = barWidth * offsetFraction)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, Amber500, Amber400, Amber500, Color.Transparent))
                )
        )
    }
}

@Composable
private fun ShimmerText(text: String, shimmerProgress: Float) {
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(GrayText, WhiteText, GrayText),
        start = Offset(x = shimmerProgress * 300f - 150f, y = 0f),
        end = Offset(x = shimmerProgress * 300f + 150f, y = 0f)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = TextStyle(fontSize = 14.sp, letterSpacing = 0.2.sp, brush = shimmerBrush))
        ThinkingDots()
    }
}

@Composable
private fun ThinkingDots() {
    val infinite = rememberInfiniteTransition(label = "dots")
    val dot1 by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse, StartOffset(0)),
        label = "dot1"
    )
    val dot2 by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse, StartOffset(200)),
        label = "dot2"
    )
    val dot3 by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse, StartOffset(400)),
        label = "dot3"
    )
    Text(".", color = WhiteText.copy(alpha = dot1), fontSize = 14.sp)
    Text(".", color = WhiteText.copy(alpha = dot2), fontSize = 14.sp)
    Text(".", color = WhiteText.copy(alpha = dot3), fontSize = 14.sp)
}
