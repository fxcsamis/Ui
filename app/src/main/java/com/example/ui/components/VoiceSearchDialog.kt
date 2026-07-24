package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.CloudihubViewModel
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceSearchDialog(
    viewModel: CloudihubViewModel
) {
    // Keep internal visible states to support smooth exit transitions
    var animateShow by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    // Synchronize with ViewModel's state
    LaunchedEffect(viewModel.showVoiceDialog) {
        if (viewModel.showVoiceDialog) {
            animateShow = true
            isVisible = true
        } else {
            isVisible = false
        }
    }

    // When exit animation finishes, hide the dialog from the composition tree
    LaunchedEffect(isVisible) {
        if (!isVisible && animateShow) {
            delay(220) // Let exit animation finish
            animateShow = false
        }
    }

    val scope = rememberCoroutineScope()
    val dismissWithAnimation = {
        scope.launch {
            isVisible = false
            delay(220)
            viewModel.stopVoiceSearch()
        }
    }

    if (animateShow) {
        val infiniteTransition = rememberInfiniteTransition(label = "CrystalSphereTransition")

        // Slow 3D rotation angle
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(15000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Slow3DRotation"
        )

        // Pulsing animations for custom audio wave visualizer
        val pulse1 by infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Pulse1"
        )

        val pulse2 by infiniteTransition.animateFloat(
            initialValue = 1.15f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Pulse2"
        )

        val wavePhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WavePhase"
        )

        Dialog(
            onDismissRequest = { dismissWithAnimation() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val backdropAlpha by animateFloatAsState(
                targetValue = if (isVisible) 0.55f else 0f,
                animationSpec = tween(durationMillis = 200, easing = EaseOutQuad),
                label = "VoiceBackdropAlpha"
            )

            val scale by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0.82f,
                animationSpec = spring(
                    dampingRatio = 0.72f, // smooth, bouncy iOS feel
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "VoiceContentScale"
            )

            val alpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "VoiceContentAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = backdropAlpha))
                    }
                    .clickable { dismissWithAnimation() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .graphicsLayer(alpha = alpha)
                        .clickable(enabled = false) {} // Prevent click-through closing
                        .padding(24.dp)
                        .testTag("voice_dialog_card"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top row with clean glass Close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { dismissWithAnimation() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Central high-fidelity 3D audio-wave visualizer sphere
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp) // Beautiful centered compact size
                                .scale(0.85f)  // Constantly slightly small, no more pulsing size changes
                                .graphicsLayer(
                                    rotationY = rotationAngle,
                                    cameraDistance = 12f
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val maxRadius = size.width * 0.42f

                                // 1. Draw glowing background radial gradient
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF0284C7).copy(alpha = 0.35f),
                                            Color(0xFF38BDF8).copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX, centerY),
                                        radius = maxRadius * 1.4f
                                    )
                                )

                                // 2. Draw outer pulsing rings with glowing strokes
                                drawCircle(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    radius = maxRadius * pulse1,
                                    style = Stroke(width = 2.dp.toPx()),
                                    center = Offset(centerX, centerY)
                                )

                                drawCircle(
                                    color = Color(0xFF0284C7).copy(alpha = 0.5f),
                                    radius = maxRadius * 0.75f * pulse2,
                                    style = Stroke(width = 1.5.dp.toPx()),
                                    center = Offset(centerX, centerY)
                                )

                                // 3. Draw central core sphere with linear metallic gradient
                                drawCircle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF0369A1), Color(0xFF38BDF8))
                                    ),
                                    radius = maxRadius * 0.42f,
                                    center = Offset(centerX, centerY)
                                )

                                // 4. Draw vertical audio wave equalizer bars in the core
                                val barWidth = 4.5.dp.toPx()
                                val barSpacing = 6.dp.toPx()
                                val numBars = 5
                                val totalBarsWidth = (numBars * barWidth) + ((numBars - 1) * barSpacing)
                                val startX = centerX - (totalBarsWidth / 2f)

                                for (i in 0 until numBars) {
                                    val phaseOffset = i * 0.9f
                                    val amplitudeMultiplier = when (i) {
                                        0, 4 -> 0.45f
                                        1, 3 -> 0.75f
                                        else -> 1.0f
                                    }
                                    val barHeight = (maxRadius * 0.55f) * (0.25f + 0.75f * sin(wavePhase + phaseOffset)) * amplitudeMultiplier
                                    val barX = startX + i * (barWidth + barSpacing)

                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(barX, centerY - (barHeight / 2f)),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // Bottom text instructions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.voiceMessage,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Speak clearly to search documents, nodes or music",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
