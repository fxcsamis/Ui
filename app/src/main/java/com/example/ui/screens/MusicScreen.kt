package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudMusicTrack
import com.example.ui.CloudihubViewModel
import kotlinx.coroutines.delay

@Composable
fun MusicScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = viewModel.isDarkTheme
    val currentTrack = viewModel.currentTrack
    val isPlaying = viewModel.isPlaying
    val progressSec = viewModel.currentTrackProgressSec
    val totalSec = currentTrack.durationSec
    val context = LocalContext.current

    // States for custom interactions in the redesigned screen
    var selectedCategory by remember { mutableStateOf("All Hits") }
    val likedTracks = remember { mutableStateListOf<String>() }
    
    // Smooth transition animations
    val listState = rememberScrollState()

    // Filter tracks based on categories for real filter experience
    val filteredTracks = remember(selectedCategory, viewModel.musicTracks) {
        when (selectedCategory) {
            "Lofi Chill" -> viewModel.musicTracks.filter { it.title.contains("Lofi", ignoreCase = true) || it.artist.contains("Lofi", ignoreCase = true) || it.artist.contains("Ambient", ignoreCase = true) }
            "Romantic" -> viewModel.musicTracks.filter { it.id == "m4" || it.title.contains("Acoustic", ignoreCase = true) || it.title.contains("Silver", ignoreCase = true) }
            "Devotional" -> viewModel.musicTracks.filter { it.artist.contains("Peace", ignoreCase = true) || it.title.contains("Heaven", ignoreCase = true) }
            else -> viewModel.musicTracks
        }
    }

    // Dynamic stats for concentric circle values
    var viewsCount by remember { mutableStateOf("5.8M Views") }
    var playlistCount by remember { mutableStateOf("24K Playlist") }
    var likesCount by remember { mutableStateOf("1.2M Likes") }

    // Color theme setups
    val mainBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val mainTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(mainBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(listState)
                .padding(bottom = 140.dp) // extra padding for bottom play controls
        ) {
            // --- TOP HEADER TITLE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cloud Sky Stream",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = mainTextColor
                    )
                    Text(
                        text = "Premium immersive music engine",
                        fontSize = 12.sp,
                        color = subtitleTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Audio waves design micro-animation on top bar
                Row(
                    modifier = Modifier.height(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(4) { index ->
                        val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
                        val heightMultiplier by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 400 + (index * 150),
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "wave_scale"
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight(if (isPlaying) heightMultiplier else 0.2f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accentColor)
                        )
                    }
                }
            }

            // --- REDESIGNED HIGH-FIDELITY "TODAY'S MOST PLAYING SONG" CARD (SLIM & WIDE COINS-REPLACED CARD) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header text replacement of "Daily checking"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Today's most playing song",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF08A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "TRENDING #1",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF854D0E)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Song Thumbnail & Info Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"),
                                    contentDescription = "Today's hot song cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Interactive Overlay Play Indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            val trendTrack = CloudMusicTrack(
                                                id = "trend_today",
                                                title = "Dil Diyan Gallan",
                                                artist = "Atif Aslam",
                                                duration = "4:20",
                                                durationSec = 260,
                                                imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                                                streamUrl = ""
                                            )
                                            viewModel.playTrack(trendTrack)
                                            
                                            // Animate stats increment on tap
                                            viewsCount = "5.9M Views"
                                            playlistCount = "25K Playlist"
                                            likesCount = "1.3M Likes"
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying && currentTrack.title == "Dil Diyan Gallan") Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play Hot Track",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Dil Diyan Gallan",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Atif Aslam",
                                    fontSize = 12.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "98% Love rate",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // --- 3 CONCENTRIC LIQUIDITY GRADIENT LINE CIRCLES (BIG -> MEDIUM -> SMALL) ---
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .testTag("concentric_stats"),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. BIG CIRCLE (Outer Line) - Represents Views count
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF38BDF8),
                                                Color(0xFF0EA5E9),
                                                Color(0xFF0284C7),
                                                Color(0xFF38BDF8)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .background(Color(0xFFE0F2FE).copy(alpha = 0.05f), CircleShape),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text(
                                    text = viewsCount,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 2. MEDIUM CIRCLE (Middle Line) - Represents Playlist adds
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFEC4899),
                                                Color(0xFFF43F5E),
                                                Color(0xFFD946EF),
                                                Color(0xFFEC4899)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .background(Color(0xFFFCE7F3).copy(alpha = 0.05f), CircleShape),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text(
                                    text = playlistCount,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFBE185D),
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 3. SMALL CIRCLE (Inner Line) - Represents Likes count
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFF59E0B),
                                                Color(0xFFEF4444),
                                                Color(0xFFF97316),
                                                Color(0xFFF59E0B)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .background(Color(0xFFFEE2E2).copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = likesCount,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB91C1C),
                                    modifier = Modifier.padding(2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- CATEGORY CHIPS TAB BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("All Hits", "Lofi Chill", "Romantic", "Devotional")
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) accentColor else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else mainTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- REBUILT LIST OF HIGH-FIDELITY MUSIC TRACKS ---
            Text(
                text = "$selectedCategory Tracks",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = mainTextColor,
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 12.dp)
            )

            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No songs found in this category.", color = subtitleTextColor)
                }
            } else {
                filteredTracks.forEachIndexed { index, track ->
                    val isCurrent = currentTrack.id == track.id
                    val isTrackLiked = likedTracks.contains(track.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                if (isDark) Color(0xFF1E293B) else Color(0xFFF0F9FF)
                            } else cardBgColor
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isCurrent) accentColor.copy(alpha = 0.5f) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { viewModel.playTrack(track) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Album Art
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(track.imageUrl),
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isCurrent && isPlaying) {
                                    // Animated playing overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = "Playing",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Title & Singer
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mainTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    fontSize = 11.sp,
                                    color = subtitleTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Actions Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Like action
                                IconButton(
                                    onClick = {
                                        if (isTrackLiked) likedTracks.remove(track.id) else likedTracks.add(track.id)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isTrackLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isTrackLiked) Color(0xFFEF4444) else subtitleTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Custom play button indicator
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = if (isCurrent && isPlaying) accentColor else accentColor.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play state",
                                        tint = if (isCurrent && isPlaying) Color.White else accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FLOATING CONTROLLER / PLAYBACK CARD (PERSISTENT IMMERSIVE PLAYER DECK) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Title and Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spinning Art disk
                        val infiniteTransition = rememberInfiniteTransition(label = "ArtworkRotation")
                        val rotationAngle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(22000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ArtworkAngle"
                        )
                        val activeRotation = if (isPlaying) rotationAngle else 0f

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(currentTrack.imageUrl),
                                contentDescription = currentTrack.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(activeRotation)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = mainTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack.artist,
                                fontSize = 11.sp,
                                color = subtitleTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Playback Control Button deck
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { viewModel.previousTrack() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = mainTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(accentColor, CircleShape)
                                    .clickable { viewModel.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.nextTrack() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = mainTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar & Tickers
                    val progressFraction = if (totalSec > 0) progressSec.toFloat() / totalSec else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(progressSec),
                            fontSize = 10.sp,
                            color = subtitleTextColor,
                            fontWeight = FontWeight.Bold
                        )

                        Slider(
                            value = progressFraction,
                            onValueChange = { /* Tap seek action */ },
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentColor,
                                inactiveTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                thumbColor = accentColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(16.dp)
                        )

                        Text(
                            text = currentTrack.duration,
                            fontSize = 10.sp,
                            color = subtitleTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Utility function to print professional timestamp formats
private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
