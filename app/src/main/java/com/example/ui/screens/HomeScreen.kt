package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudVideo
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudShape
import com.example.ui.components.CloudSkyBackground
import com.example.ui.components.FloatingAiLottieWidget
import com.example.ui.components.LottieDownloadIcon
import com.example.ui.components.NavigationTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color(0xFFE2E8F0),
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0)
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 300f, translateAnim - 300f),
            end = Offset(translateAnim + 300f, translateAnim + 300f)
        )
    )
}

@Composable
fun ShimmerVideoCloudCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmer()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmer()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(64.dp, 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shimmer()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val videos = viewModel.videos
    val searchQuery = viewModel.searchQuery
    val activeDownloads by viewModel.downloads.collectAsState()
    val activeCount = activeDownloads.count { it.status == com.example.ui.DownloadStatus.DOWNLOADING || it.status == com.example.ui.DownloadStatus.QUEUED }

    // Floating categories
    val categories = listOf("All", "Music", "Rainclouds", "Infrastructure", "Sky Timelapse", "Edge Gaming", "Aesthetics")
    var selectedCategory by remember { mutableStateOf("All") }

    val lazyListState = rememberLazyListState()
    val isDark = viewModel.isDarkTheme
    val keyboardController = LocalSoftwareKeyboardController.current

    var isSearchScreenOpen by remember { mutableStateOf(false) }
    var selectedVideoToShare by remember { mutableStateOf<CloudVideo?>(null) }
    var selectedVideoToDownload by remember { mutableStateOf<CloudVideo?>(null) }
    var selectedVideoForMoreOptions by remember { mutableStateOf<CloudVideo?>(null) }
    var recentSearches by remember {
        mutableStateOf(listOf("Rainclouds", "Storm tracker", "Space timelapse", "Sky view", "Thunderstorm", "Rainbow"))
    }

    // --- NESTED SCROLL & COLLAPSIBLE ANIMATION STATE ---
    val density = LocalDensity.current
    var topBarOffsetHeightPx by remember { mutableStateOf(0f) }
    var topBarHeightPx by remember { mutableStateOf(with(density) { 140.dp.toPx() }) }

    val isEmptyResults = videos.isEmpty() && !viewModel.isLoadingVideos

    LaunchedEffect(isEmptyResults) {
        if (isEmptyResults) {
            topBarOffsetHeightPx = 0f
        }
    }

    val nestedScrollConnection = remember(topBarHeightPx, isEmptyResults) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isEmptyResults) {
                    topBarOffsetHeightPx = 0f
                    return Offset.Zero
                }
                // Instantly update the top bar offset based on scroll gestures
                val delta = available.y
                val newOffset = topBarOffsetHeightPx + delta
                topBarOffsetHeightPx = newOffset.coerceIn(-topBarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    val measuredTopBarDp = with(density) { topBarHeightPx.toDp() }
    val topPaddingDp = if (measuredTopBarDp > 0.dp) measuredTopBarDp + 52.dp else 200.dp

    val categoryLazyRowState = rememberLazyListState()
    LaunchedEffect(categoryLazyRowState) {
        while (true) {
            if (categoryLazyRowState.isScrollInProgress) {
                // Wait while user is dragging
                while (categoryLazyRowState.isScrollInProgress) {
                    kotlinx.coroutines.delay(100)
                }
                // Pause 2.5 seconds after user finishes scrolling before auto-slide resumes
                kotlinx.coroutines.delay(2500)
            } else {
                categoryLazyRowState.scrollBy(1.2f)
                kotlinx.coroutines.delay(16)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            .nestedScroll(nestedScrollConnection)
    ) {
        if (isEmptyResults) {
            CloudSkyBackground(modifier = Modifier.fillMaxSize())
        }
        // --- VIDEO FEED / SHIMMER FEED WITH PULL TO REFRESH ---
        PullToRefreshBox(
            isRefreshing = viewModel.isLoadingVideos,
            onRefresh = { viewModel.loadHybridFeed() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(top = topPaddingDp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Content Feed Items: Loading, Empty, or Videos
                if (viewModel.isLoadingVideos) {
                    items(3) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ShimmerVideoCloudCard()
                        }
                    }
                } else if (videos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CloudShape())
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Empty Search",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No sky matches found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Try searching for Cloud, Rain or Space",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                } else {
                    items(videos, key = { it.id }) { video ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val isVideoDownloading = activeDownloads.any { it.videoId == video.id && (it.status == com.example.ui.DownloadStatus.DOWNLOADING || it.status == com.example.ui.DownloadStatus.QUEUED) }
                            VideoCloudCard(
                                video = video,
                                isWatchLater = viewModel.isWatchLater(video.id),
                                isDownloading = isVideoDownloading,
                                onWatchLaterClick = { viewModel.toggleWatchLater(video) },
                                onDownloadClick = { selectedVideoToDownload = video },
                                onMoreOptionsClick = { selectedVideoForMoreOptions = video },
                                onPlayClick = { viewModel.playVideo(video) }
                            )
                        }
                    }
                }
            }
        }

        // --- ANIMATED FLOATING IMMERSIVE TOP BAR CONTAINER (Floats on top of feed) ---
        var startEntranceAnimation by remember { mutableStateOf(false) }
        val entranceProgress by animateFloatAsState(
            targetValue = if (startEntranceAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1200, easing = EaseOutQuart),
            label = "TopBarEntrance"
        )
        LaunchedEffect(Unit) {
            startEntranceAnimation = true
        }

        val fraction = if (topBarHeightPx > 0f) -topBarOffsetHeightPx / topBarHeightPx else 0f
        val translationY = with(density) { topBarOffsetHeightPx.toDp() }
        val animatedOffsetY = translationY - (24 * (1f - entranceProgress)).dp
        
        // Mathematical fade and blur to guarantee zero lingering artifacts
        val baseAlpha = if (fraction >= 0.90f) {
            val progress = (fraction - 0.90f) / 0.10f
            (1f - progress).coerceIn(0f, 1f)
        } else {
            1f
        }
        val alpha = baseAlpha * entranceProgress

        // Start blurring gradually only after 40% has scrolled up/entered
        val blurRadius = if (fraction >= 0.40f) {
            val progress = (fraction - 0.40f) / 0.60f
            (progress * 12f).dp
        } else {
            0.dp
        }
        
        // Smoothly fade out the top bar's background color so it hides completely as it blurs and scrolls out
        val bgAlpha = if (fraction >= 0.40f) {
            val progress = (fraction - 0.40f) / 0.60f
            (1f - progress).coerceIn(0f, 1f)
        } else {
            1f
        }

        val topBarBgColor = if (isEmptyResults) {
            Color.Transparent
        } else if (isDark) {
            Color(0xFF0F172A).copy(alpha = bgAlpha * entranceProgress)
        } else {
            Color(0xFFF8FAFC).copy(alpha = bgAlpha * entranceProgress)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = animatedOffsetY)
                .clipToBounds() // Strictly clip within layout bounds to prevent any blur bleeding downwards
                .background(topBarBgColor)
                .statusBarsPadding() // Seamless immersive status bar matching
                .onSizeChanged { size ->
                    topBarHeightPx = size.height.toFloat()
                }
                .graphicsLayer(alpha = alpha)
                .let { modifier ->
                    if (blurRadius > 0.dp) modifier.blur(blurRadius) else modifier
                }
                .padding(bottom = 8.dp)
        ) {
            // 1. Search Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cloud-shaped Search Bar Container
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                        .clickable { isSearchScreenOpen = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text("Search cloud files...", color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8), fontSize = 14.sp)
                        } else {
                            Text(searchQuery, color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateSearchQuery("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Reload / Refresh Feed Button
                IconButton(
                    onClick = { viewModel.loadHybridFeed() },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("reload_feed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Feed",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Download Hub Button matching Voice/Reload Theme
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.showDownloadHub = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("download_icon_button")
                    ) {
                        LottieDownloadIcon(
                            isDownloading = activeCount > 0,
                            size = 20.dp
                        )
                    }

                    if (activeCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Voice Speech-to-Text Button
                IconButton(
                    onClick = { viewModel.startVoiceSearch() },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("voice_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice search",
                        tint = Color(0xFF0369A1),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Profile Logo Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBAE6FD))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { viewModel.selectTab(NavigationTab.Profile) }
                        .testTag("profile_avatar_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = if (viewModel.isGoogleSignedIn && viewModel.signedInUserPhoto.isNotEmpty()) {
                        viewModel.signedInUserPhoto
                    } else {
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100"
                    }
                    Image(
                        painter = rememberAsyncImagePainter(avatarUrl),
                        contentDescription = "Profile Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2. Quick Category List with Frozen/Pinned Shorts & Live Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed / Frozen "Shorts" & "Live" Buttons on Left (Do NOT scroll)
                Row(
                    modifier = Modifier
                        .zIndex(5f)
                        .padding(start = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Shorts Button
                    val isShortsSelected = selectedCategory == "Shorts"
                    Row(
                        modifier = Modifier
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.15f),
                                ambientColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .clip(CircleShape)
                            .background(if (isShortsSelected) Color(0xFF0284C7) else if (isDark) Color(0xFF1E293B) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isShortsSelected) Color.Transparent else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedCategory = "Shorts"
                                viewModel.updateSearchQuery("Shorts")
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShortsLogoIcon(modifier = Modifier.size(22.dp))
                        Text(
                            text = "Shorts",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isShortsSelected) Color.White else if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    // 2. Live Button
                    val isLiveSelected = selectedCategory == "Live"
                    Row(
                        modifier = Modifier
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.15f),
                                ambientColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .clip(CircleShape)
                            .background(if (isLiveSelected) Color(0xFF0284C7) else if (isDark) Color(0xFF1E293B) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isLiveSelected) Color.Transparent else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedCategory = "Live"
                                viewModel.updateSearchQuery("Live")
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveLogoIcon(modifier = Modifier.size(26.dp))
                        Text(
                            text = "Live",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveSelected) Color.White else if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }

                // Scrollable Container with Soft Smoke Gradient Overlay on the left
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Scrollable Categories LazyRow with continuous auto-slide animation
                    LazyRow(
                        state = categoryLazyRowState,
                        contentPadding = PaddingValues(start = 24.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(count = Int.MAX_VALUE) { index ->
                            val category = categories[index % categories.size]
                            val isSelected = selectedCategory == category
                            val isAllCategory = category == "All"
                            val background = if (isSelected) {
                                if (isAllCategory) (if (isDark) Color(0xFF383838) else Color(0xFF27272A))
                                else Color(0xFF0284C7)
                            } else {
                                if (isDark) Color(0xFF1E293B) else Color.White
                            }
                            val border = if (isSelected) Color.Transparent else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            val textCol = if (isSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF0F172A)

                            Box(
                                modifier = Modifier
                                    .shadow(if (isSelected) 0.dp else 1.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(background)
                                    .border(1.dp, border, CircleShape)
                                    .clickable {
                                        selectedCategory = category
                                        if (category == "All") {
                                            viewModel.updateSearchQuery("")
                                        } else {
                                            viewModel.updateSearchQuery(category)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textCol
                                )
                            }
                        }
                    }

                    // Soft Fog / Smoke Gradient Shadow Overlay right behind & next to Live button
                    val smokeColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-12).dp)
                            .width(72.dp)
                            .matchParentSize()
                            .zIndex(3f)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        smokeColor,
                                        smokeColor.copy(alpha = 0.98f),
                                        smokeColor.copy(alpha = 0.88f),
                                        smokeColor.copy(alpha = 0.65f),
                                        smokeColor.copy(alpha = 0.35f),
                                        smokeColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }

        // --- ANIMATED SEARCH SCREEN OVERLAY WITH SIDE SLIDE ANIMATION ---
        AnimatedVisibility(
            visible = isSearchScreenOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 400, easing = EaseOutQuart)
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 350, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(350)),
            modifier = Modifier.fillMaxSize().zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            ) {
                CloudSkyBackground(modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Top Search Row in Search Screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isSearchScreenOpen = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Go back",
                                tint = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }

                        // Search Input
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                                .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                textStyle = TextStyle(
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotEmpty()) {
                                            viewModel.triggerDoneSearchKeyboardAction(searchQuery)
                                            isSearchScreenOpen = false
                                            keyboardController?.hide()
                                        }
                                    }
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(if (isDark) Color.White else Color(0xFF0284C7)),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(
                                        contentAlignment = Alignment.CenterStart,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search sky and clouds...",
                                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.updateSearchQuery("") },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Voice Button
                        IconButton(
                            onClick = { viewModel.startVoiceSearch() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice search",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Download Button
                        IconButton(
                            onClick = { viewModel.showDownloadHub = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            LottieDownloadIcon(
                                isDownloading = activeCount > 0,
                                size = 26.dp
                            )
                        }

                        // Reload Button
                        IconButton(
                            onClick = { viewModel.loadHybridFeed() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Feed",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Profile Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFBAE6FD))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    isSearchScreenOpen = false
                                    viewModel.selectTab(NavigationTab.Profile)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarUrl = if (viewModel.isGoogleSignedIn && viewModel.signedInUserPhoto.isNotEmpty()) {
                                viewModel.signedInUserPhoto
                            } else {
                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100"
                            }
                            Image(
                                painter = rememberAsyncImagePainter(avatarUrl),
                                contentDescription = "Profile Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Recent Searches Section
                        if (recentSearches.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                TextButton(
                                    onClick = { recentSearches = emptyList() }
                                ) {
                                    Text("Clear All", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { searchItem ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0).copy(alpha = 0.6f))
                                            .clickable {
                                                viewModel.updateSearchQuery(searchItem)
                                                isSearchScreenOpen = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "History",
                                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = searchItem,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                recentSearches = recentSearches.filter { it != searchItem }
                                            },
                                            modifier = Modifier.size(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Trending Searches Section
                        Text(
                            text = "Trending Searches",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val trendingSearches = listOf("Beautiful Aurora", "Nimbus Clouds", "Cosmic Stardust", "Lightning Strike", "Solar Eclipse")
                        trendingSearches.forEachIndexed { idx, trend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f))
                                    .clickable {
                                        if (!recentSearches.contains(trend)) {
                                            recentSearches = listOf(trend) + recentSearches
                                        }
                                        viewModel.updateSearchQuery(trend)
                                        isSearchScreenOpen = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (idx) {
                                                0 -> Color(0xFFEF4444)
                                                1 -> Color(0xFFF97316)
                                                2 -> Color(0xFFF59E0B)
                                                else -> if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (idx + 1).toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = trend,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Trending",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        selectedVideoToShare?.let { videoToShare ->
            ShareVideoBottomSheet(
                video = videoToShare,
                onDismiss = { selectedVideoToShare = null }
            )
        }
        selectedVideoToDownload?.let { videoToDownload ->
            DownloadVideoBottomSheet(
                video = videoToDownload,
                viewModel = viewModel,
                onDismiss = { selectedVideoToDownload = null }
            )
        }
        selectedVideoForMoreOptions?.let { videoForMore ->
            VideoMoreOptionsSheet(
                video = videoForMore,
                isWatchLater = viewModel.isWatchLater(videoForMore.id),
                onDismiss = { selectedVideoForMoreOptions = null },
                onPlayClick = { viewModel.playVideo(videoForMore) },
                onShareClick = { selectedVideoToShare = videoForMore },
                onWatchLaterClick = { viewModel.toggleWatchLater(videoForMore) },
                onDownloadClick = { selectedVideoToDownload = videoForMore },
                onNotInterestedClick = {
                    Toast.makeText(viewModel.getApplication(), "Marked as Not Interested", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // --- FLOATING OVERLAY LOTTIE WIDGET & DYNAMIC AI ASSISTANT ---
        FloatingAiLottieWidget(
            isMediaPlaying = (viewModel.activeStreamingUrl.isNotEmpty() || viewModel.isPlaying),
            onSearchRequested = { query ->
                viewModel.updateSearchQuery(query)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp)
                .zIndex(100f)
        )
    }
}

@Composable
fun CloudStatusBadge(viewsText: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Cloud,
            contentDescription = "Cloud Status",
            tint = Color(0xFF0284C7),
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = viewsText,
            color = Color(0xFF334155),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

const val CUSTOM_SHARE_ICON_URL = "https://i.postimg.cc/1zmgjdRt/f6d0aee22a954d7db19f3c210f9d876e.jpg"
const val CUSTOM_DOWNLOAD_ICON_URL = "https://i.postimg.cc/Nfk0T4YX/236-2015-dekretua-LH.jpg"
const val CUSTOM_WATCH_LATER_ICON_URL = "https://i.postimg.cc/G2tMPzZm/Fast-Delivery-icon-concept-in-black-duo-line-color.jpg"

@Composable
fun AnimatedIconButton(
    icon: ImageVector? = null,
    imageUrl: String? = null,
    isLottieDownload: Boolean = false,
    isDownloading: Boolean = false,
    contentDescription: String,
    tint: Color = Color(0xFF64748B),
    imageSize: Dp = 26.dp,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    IconButton(
        onClick = {
            coroutineScope.launch {
                scale.animateTo(0.7f, animationSpec = tween(80))
                scale.animateTo(1.15f, animationSpec = tween(100))
                scale.animateTo(1.0f, animationSpec = tween(100))
            }
            onClick()
        },
        modifier = Modifier
            .size(38.dp)
            .scale(scale.value)
    ) {
        if (isLottieDownload) {
            LottieDownloadIcon(
                isDownloading = isDownloading,
                size = imageSize
            )
        } else if (!imageUrl.isNullOrEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(imageSize)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) Modifier.border(2.dp, Color(0xFFFF9100), CircleShape) else Modifier
                        )
                )
            }
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun VideoCloudCard(
    video: CloudVideo,
    isWatchLater: Boolean,
    isDownloading: Boolean = false,
    onWatchLaterClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    onPlayClick: (CloudVideo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable { onPlayClick(video) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            // Beautiful Cloud-Shaped Thumbnail Background Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { onPlayClick(video) },
                contentAlignment = Alignment.Center
            ) {
                // Background Cloud Shape decorative layer with shadow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .shadow(elevation = 4.dp, shape = CloudShape(), clip = false)
                        .background(Color.White.copy(alpha = 0.92f), CloudShape())
                )

                // Actual video thumbnail image sitting inside
                Image(
                    painter = rememberAsyncImagePainter(video.imageUrl),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play Button floating in center
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Duration badge at the bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 10.dp, end = 10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video information and actions at bottom (On clean White background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = video.creator,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${video.sizeMb.toInt()} MB",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Right Column: Blue Cloud status badge on top, 3 Action Icons under it
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // White pill badge with Blue Cloud line icon & views count
                    CloudStatusBadge(viewsText = video.views)

                    // 3 Action Icons directly under the red dot & views count badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedIconButton(
                            imageUrl = CUSTOM_WATCH_LATER_ICON_URL,
                            contentDescription = "Watch Later",
                            isSelected = isWatchLater,
                            imageSize = 20.dp,
                            onClick = onWatchLaterClick
                        )

                        AnimatedIconButton(
                            isLottieDownload = true,
                            isDownloading = isDownloading,
                            contentDescription = "Download Video",
                            imageSize = 20.dp,
                            onClick = onDownloadClick
                        )

                        IconButton(
                            onClick = onMoreOptionsClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ShareAppTarget(
    val name: String,
    val bgColor: Color,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareVideoBottomSheet(
    video: CloudVideo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun launchNativeShare() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${video.title}\n${video.fileUrl}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share video")
        context.startActivity(shareIntent)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // Title Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sharing Link Box (YouTube Style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(CUSTOM_SHARE_ICON_URL),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.fileUrl,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(video.fileUrl))
                            Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Horizontal Scrollable App Items (WhatsApp, Facebook, Telegram, Gmail, etc.)
            val shareApps = remember {
                listOf(
                    ShareAppTarget("WhatsApp", Color(0xFF25D366), Icons.Default.Chat),
                    ShareAppTarget("Facebook", Color(0xFF1877F2), Icons.Default.Share),
                    ShareAppTarget("Messenger", Color(0xFF0084FF), Icons.Default.Send),
                    ShareAppTarget("Telegram", Color(0xFF229ED9), Icons.Default.Send),
                    ShareAppTarget("Gmail", Color(0xFFEA4335), Icons.Default.Email),
                    ShareAppTarget("Bluetooth", Color(0xFF0082FC), Icons.Default.Bluetooth),
                    ShareAppTarget("More", Color(0xFF64748B), Icons.Default.MoreHoriz)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                shareApps.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { launchNativeShare() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(app.bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = app.icon,
                                contentDescription = app.name,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = app.name,
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action List Rows (Copy link, System Share, Create post)
            Column(modifier = Modifier.fillMaxWidth()) {
                ShareActionRow(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy link",
                    onClick = {
                        clipboardManager.setText(AnnotatedString(video.fileUrl))
                        Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                ShareActionRow(
                    imageUrl = CUSTOM_SHARE_ICON_URL,
                    title = "Quick Share / System Share",
                    onClick = { launchNativeShare() }
                )

                ShareActionRow(
                    icon = Icons.Default.Edit,
                    title = "Create post",
                    onClick = {
                        Toast.makeText(context, "Opening post creation...", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ShareActionRow(
    icon: ImageVector? = null,
    imageUrl: String? = null,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

private data class DownloadFormatOption(
    val id: String,
    val title: String,
    val format: String,
    val badge: String? = null,
    val sizeMb: Double,
    val isAudio: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadVideoBottomSheet(
    video: CloudVideo,
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customTitle by remember(video.title) { mutableStateOf(video.title) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var selectedOptionId by remember { mutableStateOf("v_720p") }
    var storagePath by remember { mutableStateOf("/storage/emulated/0/Cloudihub/download/") }

    val audioOptions = remember(video.sizeMb) {
        listOf(
            DownloadFormatOption("a_48k_m4a", "48K", "(M4A)", null, (video.sizeMb * 0.12), true),
            DownloadFormatOption("a_48k_mp3", "48K", "(MP3)", "SLOW", (video.sizeMb * 0.12), true),
            DownloadFormatOption("a_128k_m4a", "128K", "(M4A)", null, (video.sizeMb * 0.2), true),
            DownloadFormatOption("a_128k_mp3", "128K", "(MP3)", "SLOW", (video.sizeMb * 0.2), true),
            DownloadFormatOption("a_256k_mp3", "256K", "(MP3)", "SLOW", (video.sizeMb * 0.28), true),
            DownloadFormatOption("a_320k_mp3", "320K", "(MP3)", "FAST", (video.sizeMb * 0.35), true)
        )
    }

    val videoOptions = remember(video.sizeMb) {
        listOf(
            DownloadFormatOption("v_144p", "144P", "(MP4)", null, (video.sizeMb * 0.25), false),
            DownloadFormatOption("v_240p", "240P", "(MP4)", null, (video.sizeMb * 0.45), false),
            DownloadFormatOption("v_360p", "360P", "(MP4)", null, (video.sizeMb * 0.65), false),
            DownloadFormatOption("v_480p", "480P", "(MP4)", null, (video.sizeMb * 0.9), false),
            DownloadFormatOption("v_720p", "720P HD", "(MP4)", null, video.sizeMb, false),
            DownloadFormatOption("v_1080p", "1080P", "(MP4)", "FULL HD", (video.sizeMb * 1.8), false)
        )
    }

    val allOptions = remember(audioOptions, videoOptions) { audioOptions + videoOptions }
    val currentSelected = allOptions.find { it.id == selectedOptionId } ?: videoOptions[4]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // TOP CARD: Thumbnail + Title + Rename + Storage Path
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Row 1: Thumbnail + Title + Rename Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Thumbnail with duration overlay
                        Box(
                            modifier = Modifier
                                .size(width = 86.dp, height = 54.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(video.imageUrl),
                                contentDescription = video.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = video.duration.ifBlank { "03:49" },
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title text
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = customTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Rename Action Button
                        Text(
                            text = if (isEditingTitle) "Save" else "Rename",
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isEditingTitle = !isEditingTitle }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Editable Title Field (when renaming)
                    AnimatedVisibility(visible = isEditingTitle) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = Color(0xFF0284C7),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Storage path + Change button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Path:$storagePath",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "59.6GB FREE / 104.9GB",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = "Change",
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    Toast.makeText(viewModel.getApplication(), "Storage path updated!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MAIN OPTIONS CONTAINER CARD (Music + Video Formats)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // SECTION 1: MUSIC
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Music",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2-Column Grid for Music
                    val audioRows = audioOptions.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        audioRows.forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { opt ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        DownloadOptionItemRow(
                                            option = opt,
                                            isSelected = selectedOptionId == opt.id,
                                            onSelect = { selectedOptionId = opt.id }
                                        )
                                    }
                                }
                                if (rowOptions.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 2: VIDEO
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2-Column Grid for Video
                    val videoRows = videoOptions.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        videoRows.forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { opt ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        DownloadOptionItemRow(
                                            option = opt,
                                            isSelected = selectedOptionId == opt.id,
                                            onSelect = { selectedOptionId = opt.id }
                                        )
                                    }
                                }
                                if (rowOptions.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BOTTOM FULL-WIDTH DOWNLOAD BUTTON
            Button(
                onClick = {
                    val qualityLabel = "${currentSelected.title} ${currentSelected.format}".trim()
                    viewModel.triggerVideoDownloadWithOptions(
                        video = video,
                        customTitle = customTitle.ifBlank { video.title },
                        qualityLabel = qualityLabel,
                        estimatedSizeMb = currentSelected.sizeMb,
                        isAudioOnly = currentSelected.isAudio
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                LottieDownloadIcon(
                    isDownloading = false,
                    size = 24.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DOWNLOAD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun DownloadOptionItemRow(
    option: DownloadFormatOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val accentColor = Color(0xFF0284C7)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = option.title,
            tint = if (isSelected) accentColor else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) accentColor else Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = option.format,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                option.badge?.let { badgeText ->
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = String.format("%.2fMB", option.sizeMb),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) accentColor else Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun ShortsLogoIcon(modifier: Modifier = Modifier) {
    Image(
        painter = rememberAsyncImagePainter("https://i.postimg.cc/KvTkCxmW/You-Tube-Shorts-Logo-PNG-Transparent-(1).jpg"),
        contentDescription = "YouTube Shorts Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
    )
}

@Composable
fun LiveLogoIcon(modifier: Modifier = Modifier) {
    Image(
        painter = rememberAsyncImagePainter("https://i.postimg.cc/HsWXKjSW/Live-icon-PNG-Transparent-Live-logo-(1).jpg"),
        contentDescription = "Live Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMoreOptionsSheet(
    video: CloudVideo,
    isWatchLater: Boolean,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onNotInterestedClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Video Header Preview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(video.imageUrl),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${video.creator} • ${video.views} views",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Option 1: Play Video
            MoreOptionRowItem(
                icon = Icons.Default.PlayCircle,
                title = "Play Video",
                subtitle = "Stream video in high quality",
                onClick = {
                    onPlayClick()
                    onDismiss()
                }
            )

            // Option 2: Share Video
            MoreOptionRowItem(
                icon = Icons.Default.Share,
                title = "Share Video",
                subtitle = "Send link to social apps",
                onClick = {
                    onShareClick()
                    onDismiss()
                }
            )

            // Option 3: Watch Later
            MoreOptionRowItem(
                icon = if (isWatchLater) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                title = if (isWatchLater) "Remove from Watch Later" else "Save to Watch Later",
                subtitle = "Access anytime from Watch Later list",
                onClick = {
                    onWatchLaterClick()
                    onDismiss()
                }
            )

            // Option 4: Download Video
            MoreOptionRowItem(
                icon = Icons.Default.CloudDownload,
                title = "Download Video",
                subtitle = "Save for offline playback",
                onClick = {
                    onDownloadClick()
                    onDismiss()
                }
            )

            // Option 5: Not Interested
            MoreOptionRowItem(
                icon = Icons.Default.Block,
                title = "Not Interested",
                subtitle = "Hide similar content from feed",
                onClick = {
                    onNotInterestedClick()
                    onDismiss()
                }
            )

            // Option 6: Copy Link
            MoreOptionRowItem(
                icon = Icons.Default.ContentCopy,
                title = "Copy Video Link",
                subtitle = "Copy direct CDN URL to clipboard",
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Video Link", video.fileUrl.ifEmpty { "https://youtube.com/watch?v=${video.id}" })
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoreOptionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F9FF))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
