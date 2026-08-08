package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium CloudeHub Splash Screen with multi-phase animation sequence:
 * 1. Deep midnight gradient canvas with ambient floating light orbs.
 * 2. Official CloudeHub logo entrance with glowing halo and scale pop.
 * 3. Shimmering typography reveal ("CloudeHub").
 * 4. Orbital feature icons (Cloud, Video, Music, AI, Downloads) floating into position with pulse waves.
 * 5. Smooth finish transition into main app experience.
 */
@Composable
fun CloudeHubSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation States
    var animationPhase by remember { mutableStateOf(0) } // 0: Start, 1: Logo, 2: Text, 3: Orbit, 4: Ready

    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(30f) }

    val orbitScale = remember { Animatable(0f) }
    val orbitAlpha = remember { Animatable(0f) }

    // Infinite ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")
    
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HaloRotation"
    )

    // Sequence Controller
    LaunchedEffect(Unit) {
        // Phase 1: Logo Entrance
        delay(150)
        animationPhase = 1
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Phase 2: Text & Subtitle Reveal
        delay(500)
        animationPhase = 2
        launch {
            textAlpha.animateTo(1f, animationSpec = tween(500))
        }
        launch {
            textOffsetY.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        }

        // Phase 3: Orbital Feature Badges
        delay(400)
        animationPhase = 3
        launch {
            orbitAlpha.animateTo(1f, animationSpec = tween(500))
        }
        launch {
            orbitScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Phase 4: Hold & Finish
        delay(1000)
        animationPhase = 4
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC), // Pure Crisp Light Slate
                        Color(0xFFE0F2FE), // Refreshing Sky Blue Tint
                        Color(0xFFF1F5F9)  // Soft Warm Neutral Light
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Quick tap to skip splash screen
                onSplashFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // --- 1. AMBIENT GLOWING ORBS CANVAS ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left cyan ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x350284C7),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.2f, height * 0.25f),
                    radius = width * 0.7f
                ),
                center = Offset(width * 0.2f, height * 0.25f),
                radius = width * 0.7f
            )

            // Bottom-right purple ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x25C084FC),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.8f, height * 0.75f),
                    radius = width * 0.8f
                ),
                center = Offset(width * 0.8f, height * 0.75f),
                radius = width * 0.8f
            )
        }

        // --- 2. MAIN LOGO & BRANDING CONTENT ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Orbital Feature Badges Floating Around Logo (Outlined Line Icons)
                val badges = listOf(
                    OrbitBadgeData("Cloud Engine", Icons.Outlined.CloudQueue, Color(0xFF0284C7), -60f, 105.dp),
                    OrbitBadgeData("4K Video", Icons.Outlined.PlayCircleOutline, Color(0xFFE11D48), 20f, 110.dp),
                    OrbitBadgeData("Hi-Fi Music", Icons.Outlined.MusicNote, Color(0xFF9333EA), 110f, 105.dp),
                    OrbitBadgeData("Fast Engine", Icons.Outlined.Bolt, Color(0xFFD97706), 180f, 110.dp),
                    OrbitBadgeData("Smart Vault", Icons.Outlined.Lock, Color(0xFF059669), 240f, 105.dp)
                )

                badges.forEach { badge ->
                    val angleRad = Math.toRadians((badge.angleDeg + haloRotation * 0.2f).toDouble())
                    val badgeOffsetX = (badge.radius.value * cos(angleRad)).dp
                    val badgeOffsetY = (badge.radius.value * sin(angleRad)).dp

                    Box(
                        modifier = Modifier
                            .offset(x = badgeOffsetX, y = badgeOffsetY)
                            .scale(orbitScale.value)
                            .alpha(orbitAlpha.value)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.5.dp, badge.color.copy(alpha = 0.8f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(badge.color.copy(alpha = 0.12f), Color.White)
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = badge.icon,
                                    contentDescription = badge.label,
                                    tint = badge.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // --- OFFICIAL CLOUDEHUB LOGO (CLEAN WITHOUT CIRCLES OR HALO RINGS) ---
                Image(
                    painter = rememberAsyncImagePainter(com.example.R.drawable.cloudihub_logo_1784004021392),
                    contentDescription = "Official CloudeHub Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(130.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. TYPOGRAPHY & BRAND NAME ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffsetY.value.dp)
                    .alpha(textAlpha.value)
            ) {
                // Title
                Text(
                    text = "CloudeHub",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.sp,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline with Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFFC084FC))
                        )
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next-Gen Cloud & Media Ecosystem",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- 4. BOTTOM LOADING INDICATOR ---
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .alpha(textAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulsingDot(delayMs = 0)
                    PulsingDot(delayMs = 150)
                    PulsingDot(delayMs = 300)
                }
            }
        }

        // --- 5. FOOTER POWERED BY TEXT ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .alpha(textAlpha.value)
        ) {
            Text(
                text = "POWERED BY CLOUDEHUB AI v3.0",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

private data class OrbitBadgeData(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val angleDeg: Float,
    val radius: androidx.compose.ui.unit.Dp
)

@Composable
private fun PulsingDot(delayMs: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "DotPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotAlpha"
    )

    Box(
        modifier = Modifier
            .size(7.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(Color(0xFF38BDF8))
    )
}
