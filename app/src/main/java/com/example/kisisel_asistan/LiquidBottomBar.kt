package com.example.kisisel_asistan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class NavTab(val icon: ImageVector, val label: String)

val NAV_TABS = listOf(
    NavTab(Icons.Filled.Home, "Ana Sayfa"),
    NavTab(Icons.Filled.ChatBubble, "Sohbet"),
    NavTab(Icons.Filled.Favorite, "Sağlık"),
    NavTab(Icons.Filled.Search, "Ara"),
    NavTab(Icons.Filled.Settings, "Ayarlar"),
)

@Composable
fun LiquidBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val density = LocalDensity.current
    val baseSizePx = with(density) { 48.dp.toPx() }

    var barWidthPx by remember { mutableStateOf(0f) }
    val tabWidthPx = if (NAV_TABS.isNotEmpty() && barWidthPx > 0f) barWidthPx / NAV_TABS.size else 0f

    val pillLeft = remember { Animatable(0f) }
    val pillWidth = remember { Animatable(baseSizePx) }
    val pillScaleY = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(selected, barWidthPx) {
        if (tabWidthPx <= 0f) return@LaunchedEffect
        val targetLeft = tabWidthPx * selected + tabWidthPx / 2f - baseSizePx / 2f

        if (!initialized) {
            pillLeft.snapTo(targetLeft)
            pillWidth.snapTo(baseSizePx)
            initialized = true
            return@LaunchedEffect
        }

        val curLeft = pillLeft.value
        val stretchLeft = minOf(curLeft, targetLeft)
        val stretchRight = maxOf(curLeft, targetLeft) + baseSizePx
        val stretchWidth = stretchRight - stretchLeft

        launch { pillLeft.animateTo(stretchLeft, tween(200, easing = FastOutSlowInEasing)) }
        launch { pillWidth.animateTo(stretchWidth, tween(200, easing = FastOutSlowInEasing)) }
        pillScaleY.animateTo(0.72f, tween(200, easing = LinearOutSlowInEasing))

        val settle = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        launch { pillLeft.animateTo(targetLeft, settle) }
        launch { pillWidth.animateTo(baseSizePx, settle) }
        pillScaleY.animateTo(1f, settle)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SamanthaTheme.bar)
            .padding(vertical = 10.dp)
            .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(48.dp)
                .width(with(density) { pillWidth.value.toDp() })
                .offset(x = with(density) { pillLeft.value.toDp() })
                .graphicsLayer { scaleY = pillScaleY.value; transformOrigin = TransformOrigin.Center }
                .background(SamanthaTheme.pill, RoundedCornerShape(24.dp))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            NAV_TABS.forEachIndexed { index, tab ->
                val isActive = index == selected
                val iconScale by animateFloatAsState(
                    targetValue = if (isActive) 1.12f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label = "icon-scale-$index"
                )
                val tint by animateColorAsState(
                    targetValue = if (isActive) SamanthaTheme.accent else SamanthaTheme.muted,
                    label = "icon-tint-$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                    )
                }
            }
        }
    }
}
