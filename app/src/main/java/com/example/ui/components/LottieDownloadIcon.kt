package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

const val LOTTIE_DOWNLOAD_URL = "https://lottie.host/1973d2b1-290a-446a-ac24-9c910a7a82c7/WQsuxbfZuM.lottie"

@Composable
fun LottieDownloadIcon(
    isDownloading: Boolean = false,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url(LOTTIE_DOWNLOAD_URL)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isDownloading,
        iterations = if (isDownloading) LottieConstants.IterateForever else 1,
        restartOnPlay = true
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { if (isDownloading) progress else 0f },
            modifier = Modifier.size(size)
        )
    }
}
