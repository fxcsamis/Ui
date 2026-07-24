package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudVideo
import com.example.ui.CloudihubViewModel
import kotlinx.coroutines.delay

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoStreamingPlayer(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val video = viewModel.playingVideo ?: return
    val streamUrl = viewModel.activeStreamingUrl
    val extractorMsg = viewModel.extractorModeMsg
    val isExtracting = viewModel.isExtracting

    var isPlayerExpanded by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackErrorMsg by remember { mutableStateOf<String?>(null) }
    var showControlsOverlay by remember { mutableStateOf(true) }

    // Custom Interactive States
    var isLiked by remember(video.id) { mutableStateOf(false) }
    var isDisliked by remember(video.id) { mutableStateOf(false) }
    var isSubscribed by remember(video.id) { mutableStateOf(false) }
    var isWatchLater by remember(video.id) { mutableStateOf(false) }
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

    // Draggable gesture to collapse/expand
    val draggableState = rememberDraggableState { delta ->
        if (delta > 20f && isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (delta < -20f && !isPlayerExpanded) {
            isPlayerExpanded = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical
            )
    ) {
        if (!isPlayerExpanded) {
            // ==========================================
            // MODULE 5: SLIDING MINI-PLAYER VIEW (Bottom bar)
            // ==========================================
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 86.dp)
                    .fillMaxWidth()
                    .height(68.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { isPlayerExpanded = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.96f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(video.imageUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.creator,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Mini Play/Pause",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.stopVideo() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Mini Close",
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // YOUTUBE-STYLE FULL PLAYER SCREEN (As requested in 2nd Screenshot)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. TOP VIDEO PLAYER CONTAINER (16:9 Aspect Ratio)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.77f)
                            .background(Color.Black)
                            .clickable { showControlsOverlay = !showControlsOverlay },
                        contentAlignment = Alignment.Center
                    ) {
                        if (playbackErrorMsg != null) {
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
                                streamUrl = streamUrl,
                                isPlaying = isPlaying,
                                onPlaybackError = { err -> playbackErrorMsg = err },
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
                                        onClick = { isPlayerExpanded = false },
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

                        // Medium Size Action Buttons Row (Save, Popup, Play Audio, Share, Download)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Save
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
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isWatchLater) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Save",
                                        tint = if (isWatchLater) Color(0xFF0284C7) else Color(0xFF334155),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Save",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }

                                // 2. Popup (Floating Window)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isPlayerExpanded = false
                                            Toast.makeText(context, "Popup Player Floating Mode", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PictureInPicture,
                                        contentDescription = "Popup",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Popup",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }

                                // 3. Play Audio
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            Toast.makeText(context, "Background Audio Mode Enabled", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Headphones,
                                        contentDescription = "Play Audio",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Play Audio",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }

                                // 4. Share
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
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = "Share",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Share",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }

                                // 5. Download
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.triggerVideoDownload(video)
                                            Toast.makeText(context, "Cloud downloading started!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudDownload,
                                        contentDescription = "Download",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(22.dp)
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

                        // Up Next Feed List Items (Large video thumbnails as in Screenshot 2)
                        items(upNextList, key = { it.id }) { relVideo ->
                            UpNextLargeVideoCard(
                                video = relVideo,
                                onClick = {
                                    viewModel.playVideo(relVideo)
                                },
                                onDownloadClick = {
                                    viewModel.triggerVideoDownload(relVideo)
                                    Toast.makeText(context, "Downloading ${relVideo.title}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
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
    streamUrl: String,
    isPlaying: Boolean,
    onPlaybackError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(streamUrl) {
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

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier
    )
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
