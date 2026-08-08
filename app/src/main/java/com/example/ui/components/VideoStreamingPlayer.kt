@file:kotlin.OptIn(ExperimentalSharedTransitionApi::class)
package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.ui.CloudVideo
import com.example.ui.CloudihubViewModel
import com.example.ui.DownloadStatus
import com.example.ui.screens.VideoCloudCard
import com.example.ui.screens.DownloadVideoBottomSheet
import com.example.ui.screens.ShareVideoBottomSheet
import com.example.ui.components.LocalSharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.AnimatedVisibilityScope
import kotlinx.coroutines.delay

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoStreamingPlayer(
    viewModel: CloudihubViewModel,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val video = viewModel.playingVideo ?: return
    val streamUrl = viewModel.activeStreamingUrl
    val extractorMsg = viewModel.extractorModeMsg
    val isExtracting = viewModel.isExtracting
    val downloads by viewModel.downloads.collectAsState()
    val isVideoDownloading = downloads.any { it.videoId == video.id && (it.status == com.example.ui.DownloadStatus.DOWNLOADING || it.status == com.example.ui.DownloadStatus.QUEUED) }

    var selectedVideoToDownload by remember { mutableStateOf<CloudVideo?>(null) }
    var selectedVideoForMoreOptions by remember { mutableStateOf<CloudVideo?>(null) }

    var isPlayerExpanded = viewModel.isVideoPlayerExpanded
    var isPlaying by remember { mutableStateOf(true) }
    var isAudioMode by remember { mutableStateOf(false) }
    var playbackErrorMsg by remember { mutableStateOf<String?>(null) }
    var showControlsOverlay by remember { mutableStateOf(true) }
    var hasCompletedPopupFirstLoop by remember(video.id) { mutableStateOf(false) }

    // Custom Interactive States
    var isLiked by remember(video.id) { mutableStateOf(false) }
    var isDisliked by remember(video.id) { mutableStateOf(false) }
    var isSubscribed by remember(video.id) { mutableStateOf(false) }
    var isWatchLater by remember(video.id) { mutableStateOf(false) }
    var isSaveAnimating by remember(video.id) { mutableStateOf(false) }
    var isProfileRingActive by remember(video.id) { mutableStateOf(false) }
    var likeCount by remember(video.id) { mutableStateOf((800 + Math.random() * 500).toInt()) }
    
    val comments = remember(video.id) {
        mutableStateListOf(
            "Wow! The Piped API-extraction system is incredibly fast.",
            "Visual resolution looks crisp and premium.",
            "Amazing video content. Love the UI update!",
            "Great playback experience without clutter."
        )
    }

    val context = LocalContext.current

    // Persistent ExoPlayer instance created once per streamUrl to support uninterrupted playback
    val exoPlayer = remember(streamUrl) {
        if (streamUrl.isEmpty()) null
        else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
                playWhenReady = isPlaying
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playbackErrorMsg = error.localizedMessage ?: "Playback stream interrupted"
                    }
                })
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        if (exoPlayer != null) {
            if (isPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        }
    }

    // Auto-hide controls overlay after 4 seconds if playing
    LaunchedEffect(showControlsOverlay, isPlaying) {
        if (showControlsOverlay && isPlaying) {
            delay(4000)
            showControlsOverlay = false
        }
    }

    // Timer timeline calculation
    var progressSec by remember(video.id) { mutableStateOf(0) }
    val totalSeconds = remember(video) {
        val parts = video.duration.split(":")
        if (parts.size == 2) {
            val mins = parts[0].toIntOrNull() ?: 0
            val secs = parts[1].toIntOrNull() ?: 0
            mins * 60 + secs
        } else {
            300
        }
    }

    LaunchedEffect(isPlaying, streamUrl) {
        if (isPlaying && streamUrl.isNotEmpty()) {
            while (progressSec < totalSeconds) {
                delay(1000)
                progressSec++
            }
        }
    }

    val progressPercent = if (totalSeconds > 0) progressSec.toFloat() / totalSeconds else 0f

    // Up Next Video List compilation
    val upNextList = remember(video.id, viewModel.relatedVideos, viewModel.videos) {
        val list = mutableListOf<CloudVideo>()
        list.addAll(viewModel.relatedVideos)
        val feedFallback = viewModel.videos.filter { it.id != video.id && list.none { r -> r.id == it.id } }
        list.addAll(feedFallback)
        if (list.isEmpty()) {
            list.addAll(viewModel.getLocalFallbackVideos().filter { it.id != video.id })
        }
        list
    }

    // Floating PIP drag & edge snap state
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val cardWidthPx = with(density) { 220.dp.toPx() }
    val cardHeightPx = with(density) { 135.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }

    // Left snap target X offset (relative to Alignment.BottomEnd with 16dp end padding)
    val leftSnapX = -(screenWidthPx - cardWidthPx - (marginPx * 2f))
    val minOffsetY = -(screenHeightPx - cardHeightPx - with(density) { 120.dp.toPx() })
    val maxOffsetY = with(density) { 30.dp.toPx() }

    val pipOffsetX = remember { Animatable(0f) }
    val pipOffsetY = remember { Animatable(0f) }
    val swipeY = remember { Animatable(0f) }

    val sharedTransitionScope = LocalSharedTransitionScope.current

    AnimatedContent(
        targetState = isPlayerExpanded,
        transitionSpec = {
            if (targetState) {
                (fadeIn(animationSpec = spring(stiffness = 380f)) +
                 scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f)) +
                 slideInVertically(initialOffsetY = { it / 3 }, animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = 380f)) +
                        scaleOut(targetScale = 0.85f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f))
                    )
            } else {
                (fadeIn(animationSpec = spring(stiffness = 380f)) +
                 scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f)) +
                 slideInVertically(initialOffsetY = { -it / 4 }, animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = 380f)) +
                        scaleOut(targetScale = 0.85f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f)) +
                        slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f))
                    )
            }
        },
        label = "OrbitalVideoPlayerTransition",
        modifier = modifier.fillMaxSize()
    ) { expanded ->
        if (!expanded) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ==========================================
                // MODULE 5: FLOATING PICTURE-IN-PICTURE MINI-PLAYER VIEW (Movable Widget)
                // ==========================================
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 90.dp)
                    .offset { IntOffset(pipOffsetX.value.roundToInt(), pipOffsetY.value.roundToInt()) }
                    .width(220.dp)
                    .height(135.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // Snap to closest side (Left or Right) quickly
                                val midPoint = leftSnapX / 2f
                                val targetX = if (pipOffsetX.value < midPoint) leftSnapX else 0f
                                coroutineScope.launch {
                                    pipOffsetX.animateTo(
                                        targetValue = targetX,
                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                    )
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newX = (pipOffsetX.value + dragAmount.x).coerceIn(leftSnapX, 0f)
                                    val newY = (pipOffsetY.value + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)
                                    pipOffsetX.snapTo(newX)
                                    pipOffsetY.snapTo(newY)
                                }
                            }
                        )
                    }
                    .clickable { viewModel.toggleVideoPlayerExpansion(true) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (streamUrl.isNotEmpty() || exoPlayer != null) {
                        ExoPlayerSurface(
                            exoPlayer = exoPlayer,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(video.imageUrl),
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Touch overlay & controls gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Top Right: Close (X) Button
                    IconButton(
                        onClick = { viewModel.stopVideo() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Mini Close",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Top Left: Fullscreen Expand Arrow
                    IconButton(
                        onClick = { viewModel.toggleVideoPlayerExpansion(true) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expand Player",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Center: Play / Pause Button
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Mini Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Bottom Bar: Video Title
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
        } else {
            val view = LocalView.current
            DisposableEffect(Unit) {
                val window = (view.context as? Activity)?.window
                val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
                insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController?.hide(WindowInsetsCompat.Type.statusBars())
                onDispose {
                    insetsController?.show(WindowInsetsCompat.Type.statusBars())
                }
            }

            // ==========================================
            // YOUTUBE-STYLE FULL PLAYER SCREEN (Edge-to-Edge)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val dragFrac = (swipeY.value / (screenHeightPx * 0.5f)).coerceIn(0f, 1f)
                        alpha = (1f - dragFrac * 1.2f).coerceIn(0f, 1f)
                    }
                    .background(Color.White)
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. TOP VIDEO PLAYER CONTAINER (YouTube 16:9 Aspect Ratio with Swipe-Down Gesture)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .offset { IntOffset(0, swipeY.value.roundToInt()) }
                            .graphicsLayer {
                                val dragFrac = (swipeY.value / (screenHeightPx * 0.5f)).coerceIn(0f, 1f)
                                scaleX = 1f - (dragFrac * 0.25f)
                                scaleY = 1f - (dragFrac * 0.25f)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (swipeY.value > with(density) { 120.dp.toPx() }) {
                                            viewModel.toggleVideoPlayerExpansion(false)
                                        }
                                        coroutineScope.launch {
                                            swipeY.animateTo(
                                                0f,
                                                spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)
                                            )
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (dragAmount.y > 0 || swipeY.value > 0) {
                                            change.consume()
                                            coroutineScope.launch {
                                                swipeY.snapTo((swipeY.value + dragAmount.y).coerceAtLeast(0f))
                                            }
                                        }
                                    }
                                )
                            }
                            .background(Color.Black)
                            .clickable { showControlsOverlay = !showControlsOverlay },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAudioMode) {
                            // Audio Mode View: Video Thumbnail + Dark Shadow Overlay + Center Lottie + Left Buttons
                            Image(
                                painter = rememberAsyncImagePainter(video.imageUrl),
                                contentDescription = video.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Dark Shadow Effect Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.75f),
                                                Color.Black.copy(alpha = 0.92f)
                                            )
                                        )
                                    )
                            )

                            // Center Lottie Animation
                            val audioLottieComp by rememberLottieComposition(
                                LottieCompositionSpec.Url(LOTTIE_AUDIO_3_URL)
                            )
                            if (audioLottieComp != null) {
                                val audioProgress by animateLottieCompositionAsState(
                                    composition = audioLottieComp,
                                    iterations = LottieConstants.IterateForever,
                                    isPlaying = isPlaying
                                )
                                LottieAnimation(
                                    composition = audioLottieComp,
                                    progress = { audioProgress },
                                    modifier = Modifier
                                        .size(150.dp)
                                        .align(Alignment.Center)
                                )
                            }

                            // Left Side Buttons with Dark Shadow Framing
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Button 1: Play as Video
                                Surface(
                                    onClick = {
                                        isAudioMode = false
                                        Toast.makeText(context, "Switched to Video Mode", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.82f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OndemandVideo,
                                            contentDescription = "Play as Video",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Play as Video",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Button 2: Play Next
                                Surface(
                                    onClick = {
                                        if (upNextList.isNotEmpty()) {
                                            viewModel.playVideo(upNextList.first())
                                            Toast.makeText(context, "Playing Next Video", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No next video in playlist", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.82f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Play Next",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Play Next",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else if (playbackErrorMsg != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Playback Error: $playbackErrorMsg",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        playbackErrorMsg = null
                                        viewModel.extractStreamAndPreparePlayer(video)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry Stream", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (isExtracting || streamUrl.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFF0284C7))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = extractorMsg,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            ExoPlayerSurface(
                                exoPlayer = exoPlayer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // VIDEO CONTROLS OVERLAY ON TOP OF PLAYER
                        if (showControlsOverlay) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            ) {
                                // Top bar overlay: Collapse button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleVideoPlayerExpansion(false) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Minimize Player",
                                            tint = Color.White
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = "1080p • HD",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Center overlay: Transport controls (Rewind, Play/Pause, Forward)
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { progressSec = (progressSec - 10).coerceAtLeast(0) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Rewind 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .border(1.5.dp, Color.White, CircleShape)
                                            .clickable { isPlaying = !isPlaying },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { progressSec = (progressSec + 10).coerceAtMost(totalSeconds) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Forward 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Bottom overlay: Integrated Seekbar & Duration
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${formatTime(progressSec)} / ${video.duration}",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable {
                                                    Toast.makeText(context, "Fullscreen Mode Toggled", Toast.LENGTH_SHORT).show()
                                                }
                                        )
                                    }
                                    Slider(
                                        value = progressPercent,
                                        onValueChange = { percent ->
                                            progressSec = (percent * totalSeconds).toInt()
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF0284C7),
                                            activeTrackColor = Color(0xFF0284C7),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. BELOW PLAYER SCROLLABLE FEED (Title, Channel, Action Pills, Up Next)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title & Subtitle Info (Views / Buffered Status as in Screenshot 2)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = video.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${video.views} • Buffered 7%",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Creator Channel Row + Upper Right Like/Dislike Counter
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Slim Creator Profile Pic & Name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .then(
                                                if (isProfileRingActive) {
                                                    Modifier
                                                        .border(
                                                            border = androidx.compose.foundation.BorderStroke(
                                                                2.dp,
                                                                Brush.sweepGradient(
                                                                    listOf(
                                                                        Color(0xFF0284C7),
                                                                        Color(0xFF38BDF8),
                                                                        Color(0xFF818CF8),
                                                                        Color(0xFF0284C7)
                                                                    )
                                                                )
                                                            ),
                                                            shape = CircleShape
                                                        )
                                                        .padding(2.dp)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(video.imageUrl),
                                            contentDescription = video.creator,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .border(0.5.dp, Color(0xFFCBD5E1), CircleShape)
                                        )

                                        // Small Star Badge on top of profile picture
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 2.dp, y = (-2).dp)
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(0.5.dp, Color(0xFFCBD5E1), CircleShape)
                                                .clickable {
                                                    isProfileRingActive = !isProfileRingActive
                                                    Toast.makeText(
                                                        context,
                                                        if (isProfileRingActive) "Profile Ring Highlighted!" else "Highlight Removed",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isProfileRingActive) Icons.Default.Star else Icons.Outlined.Star,
                                                contentDescription = "Star Highlight",
                                                tint = if (isProfileRingActive) Color(0xFFEAB308) else Color(0xFF64748B),
                                                modifier = Modifier.size(9.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            text = video.creator,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Acoustic Therapy",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Upper Right Like & Dislike Counter (As in Screenshot 1)
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                isLiked = !isLiked
                                                if (isLiked) {
                                                    likeCount++
                                                    if (isDisliked) isDisliked = false
                                                } else {
                                                    likeCount--
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                                contentDescription = "Like",
                                                tint = if (isLiked) Color(0xFF0284C7) else Color(0xFF475569),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$likeCount",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(12.dp)
                                                .background(Color(0xFFCBD5E1))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                isDisliked = !isDisliked
                                                if (isDisliked && isLiked) {
                                                    isLiked = false
                                                    likeCount--
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                                                contentDescription = "Dislike",
                                                tint = if (isDisliked) Color.Red else Color(0xFF475569),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Medium Size Action Buttons Row (Save, Play Audio, Popup, Share, Download)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // 1. Save (Continuously Animated)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isWatchLater = !isWatchLater
                                            Toast.makeText(
                                                context,
                                                if (isWatchLater) "Saved to Playlist!" else "Removed from Save",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.height(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val lottieSaveComp by rememberLottieComposition(
                                            LottieCompositionSpec.Url(LOTTIE_SAVE_URL)
                                        )
                                        if (lottieSaveComp != null) {
                                            var isSaveAnimPlaying by remember { mutableStateOf(true) }
                                            val saveProgress by animateLottieCompositionAsState(
                                                composition = lottieSaveComp,
                                                isPlaying = isSaveAnimPlaying,
                                                iterations = 1,
                                                restartOnPlay = true
                                            )
                                            LaunchedEffect(saveProgress) {
                                                if (isSaveAnimPlaying && saveProgress >= 0.98f) {
                                                    isSaveAnimPlaying = false
                                                    delay(1500L)
                                                    isSaveAnimPlaying = true
                                                }
                                            }
                                            LottieAnimation(
                                                composition = lottieSaveComp,
                                                progress = { saveProgress },
                                                modifier = Modifier.requiredSize(36.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isWatchLater) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                                contentDescription = "Save",
                                                tint = if (isWatchLater) Color(0xFF0284C7) else Color(0xFF334155),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Save",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isWatchLater) Color(0xFF0284C7) else Color(0xFF334155)
                                    )
                                }

                                // 2. Play Audio (Moved to Popup's previous position)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isAudioMode = !isAudioMode
                                            Toast.makeText(
                                                context,
                                                if (isAudioMode) "Background Audio Mode Active" else "Video Mode Active",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.height(22.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val lottieAudioBtnComp by rememberLottieComposition(
                                            LottieCompositionSpec.Url(LOTTIE_AUDIO_3_URL)
                                        )
                                        if (lottieAudioBtnComp != null) {
                                            val btnProgress by animateLottieCompositionAsState(
                                                composition = lottieAudioBtnComp,
                                                iterations = LottieConstants.IterateForever,
                                                isPlaying = isPlaying
                                            )
                                            LottieAnimation(
                                                composition = lottieAudioBtnComp,
                                                progress = { btnProgress },
                                                modifier = Modifier.requiredSize(63.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (isAudioMode) "Audio Active" else "Play Audio",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isAudioMode) Color(0xFF0284C7) else Color(0xFF334155)
                                    )
                                }

                                // 3. Popup (Floating Window - Moved to Play Audio's previous position)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isPlayerExpanded = false
                                            Toast.makeText(context, "Popup Player Floating Mode", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.height(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val lottiePopupComp by rememberLottieComposition(
                                            LottieCompositionSpec.Url(LOTTIE_POPUP_URL)
                                        )
                                        if (lottiePopupComp != null) {
                                            val popupProgress by animateLottieCompositionAsState(
                                                composition = lottiePopupComp,
                                                iterations = LottieConstants.IterateForever,
                                                clipSpec = if (hasCompletedPopupFirstLoop) LottieClipSpec.Progress(0.25f, 1f) else null,
                                                isPlaying = isPlaying
                                            )
                                            LaunchedEffect(popupProgress) {
                                                if (popupProgress >= 0.98f) {
                                                    hasCompletedPopupFirstLoop = true
                                                }
                                            }
                                            LottieAnimation(
                                                composition = lottiePopupComp,
                                                progress = { popupProgress },
                                                modifier = Modifier.requiredSize(52.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Popup",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }

                                // 4. Share (Animated Lottie)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Watch ${video.title} on Cloudihub Video Streamer!")
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share via"))
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.height(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val lottieShareComp by rememberLottieComposition(
                                            LottieCompositionSpec.Url(LOTTIE_SHARE_URL)
                                        )
                                        if (lottieShareComp != null) {
                                            val shareProgress by animateLottieCompositionAsState(
                                                composition = lottieShareComp,
                                                iterations = LottieConstants.IterateForever,
                                                isPlaying = true
                                            )
                                            LottieAnimation(
                                                composition = lottieShareComp,
                                                progress = { shareProgress },
                                                modifier = Modifier.requiredSize(22.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = "Share",
                                                tint = Color(0xFF334155),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Share",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }

                                // 5. Download (Continuously Running with 1.5s pause)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedVideoToDownload = video
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    LottieDownloadIcon(
                                        isDownloading = true,
                                        size = 22.dp,
                                        pauseDelayMs = 1500L
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Download",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }

                        // Inline Comments Box Preview
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Comments (${comments.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (comments.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFBAE6FD)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "U",
                                                    color = Color(0xFF0284C7),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = comments[0],
                                                fontSize = 11.sp,
                                                color = Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section Divider Line
                        item {
                            HorizontalDivider(
                                color = Color(0xFFE2E8F0),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // "Up next" Header (As requested in Screenshot 2)
                        item {
                            Text(
                                text = "Up next",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        // Up Next Feed List Items (Same thumbnail card as Home Page with Watch, Download & 3-dot options)
                        items(upNextList, key = { it.id }) { relVideo ->
                            val isRelDownloading = downloads.any { it.videoId == relVideo.id && (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED) }
                            VideoCloudCard(
                                video = relVideo,
                                isWatchLater = viewModel.isWatchLater(relVideo.id),
                                isDownloading = isRelDownloading,
                                onWatchLaterClick = { viewModel.toggleWatchLater(relVideo) },
                                onDownloadClick = { selectedVideoToDownload = relVideo },
                                onMoreOptionsClick = { selectedVideoForMoreOptions = relVideo },
                                onPlayClick = { viewModel.playVideo(relVideo) }
                            )
                        }
                    }
                }
            }

            // Download options bottom sheet (Resolution, type, size, format)
            if (selectedVideoToDownload != null) {
                DownloadVideoBottomSheet(
                    video = selectedVideoToDownload!!,
                    viewModel = viewModel,
                    onDismiss = { selectedVideoToDownload = null }
                )
            }

            // More options / share sheet
            if (selectedVideoForMoreOptions != null) {
                ShareVideoBottomSheet(
                    video = selectedVideoForMoreOptions!!,
                    onDismiss = { selectedVideoForMoreOptions = null }
                )
            }
        }
    }
}

// LARGE YOUTUBE-STYLE VIDEO CARD FOR "UP NEXT" SECTION (AS SHOWN IN SCREENSHOT 2)
@Composable
fun UpNextLargeVideoCard(
    video: CloudVideo,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Large Thumbnail Box with Play Overlay & Duration Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(video.imageUrl),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play Button overlay
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Duration Badge (Bottom Right)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = video.duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Row: Creator Avatar, Title & Subtitle, Download Badge Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = rememberAsyncImagePainter(video.imageUrl),
                    contentDescription = video.creator,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${video.creator} • ${video.views}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Download pill button on right (e.g. 0MB as in Screenshot 2)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { onDownloadClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "0MB",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ExoPlayerSurface(
    exoPlayer: ExoPlayer?,
    modifier: Modifier = Modifier
) {
    if (exoPlayer == null) return

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        update = { playerView ->
            if (playerView.player != exoPlayer) {
                playerView.player = exoPlayer
            }
        },
        modifier = modifier
    )
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ExoPlayerSurface(
    streamUrl: String,
    isPlaying: Boolean = true,
    onPlaybackError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(streamUrl) {
        if (streamUrl.isEmpty()) null
        else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
                playWhenReady = isPlaying
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        onPlaybackError(error.localizedMessage ?: "Playback stream interrupted")
                    }
                })
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        if (exoPlayer != null) {
            if (isPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        }
    }

    ExoPlayerSurface(exoPlayer = exoPlayer, modifier = modifier)
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
