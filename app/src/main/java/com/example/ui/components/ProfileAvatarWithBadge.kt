package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudihubViewModel

@Composable
fun ProfileAvatarWithBadge(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
    badgeSize: Dp = 16.dp,
    borderWidth: Dp = 1.5.dp,
    onClick: (() -> Unit)? = null
) {
    val avatarUrl = if (viewModel.userProfileAvatar.isNotEmpty()) {
        viewModel.userProfileAvatar
    } else if (viewModel.isGoogleSignedIn && viewModel.signedInUserPhoto.isNotEmpty()) {
        viewModel.signedInUserPhoto
    } else {
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
    }

    val activeBadge = viewModel.currentActiveBadge
    val hasBadge = activeBadge.imageUrl.isNotEmpty()

    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "avatarGlowRotation"
    )

    Box(
        modifier = modifier
            .size(avatarSize)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (hasBadge) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(glowRotation)
            ) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(activeBadge.colorHex).copy(alpha = 0.15f),
                            Color(activeBadge.colorHex),
                            Color(activeBadge.colorHex).copy(alpha = 0.15f)
                        )
                    ),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Image(
            painter = rememberAsyncImagePainter(avatarUrl),
            contentDescription = "Profile Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(borderWidth, Color.White, CircleShape)
        )

        if (activeBadge.imageUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = (badgeSize.value * 0.1f).dp, y = (badgeSize.value * 0.1f).dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(activeBadge.colorHex), CircleShape)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(activeBadge.imageUrl),
                    contentDescription = activeBadge.levelName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
