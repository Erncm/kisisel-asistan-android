package com.example.kisisel_asistan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceAssistantBar(
    modifier: Modifier = Modifier,
    audioVolume: Float = 0.3f,
    onSendMessage: (String) -> Unit = {},
    onAttachClick: () -> Unit = {},
    onOrbLongPress: () -> Unit = {}
) {
    var isTextMode by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "OrbAnimation")
    val step by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbStep"
    )

    val animatedVolume by animateFloatAsState(
        targetValue = audioVolume,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "VolumeSpring"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = !isTextMode,
            enter = fadeIn() + scaleIn(initialScale = 0.2f),
            exit = fadeOut() + scaleOut(targetScale = 0.2f)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isTextMode = true },
                        onLongClick = { onOrbLongPress() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((110 * (1f + animatedVolume * 0.4f)).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF8C00).copy(alpha = 0.7f),
                                    Color(0xFFF59E0B).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .blur(20.dp)
                )

                Canvas(
                    modifier = Modifier
                        .size((90 * (1f + animatedVolume * 0.2f)).dp)
                        .blur(6.dp)
                ) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val radius = size.minDimension / 2

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFEB3B),
                                Color(0xFFFF8C00),
                                Color(0xFFF57C00).copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(cx, cy)
                    )

                    for (i in 0..2) {
                        val angle = step + (i * 2.094f)
                        val waveX = cx + (kotlin.math.cos(angle.toDouble()) * (18 + animatedVolume * 20)).toFloat()
                        val waveY = cy + (kotlin.math.sin((angle * 1.3).toDouble()) * (18 + animatedVolume * 20)).toFloat()
                        val waveRadius = radius * 0.65f + (kotlin.math.sin((step * 1.2 + i).toDouble()) * 10).toFloat()

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (i == 0) Color.White.copy(alpha = 0.8f) else Color(0xFFFFB74D).copy(alpha = 0.7f),
                                    Color(0xFFFF8C00).copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                center = Offset(waveX, waveY),
                                radius = waveRadius
                            ),
                            radius = waveRadius,
                            center = Offset(waveX, waveY)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isTextMode,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFF59E0B).copy(alpha = 0.2f),
                                Color(0xFFFF8C00),
                                Color(0xFFF59E0B),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0xFF0F0F12).copy(alpha = 0.95f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onAttachClick) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Dosya ekle",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Color(0xFFFF8C00)),
                        decorationBox = { innerTextField ->
                            Box {
                                if (textInput.isEmpty()) {
                                    Text(
                                        text = "Bir şeyler yazın...",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    if (textInput.isNotBlank()) {
                                        onSendMessage(textInput)
                                        textInput = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Gönder",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    isTextMode = false
                                    textInput = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
