package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * A custom Shape that outlines a beautiful, fluffy cloud using cubic Bezier curves.
 */
class CloudShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = createCloudPath(size.width, size.height)
        return Outline.Generic(path)
    }
}

/**
 * Creates an organic-looking cloud path that fits inside the given width and height.
 */
fun createCloudPath(w: Float, h: Float): Path {
    return Path().apply {
        // Start near bottom-left
        moveTo(w * 0.25f, h * 0.75f)
        
        // Left curve
        cubicTo(
            w * 0.05f, h * 0.75f,
            w * 0.05f, h * 0.45f,
            w * 0.22f, h * 0.45f
        )
        
        // Top-left curve
        cubicTo(
            w * 0.18f, h * 0.20f,
            w * 0.42f, h * 0.15f,
            w * 0.48f, h * 0.32f
        )
        
        // Top-right curve
        cubicTo(
            w * 0.55f, h * 0.12f,
            w * 0.82f, h * 0.18f,
            w * 0.80f, h * 0.40f
        )
        
        // Right curve
        cubicTo(
            w * 0.95f, h * 0.40f,
            w * 0.96f, h * 0.68f,
            w * 0.82f, h * 0.72f
        )
        
        // Bottom curve (fluffy bottom)
        cubicTo(
            w * 0.70f, h * 0.82f,
            w * 0.55f, h * 0.84f,
            w * 0.45f, h * 0.78f
        )
        cubicTo(
            w * 0.38f, h * 0.82f,
            w * 0.30f, h * 0.80f,
            w * 0.25f, h * 0.75f
        )
        
        close()
    }
}

/**
 * Animated drifting cloud layers for the app's background.
 * Creates an elegant, modern, layered sky background with slow-drifting smooth animations.
 */
@Composable
fun CloudSkyBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "CloudDrift")
    
    // Smooth infinite floating offsets for depth
    val drift1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DriftLayer1"
    )

    val drift2 by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = -180f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DriftLayer2"
    )

    val drift3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DriftLayer3"
    )

    // Soft floating bobbing up/down
    val bobbing by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = SineToSineEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BobbingY"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw Sky sky-blue gradient base
        drawRect(
            color = Color(0xFFF1F5F9) // Sweet off-white sky tone
        )

        // Drifting Background Clouds Layer 1 (Very light translucent blue)
        drawCloudSet(
            offsetX = drift1,
            offsetY = height * 0.15f + bobbing * 0.5f,
            scale = 1.3f,
            alpha = 0.25f,
            color = Color(0xFFDBEAFE), // Light blue-sky
            width = width,
            height = height
        )

        // Drifting Background Clouds Layer 2 (Light grey-white)
        drawCloudSet(
            offsetX = drift2,
            offsetY = height * 0.45f - bobbing,
            scale = 1.6f,
            alpha = 0.35f,
            color = Color(0xFFF8FAFC), // Slate cloud white
            width = width,
            height = height
        )

        // Drifting Background Clouds Layer 3 (Soft bright cloud white)
        drawCloudSet(
            offsetX = drift3,
            offsetY = height * 0.70f + bobbing * 1.2f,
            scale = 1.1f,
            alpha = 0.45f,
            color = Color(0xFFFFFFFF), // Crisp pure cloud white
            width = width,
            height = height
        )
    }
}

private fun DrawScope.drawCloudSet(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    alpha: Float,
    color: Color,
    width: Float,
    height: Float
) {
    // We draw multiple clouds in a row to tile them infinitely as they drift
    val cloudWidth = 240f * scale
    val cloudHeight = 150f * scale

    val startX = (offsetX % (width + cloudWidth)) - cloudWidth

    for (i in -1..2) {
        val x = startX + i * (width * 0.6f + cloudWidth * 0.5f)
        val path = createCloudPath(cloudWidth, cloudHeight)
        
        drawContext.canvas.save()
        drawContext.canvas.translate(x, offsetY)
        
        drawPath(
            path = path,
            color = color,
            alpha = alpha
        )
        
        drawContext.canvas.restore()
    }
}

// Custom easing for smooth fluid motion
private val SineToSineEasing = Easing { fraction ->
    val radians = fraction * Math.PI.toFloat()
    (1f - kotlin.math.cos(radians)) / 2f
}
