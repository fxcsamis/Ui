package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.example.ui.components.LocalSharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudihubViewModel

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val duration: String,
    val category: String,
    val views: String,
    val likes: String = "16K"
)

data class SpeedDialItem(
    val id: String,
    val title: String,
    val imageUrl: String
)

data class PlaylistData(
    val id: String,
    val title: String,
    val creator: String,
    val year: String = "Playlist • 2026",
    val description: String = "No description",
    val tracksCount: String = "38 tracks",
    val images: List<String>,
    val tracks: List<MusicTrack>
)

val sampleBanglaPlaylist = PlaylistData(
    id = "p1",
    title = "bangla best",
    creator = "Bibhas Debnath",
    year = "Playlist • 2026",
    description = "No description",
    tracksCount = "38 tracks",
    images = listOf(
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
        "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400",
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"
    ),
    tracks = listOf(
        MusicTrack("t1", "Jiboner Ayna", "Parvez", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400", "4:12", "Romantic", "15M"),
        MusicTrack("t2", "Bolbona Go Ar Kono Din - 21", "Baul Sukumar", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400", "5:04", "Folk", "28M"),
        MusicTrack("t3", "Aaina Mon Bhanga", "Zubeen Garg, Jeet Gannguli, & Priyo Chattopadhyay", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400", "4:45", "Melody", "42M"),
        MusicTrack("t4", "Chokhe Shanti Lage Amar", "Sathi Khan", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400", "3:58", "Pop", "18M"),
        MusicTrack("t5", "Jeena Haraam", "Vishal Mishra", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400", "3:30", "Chill", "35M"),
        MusicTrack("t6", "Ruposh (Original Score)", "Wajhi Farooki", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400", "4:20", "Romantic", "211M"),
        MusicTrack("t7", "Toota Jo Kabhi Tara", "Sachin-Jigar, Sumedha Karmahe & Atif Aslam", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=400", "5:05", "Romantic", "150M"),
        MusicTrack("t8", "Anyayo", "Aneesh & Krtin Kay", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400", "3:15", "Chill", "2.6M"),
        MusicTrack("t9", "Kusu Kusu", "Zahrah S Khan & Dev Negi", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400", "3:45", "Party", "907M"),
        MusicTrack("t10", "Jhol", "Maanu & Annural Khalid", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400", "3:10", "Relax", "808M")
    )
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MusicScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = viewModel.isDarkTheme
    val context = LocalContext.current

    // Playlist overlay & search screen state
    var selectedPlaylist by remember { mutableStateOf<PlaylistData?>(null) }
    var isSearchScreenOpen by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.isPlaylistOverlayOpen) {
        if (!viewModel.isPlaylistOverlayOpen) {
            selectedPlaylist = null
        }
    }

    BackHandler(enabled = isSearchScreenOpen || selectedPlaylist != null || viewModel.isFullMusicPlayerOpen) {
        if (isSearchScreenOpen) {
            isSearchScreenOpen = false
        } else if (selectedPlaylist != null) {
            selectedPlaylist = null
            viewModel.isPlaylistOverlayOpen = false
        } else if (viewModel.isFullMusicPlayerOpen) {
            viewModel.isFullMusicPlayerOpen = false
        }
    }

    // Player States
    var currentTrack by remember {
        mutableStateOf(
            MusicTrack(
                id = "0",
                title = "Toota Jo Kabhi Tara",
                artist = "Sachin-Jigar, Sumedha Karmahe & Atif Aslam",
                albumArt = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800",
                duration = "5:05",
                category = "Romantic",
                views = "150M"
            )
        )
    }
    var isPlaying by remember { mutableStateOf(false) }
    val isFullPlayerOpen = viewModel.isFullMusicPlayerOpen

    LaunchedEffect(selectedPlaylist) {
        viewModel.isPlaylistOverlayOpen = (selectedPlaylist != null)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.isFullMusicPlayerOpen = false
            viewModel.isPlaylistOverlayOpen = false
        }
    }
    var currentCategory by remember { mutableStateOf("All") }
    var musicSearchQuery by remember { mutableStateOf("") }

    // Marquee runner state for category filter chips in top bar
    val topCategoryLazyRowState = rememberLazyListState()
    LaunchedEffect(topCategoryLazyRowState) {
        while (true) {
            if (topCategoryLazyRowState.isScrollInProgress) {
                while (topCategoryLazyRowState.isScrollInProgress) {
                    kotlinx.coroutines.delay(100)
                }
                kotlinx.coroutines.delay(2500)
            } else {
                topCategoryLazyRowState.scrollBy(1.2f)
                kotlinx.coroutines.delay(16)
            }
        }
    }

    // Scroll state
    val musicListState = rememberLazyListState()

    // --- TOP BAR ENTRANCE & SCROLL ANIMATION STATE ---
    var isTopBarMounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isTopBarMounted = true
    }

    val density = LocalDensity.current
    var topBarOffsetHeightPx by remember { mutableStateOf(0f) }
    val maxUpOffsetPx = with(density) { (-10.dp).toPx() }

    val topBarNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = topBarOffsetHeightPx + delta
                topBarOffsetHeightPx = newOffset.coerceIn(maxUpOffsetPx, 0f)
                return Offset.Zero
            }
        }
    }

    val animatedEntranceY by animateFloatAsState(
        targetValue = if (isTopBarMounted) 0f else -100f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "musicTopBarEntranceY"
    )
    val animatedEntranceAlpha by animateFloatAsState(
        targetValue = if (isTopBarMounted) 1f else 0f,
        animationSpec = tween(400),
        label = "musicTopBarEntranceAlpha"
    )

    // Categories filter (matching screenshot)
    val categories = remember {
        listOf("All", "Relax", "Sleep", "Energize", "Sad", "Romance", "Chill", "Focus", "Workout", "Party")
    }

    // Speed Dial Items (3x3 grid matching screenshot)
    val speedDialItems = remember {
        listOf(
            SpeedDialItem("sd1", "Sunn Raha Hai", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400"),
            SpeedDialItem("sd2", "Bulleya (From \"Ae Dil...", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400"),
            SpeedDialItem("sd3", "Fakira 🎶 Tu puchh na...", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"),
            SpeedDialItem("sd4", "Barbaad Reprise - Fe...", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400"),
            SpeedDialItem("sd5", "Finding Her", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400"),
            SpeedDialItem("sd6", "Bagdhara - Proticcho...", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400"),
            SpeedDialItem("sd7", "SHOJONI", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=400"),
            SpeedDialItem("sd8", "Ishq de Fanniyar - Fe...", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400"),
            SpeedDialItem("sd9", "Arijit Special Mix", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400")
        )
    }

    // Quick Picks Tracks List
    val quickPicks = remember {
        listOf(
            MusicTrack("qp1", "Ruposh (Original Score)", "Wajhi Farooki • 211M plays", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400", "4:20", "Romantic", "211M"),
            MusicTrack("qp2", "Anyayo", "Aneesh & Krtin Kay • 2.6M plays", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400", "3:15", "Chill", "2.6M"),
            MusicTrack("qp3", "Kusu Kusu", "Zahrah S Khan & Dev Negi • 907M plays", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400", "3:45", "Party", "907M"),
            MusicTrack("qp4", "Jhol", "Maanu & Annural Khalid • 808M plays", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400", "3:10", "Relax", "808M"),
            MusicTrack("qp5", "no signal", "juggsi & kyra • 12M plays", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400", "2:50", "Chill", "12M"),
            MusicTrack("qp6", "Toota Jo Kabhi Tara", "Sachin-Jigar, Sumedha Karmahe & Atif Aslam • 150M plays", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400", "5:05", "Romantic", "150M")
        )
    }

    // Long Listens Tracks
    val longListens = remember {
        listOf(
            MusicTrack("ll1", "1 Hour of Night Hindi Lofi Songs To Chill & Relax...", "viral vicky vlogs • 1:06:40", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400", "1:06:40", "Relax", "500K"),
            MusicTrack("ll2", "Bairan | Jukebox | Amtee | Banjaare | Viral Har...", "AMTEE • 48:02", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400", "48:02", "Focus", "300K"),
            MusicTrack("ll3", "Best of Arijit Singh Mashup 2025 | O Sajni Re...", "Aftermorning • 35:30", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400", "35:30", "Romantic", "1M")
        )
    }

    // Heard in Shorts Tracks
    val shortsTracks = remember {
        listOf(
            MusicTrack("hs1", "Traag (feat. Jozo & Kraantje Pappie)", "Bizzey • 652M plays", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400", "3:02", "Party", "652M"),
            MusicTrack("hs2", "Phagooner Mohonaye", "Various Artist • 78M plays", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400", "3:40", "Relax", "78M")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE), // Soft Cloud Sky Light
                        Color(0xFFF1F5F9), // Light Cloud Slate
                        Color(0xFFF8FAFC), // Pure Soft Cloud
                        Color(0xFFFFFFFF)  // Clean White
                    )
                )
            )
            .nestedScroll(topBarNestedScrollConnection)
    ) {
        // AMBIENT CLOUD THEME BLUR GLOWS
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-50).dp, y = 30.dp)
                .clip(CircleShape)
                .background(Color(0xFFBAE6FD).copy(alpha = 0.50f))
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 160.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDD6FE).copy(alpha = 0.45f))
                .blur(50.dp)
        )

        // 1. DYNAMIC CURVED GLASSY TOP BAR HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f)
                .graphicsLayer {
                    translationY = animatedEntranceY + topBarOffsetHeightPx
                    alpha = animatedEntranceAlpha
                }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                color = Color.White.copy(alpha = 0.70f),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.80f),
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.35f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(bottom = 12.dp)
                    ) {
                    // Title and action icons row with inline Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SimpMusic",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Good Morning",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Glassy Search Bar
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(start = 10.dp, end = 6.dp)
                                .clickable { isSearchScreenOpen = true },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (musicSearchQuery.isEmpty()) "Search music..." else musicSearchQuery,
                                    color = if (musicSearchQuery.isEmpty()) Color(0xFF64748B) else Color(0xFF0F172A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = "Open Search",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Glassy Action Buttons (Notifications & History)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.75f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(34.dp)
                            ) {
                                IconButton(
                                    onClick = { Toast.makeText(context, "Notifications", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.75f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(34.dp)
                            ) {
                                IconButton(
                                    onClick = { Toast.makeText(context, "Listening History", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "History",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Continuous Auto-Running Marquee Filter Chips Row (Pauses on touch, resumes automatically)
                    LazyRow(
                        state = topCategoryLazyRowState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(count = Int.MAX_VALUE) { index ->
                            val category = categories[index % categories.size]
                            val isSelected = currentCategory == category
                            Surface(
                                modifier = Modifier.clickable { currentCategory = category },
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.70f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.90f)
                                ),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = category,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // MAIN SCROLLABLE YOUTUBE MUSIC CONTENT
        LazyColumn(
            state = musicListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 160.dp, bottom = 140.dp)
        ) {
            // 4. QUICK PICKS SECTION (Horizontal Swipeable List)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick picks",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.clickable { }
                        ) {
                            Text(
                                text = "Play all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Swipeable horizontal column pages for quick picks
                    val quickPickPages = quickPicks.chunked(4)
                    val pagerState = rememberPagerState(pageCount = { quickPickPages.size })

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 16.dp
                    ) { pageIndex ->
                        val pageTracks = quickPickPages[pageIndex]
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pageTracks.forEach { track ->
                                val trackInteractionSource = remember { MutableInteractionSource() }
                                val trackIsPressed by trackInteractionSource.collectIsPressedAsState()
                                val trackScale by animateFloatAsState(if (trackIsPressed) 0.96f else 1.0f, label = "quickPickScale")
                                val isThisTrackPlaying = currentTrack?.id == track.id && isPlaying

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = trackScale
                                            scaleY = trackScale
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.88f),
                                    shadowElevation = 3.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = trackInteractionSource, indication = null) {
                                                currentTrack = track
                                                isPlaying = true
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box {
                                                Image(
                                                    painter = rememberAsyncImagePainter(track.albumArt),
                                                    contentDescription = track.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                                if (isThisTrackPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AnimatedEqualizerWave(tint = Color.White)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = track.title,
                                                        fontSize = 15.sp,
                                                        fontWeight = if (isThisTrackPlaying) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isThisTrackPlaying) Color(0xFF0284C7) else Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (isThisTrackPlaying) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        AnimatedEqualizerWave(tint = Color(0xFF0284C7))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = track.artist,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 5. FROM THE COMMUNITY (2x2 Collage Playlist Cards)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "From the community",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            CommunityCollageCard(
                                title = "All Time Favourites",
                                subtitle = "Arunava Choudhury • 221K views",
                                images = listOf(
                                    "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=300",
                                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                                    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
                                    "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300"
                                ),
                                onClick = {
                                    selectedPlaylist = PlaylistData(
                                        id = "comm1",
                                        title = "All Time Favourites",
                                        creator = "Arunava Choudhury",
                                        year = "Playlist • 2026",
                                        description = "No description",
                                        tracksCount = "38 tracks",
                                        images = listOf(
                                            "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=300",
                                            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                                            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
                                            "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300"
                                        ),
                                        tracks = sampleBanglaPlaylist.tracks
                                    )
                                }
                            )
                        }
                        item {
                            CommunityCollageCard(
                                title = "peace",
                                subtitle = "AYUSH PAUL • 1.1M views",
                                images = listOf(
                                    "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=300",
                                    "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
                                    "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=300",
                                    "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=300"
                                ),
                                onClick = {
                                    selectedPlaylist = PlaylistData(
                                        id = "comm2",
                                        title = "peace",
                                        creator = "AYUSH PAUL",
                                        year = "Playlist • 2026",
                                        description = "No description",
                                        tracksCount = "38 tracks",
                                        images = listOf(
                                            "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=300",
                                            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
                                            "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=300",
                                            "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=300"
                                        ),
                                        tracks = sampleBanglaPlaylist.tracks
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 6. FORGOTTEN FAVORITES (Hero Banner Card)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Forgotten favorites",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                currentTrack = MusicTrack(
                                    id = "ff1",
                                    title = "Die with a smile x awari song (Full Mashup)",
                                    artist = "Likey • 328K views",
                                    albumArt = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800",
                                    duration = "4:30",
                                    category = "Mashup",
                                    views = "328K"
                                )
                                isPlaying = true
                            }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800"),
                            contentDescription = "Forgotten Favorite",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Die with a smile x awari song (Full Mashup)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Likey • 328K views",
                                fontSize = 12.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 7. FRESH FINDS, OLD FAVORITES (Mix Cards)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Fresh finds, old favorites",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            MixCard(
                                title = "Replay Mix",
                                subtitle = "Madhur Sharma, Tarun Sharma...",
                                imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=400"
                            )
                        }
                        item {
                            MixCard(
                                title = "Archive Mix",
                                subtitle = "Sachet Parampara, Roop Kuma...",
                                imageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400"
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 8. LONG LISTENS SECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Long listens",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        longListens.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        currentTrack = track
                                        isPlaying = true
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(track.albumArt),
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = track.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = track.artist,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 9. HEARD IN SHORTS SECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Heard in Shorts",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.clickable { }
                        ) {
                            Text(
                                text = "Play all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        shortsTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        currentTrack = track
                                        isPlaying = true
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(track.albumArt),
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = track.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = track.artist,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // STICKY BOTTOM MINI PLAYER (Glassy White Cloud Style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 60.dp, start = 8.dp, end = 8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .clickable { viewModel.isFullMusicPlayerOpen = true },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(currentTrack.albumArt),
                            contentDescription = currentTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentTrack.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack.artist,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { isPlaying = !isPlaying }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // FULL SCREEN PLAYER OVERLAY (Rich expanded player)
        AnimatedVisibility(
            visible = isFullPlayerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.zIndex(90f)
        ) {
            FullMusicPlayerOverlay(
                track = currentTrack,
                isPlaying = isPlaying,
                onClose = { viewModel.isFullMusicPlayerOpen = false },
                onTogglePlay = { isPlaying = !isPlaying },
                animatedVisibilityScope = this@AnimatedVisibility
            )
        }

        // PLAYLIST DETAIL OVERLAY (Matching Screenshot 2)
        AnimatedVisibility(
            visible = selectedPlaylist != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            selectedPlaylist?.let { playlist ->
                PlaylistDetailOverlay(
                    playlist = playlist,
                    onClose = {
                        selectedPlaylist = null
                        viewModel.isPlaylistOverlayOpen = false
                    },
                    onTrackSelect = { track ->
                        currentTrack = track
                        isPlaying = true
                    },
                    onSearchClick = {
                        isSearchScreenOpen = true
                    }
                )
            }
        }

        // FULL SEARCH SCREEN OVERLAY WITH SIDE SLIDE ANIMATION
        AnimatedVisibility(
            visible = isSearchScreenOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 380, easing = EaseOutQuart)
            ) + fadeIn(animationSpec = tween(380)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 320, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(320)),
            modifier = Modifier.fillMaxSize().zIndex(150f)
        ) {
            MusicSearchScreenOverlay(
                onClose = { isSearchScreenOpen = false },
                onTrackSelect = { track ->
                    currentTrack = track
                    isPlaying = true
                    isSearchScreenOpen = false
                },
                viewModel = viewModel
            )
        }
    }
}

// COMMUNITY COLLAGE CARD COMPONENT
@Composable
fun CommunityCollageCard(
    title: String,
    subtitle: String,
    images: List<String>,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1.0f, label = "collageCardScale")

    Column(
        modifier = Modifier
            .width(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    Image(
                        painter = rememberAsyncImagePainter(images.getOrNull(0) ?: ""),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Image(
                        painter = rememberAsyncImagePainter(images.getOrNull(1) ?: ""),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                Row(modifier = Modifier.weight(1f)) {
                    Image(
                        painter = rememberAsyncImagePainter(images.getOrNull(2) ?: ""),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Image(
                        painter = rememberAsyncImagePainter(images.getOrNull(3) ?: ""),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// MIX CARD COMPONENT
@Composable
fun MixCard(
    title: String,
    subtitle: String,
    imageUrl: String
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            Text(
                text = "Mix",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// DYNAMIC LYRICS BOTTOM SHEET POPUP (Screenshot layout styled in Light Theme)
@Composable
fun DynamicLyricsSheet(
    track: MusicTrack,
    onClose: () -> Unit
) {
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

    val lyricsLines = remember(track.id) {
        listOf(
            "0:05" to "In the end, it's him and I",
            "0:12" to "He's out his head, I'm out my mind",
            "0:20" to "We got that love, the crazy kind",
            "0:28" to "I am his, and he is mine",
            "0:36" to "In the end, it's him and I",
            "0:45" to "Him and I",
            "0:52" to "Whoa-oh-oh-oh-oh",
            "1:01" to "Whoa-oh-oh-oh-oh",
            "1:10" to "In the end, it's him and I",
            "1:18" to "আমার মনের মিষ্টি সুরটা বাজুক তোকে ঘিরে",
            "1:26" to "রাতের তারায় স্বপ্ন সাজাই তোর ভালোবাসায়"
        )
    }

    var activeLineIndex by remember { mutableIntStateOf(4) }
    var sheetOffsetY by remember { mutableFloatStateOf(0f) }
    val lyricsListState = rememberLazyListState()

    // Auto-advance active lyrics line and scroll smoothly
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3500)
            activeLineIndex = (activeLineIndex + 1) % lyricsLines.size
        }
    }

    LaunchedEffect(activeLineIndex) {
        lyricsListState.animateScrollToItem(
            index = activeLineIndex,
            scrollOffset = -150
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.93f)
                .graphicsLayer {
                    translationY = sheetOffsetY
                }
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            color = Color.White,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Draggable Header Handle Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (sheetOffsetY > 100f) {
                                        onClose()
                                    }
                                    sheetOffsetY = 0f
                                },
                                onDragCancel = { sheetOffsetY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount > 0 || sheetOffsetY > 0) {
                                        sheetOffsetY = (sheetOffsetY + dragAmount).coerceAtLeast(0f)
                                        change.consume()
                                    }
                                }
                            )
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(5.dp)
                            .background(Color(0xFF94A3B8), CircleShape)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onClose) {
                            Text(
                                text = "Done",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }

                // Centered Lyrics Scrollable Content
                LazyColumn(
                    state = lyricsListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    contentPadding = PaddingValues(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(lyricsLines) { index, (_, text) ->
                        val isActive = index == activeLineIndex

                        Text(
                            text = text,
                            fontSize = if (isActive) 22.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) Color(0xFF0284C7) else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = if (isActive) 30.sp else 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeLineIndex = index }
                                .padding(horizontal = 20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Pill: "Remove lyrics"
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Remove lyrics",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                // Player Progress & Audio Visualizer Control Bar (as seen in screenshot)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Pill: Timer counter "30"
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "30",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Middle Progress Dots Slider
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .weight(1f)
                                    .background(Color(0xFFE2E8F0), CircleShape)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF94A3B8), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF0284C7), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF94A3B8), CircleShape)
                                    )
                                }
                            }
                        }

                        // Right Stop Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onClose() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(0xFF475569), RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audio Waveform Equalizer Bar (Blends naturally with popup background)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedEqualizerWave(tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synced Dynamic Live Audio",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }
        }
    }
}

// FULL SCREEN PLAYER OVERLAY (Light Theme with Scrollable Artist Profile & Famous Tracks)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullMusicPlayerOverlay(
    track: MusicTrack,
    isPlaying: Boolean,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onSelectTrack: (MusicTrack) -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        onClose()
    }
    val context = LocalContext.current
    var activeTrack by remember(track) { mutableStateOf(track) }
    var isLoved by remember { mutableStateOf(false) }
    var isArtistFollowed by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0.12f) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showArtistInfoDialog by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var playerDragOffsetY by remember { mutableFloatStateOf(0f) }

    val lazyListState = rememberLazyListState()
    val isScrolledDown by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 100
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.isAppearanceLightStatusBars = true
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.statusBars())
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val popularSongs = remember(activeTrack) {
        listOf(
            MusicTrack("p_1", "Toota Jo Kabhi Tara", "Sachin-Jigar, Sumedha Karmahe & Atif Aslam", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400", "5:05", "Romantic", "150M"),
            MusicTrack("p_2", "Ruposh (Original Score)", "Wajhi Farooki & Atif Aslam", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400", "4:20", "Romantic", "211M"),
            MusicTrack("p_3", "Jeena Haraam", "Vishal Mishra & Atif Aslam", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400", "3:30", "Chill", "210M"),
            MusicTrack("p_4", "Aaina Mon Bhanga", "Zubeen Garg, Jeet Gannguli & Atif Aslam", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400", "4:45", "Melody", "95M"),
            MusicTrack("p_5", "Jiboner Ayna", "Parvez & Atif Aslam", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400", "4:12", "Romantic", "115M"),
            MusicTrack("p_6", "Jhol", "Maanu, Annural Khalid & Atif Aslam", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400", "3:10", "Relax", "808M")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = playerDragOffsetY
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0)
                    )
                )
            )
    ) {
        // Full screen White Smoke Background Effect
        AnimatedWhiteSmokeOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // STICKY TOP MINI MUSIC BAR (Appears smoothly when scrolled down)
        AnimatedVisibility(
            visible = isScrolledDown,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(20f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color.White.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.94f),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(0)
                                }
                            }
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Collapse",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Image(
                            painter = rememberAsyncImagePainter(activeTrack.albumArt),
                            contentDescription = activeTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTrack.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeTrack.artist,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(
                            onClick = { Toast.makeText(context, "Casting music", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // MAIN SCROLLABLE CONTENT (PLAYER VIEW + ARTIST PROFILE & HITS)
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ITEM 0: MAIN FULL MUSIC PLAYER VIEW (Fills 100% height so item 1 is hidden below fold)
            item {
                Column(
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // TOP HEADER (Collapse & Cast - only when at top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (playerDragOffsetY > 120f) {
                                            onClose()
                                        }
                                        playerDragOffsetY = 0f
                                    },
                                    onDragCancel = { playerDragOffsetY = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        if (dragAmount > 0 || playerDragOffsetY > 0) {
                                            playerDragOffsetY = (playerDragOffsetY + dragAmount).coerceAtLeast(0f)
                                            change.consume()
                                        }
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Collapse",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "Playing from Playlist",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )

                        IconButton(
                            onClick = { Toast.makeText(context, "Casting music", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // ALBUM ARTWORK BANNER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 8.dp)
                            .aspectRatio(1f)
                            .shadow(14.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(activeTrack.albumArt),
                            contentDescription = activeTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.08f),
                                            Color(0xFFF1F5F9).copy(alpha = 0.40f)
                                        )
                                    )
                                )
                        )
                    }

                    // TITLE & ARTIST DETAILS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTrack.title,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeTrack.artist,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // PROGRESS BAR & TIMESTAMPS
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0F172A),
                                activeTrackColor = Color(0xFF0F172A),
                                inactiveTrackColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0:42",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = activeTrack.duration,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // PLAYBACK CONTROLS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(62.dp)
                                .clickable { onTogglePlay() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // BOTTOM UTILITY TOOLBAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showArtistInfoDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = { showLyricsSheet = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Lyrics",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = { isLoved = !isLoved },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLoved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Love",
                                    tint = if (isLoved) Color(0xFFEF4444) else Color(0xFF475569),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    sleepTimerMinutes = if (sleepTimerMinutes == 0) 15 else 0
                                    val msg = if (sleepTimerMinutes > 0) "Timer: 15m" else "Timer Off"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerMinutes > 0) Color(0xFF0284C7) else Color(0xFF475569),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Queue",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // ITEM 1: ARTIST PROFILE SECTION
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Artist Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Image(
                                    painter = rememberAsyncImagePainter(activeTrack.albumArt),
                                    contentDescription = activeTrack.artist,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFF0284C7), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7))
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verified",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "VERIFIED ARTIST",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0284C7),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeTrack.artist,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "18.5M Monthly Listeners",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Button(
                                onClick = {
                                    isArtistFollowed = !isArtistFollowed
                                    val msg = if (isArtistFollowed) "Following ${activeTrack.artist}" else "Unfollowed ${activeTrack.artist}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isArtistFollowed) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                                    contentColor = if (isArtistFollowed) Color(0xFF0F172A) else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = if (isArtistFollowed) "Following" else "+ Follow",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Artist Bio Snippet
                        Text(
                            text = "About the Artist",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${activeTrack.artist} is one of South Asia's most iconic playback singers & music composers, best known for soulful acoustic performances, chart-topping romantic anthems, and over 20 billion streams worldwide.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ITEM 2: POPULAR SONGS BY THIS ARTIST
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Popular Songs by Artist",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "See All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Showing full discography", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    popularSongs.forEachIndexed { index, popTrack ->
                        val isThisActive = activeTrack.title == popTrack.title
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isThisActive) Color(0xFFF0F9FF) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isThisActive) Color(0xFFBAE6FD) else Color(0xFFF1F5F9)
                            ),
                            shadowElevation = if (isThisActive) 4.dp else 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    activeTrack = popTrack
                                    onSelectTrack(popTrack)
                                    if (!isPlaying) onTogglePlay()
                                    Toast.makeText(context, "Playing ${popTrack.title}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(popTrack.albumArt),
                                    contentDescription = popTrack.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = popTrack.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isThisActive) Color(0xFF0284C7) else Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${popTrack.views} plays • ${popTrack.duration}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (isThisActive) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isThisActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = if (isThisActive) Color.White else Color(0xFF0F172A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ITEM 3: FEATURED ALBUMS ROW (Edge-To-Edge horizontal scroll)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "Featured Albums & Singles",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(sampleBanglaPlaylist.tracks.take(4)) { albumTrack ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                shadowElevation = 3.dp,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable {
                                        activeTrack = albumTrack
                                        onSelectTrack(albumTrack)
                                        if (!isPlaying) onTogglePlay()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Image(
                                        painter = rememberAsyncImagePainter(albumTrack.albumArt),
                                        contentDescription = albumTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = albumTrack.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = albumTrack.category,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM SPACING FOR NAV PADDING
            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }

        // ARTIST DETAILS POPUP DIALOG
        if (showArtistInfoDialog) {
            AlertDialog(
                onDismissRequest = { showArtistInfoDialog = false },
                title = {
                    Text(
                        text = track.artist,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "${track.title} • Track Details",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Monthly Listeners: 3.2M",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${track.artist} is a renowned playback singer and music producer. Known for chart-topping romantic melodies, acoustic live performances, and viral music worldwide.",
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showArtistInfoDialog = false }) {
                        Text("Close", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // DYNAMIC LYRICS BOTTOM SHEET POPUP OVERLAY
        AnimatedVisibility(
            visible = showLyricsSheet,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutLinearInEasing)) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.zIndex(50f)
        ) {
            DynamicLyricsSheet(
                track = track,
                onClose = { showLyricsSheet = false }
            )
        }
    }
}

// ANIMATED WHITE SMOKE OVERLAY FOR HERO BANNER
@Composable
fun AnimatedWhiteSmokeOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WhiteSmokeAnimation")

    val smokeOffset1 by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smokeOffset1"
    )

    val smokeOffset2 by infiniteTransition.animateFloat(
        initialValue = 25f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smokeOffset2"
    )

    val smokeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smokeAlpha"
    )

    val smokeScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smokeScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Floating Smoke Cloud Layer 1
        Box(
            modifier = Modifier
                .fillMaxWidth(1.2f)
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .offset(x = smokeOffset1.dp, y = (smokeOffset2 / 2).dp)
                .graphicsLayer {
                    scaleX = smokeScale
                    scaleY = smokeScale
                    alpha = smokeAlpha
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.90f),
                            Color(0xFFF1F5F9).copy(alpha = 0.60f),
                            Color.Transparent
                        )
                    )
                )
                .blur(48.dp)
        )

        // Floating Smoke Cloud Layer 2
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(160.dp)
                .align(Alignment.Center)
                .offset(x = (-smokeOffset2 * 1.5f).dp, y = smokeOffset1.dp)
                .graphicsLayer {
                    scaleX = 1.1f / smokeScale
                    alpha = smokeAlpha * 0.8f
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFE2E8F0).copy(alpha = 0.50f),
                            Color.Transparent
                        )
                    )
                )
                .blur(38.dp)
        )

        // Floating Smoke Particle Layer 3
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = smokeOffset1.dp, y = smokeOffset2.dp)
                .graphicsLayer {
                    scaleX = smokeScale
                    alpha = smokeAlpha * 0.5f
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
                .blur(50.dp)
        )
    }
}

// ANIMATED EQUALIZER WAVEFORM INDICATOR
@Composable
fun AnimatedEqualizerWave(
    tint: Color = Color(0xFF0284C7),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Equalizer")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Row(
        modifier = modifier.size(width = 16.dp, height = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(bar1Height)
                .background(tint, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(bar2Height)
                .background(tint, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(bar3Height)
                .background(tint, RoundedCornerShape(1.dp))
        )
    }
}

// PLAYLIST DETAIL OVERLAY (White Smoke Theme, Seamless Blend Large Banner, Animated Smoke & Frosted Glassy Controls)
@Composable
fun PlaylistDetailOverlay(
    playlist: PlaylistData,
    onClose: () -> Unit,
    onTrackSelect: (MusicTrack) -> Unit,
    onSearchClick: () -> Unit = {}
) {
    BackHandler(enabled = true) {
        onClose()
    }

    val context = LocalContext.current
    var isLiked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF), // Pure White Top
                        Color(0xFFF8FAFC), // Soft Off-White Cloud
                        Color(0xFFF1F5F9), // Light Cloud Smoke
                        Color(0xFFE2E8F0)  // Subtle Smoke Shade
                    )
                )
            )
    ) {
        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp)
        ) {
            // 1. EXTRA LARGE HERO COLLAGE BANNER (Seamessly Blended into Background with Animated White Smoke)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    // 2x2 Image Collage Banner (Edge-to-Edge)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            Image(
                                painter = rememberAsyncImagePainter(playlist.images.getOrElse(0) { "" }),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            Image(
                                painter = rememberAsyncImagePainter(playlist.images.getOrElse(1) { "" }),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            Image(
                                painter = rememberAsyncImagePainter(playlist.images.getOrElse(2) { "" }),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            Image(
                                painter = rememberAsyncImagePainter(playlist.images.getOrElse(3) { "" }),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    // SEAMLESS BACKGROUND BLEND GRADIENT OVERLAY
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color(0xFFF8FAFC).copy(alpha = 0.70f),
                                        Color(0xFFF8FAFC)
                                    )
                                )
                            )
                    )

                    // LIVE ANIMATED WHITE SMOKE EFFECT FLOATING OVER BANNER
                    AnimatedWhiteSmokeOverlay(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Title, Creator & Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title & Creator
                    Text(
                        text = playlist.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playlist.creator,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = playlist.year,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Controls: Shuffle, Play, Download
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.90f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    Toast.makeText(context, "Shuffle mode enabled", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Play Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .height(52.dp)
                                .widthIn(min = 145.dp)
                                .clickable {
                                    if (playlist.tracks.isNotEmpty()) {
                                        onTrackSelect(playlist.tracks.first())
                                    }
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Play",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.90f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    Toast.makeText(context, "Downloading playlist...", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Download",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Description and track count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = playlist.description,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = playlist.tracksCount,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFCBD5E1).copy(alpha = 0.6f))
                }
            }

            // 2. TRACKS LIST (With Animated Equalizer Waves & Selection Feedback)
            items(playlist.tracks) { track ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(if (isPressed) 0.97f else 1.0f, label = "trackScale")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(interactionSource = interactionSource, indication = null) { onTrackSelect(track) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(track.albumArt),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { Toast.makeText(context, "Options for ${track.title}", Toast.LENGTH_SHORT).show() }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. TOP FLOATING GLASSY FROSTED NAVIGATION HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(20f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glassy Circular Back Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.80f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onClose() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Glassy Frosted Pill Container with Love, Search, 3-dot
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.80f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Love",
                            tint = if (isLiked) Color(0xFFEF4444) else Color(0xFF0F172A),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    isLiked = !isLiked
                                    Toast.makeText(context, if (isLiked) "Added to Liked Playlists" else "Removed from Liked", Toast.LENGTH_SHORT).show()
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    onSearchClick()
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    Toast.makeText(context, "Playlist options", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        }
    }
}

// FULL MUSIC SEARCH OVERLAY (Side-Slide Animation, Recent Searches, Live Query Filtering)
@Composable
fun MusicSearchScreenOverlay(
    onClose: () -> Unit,
    onTrackSelect: (MusicTrack) -> Unit,
    viewModel: CloudihubViewModel
) {
    BackHandler(enabled = true) {
        onClose()
    }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val allTracks = remember {
        listOf(
            MusicTrack("t1", "Jiboner Ayna", "Parvez", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400", "4:12", "Romantic", "15M"),
            MusicTrack("t2", "Bolbona Go Ar Kono Din - 21", "Baul Sukumar", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400", "5:04", "Folk", "28M"),
            MusicTrack("t3", "Aaina Mon Bhanga", "Zubeen Garg, Jeet Gannguli & Priyo Chattopadhyay", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400", "4:45", "Melody", "42M"),
            MusicTrack("t4", "Chokhe Shanti Lage Amar", "Sathi Khan", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400", "3:58", "Pop", "18M"),
            MusicTrack("t5", "Jeena Haraam", "Vishal Mishra", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400", "3:30", "Chill", "35M"),
            MusicTrack("t6", "Ruposh (Original Score)", "Wajhi Farooki", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400", "4:20", "Romantic", "211M"),
            MusicTrack("t7", "Toota Jo Kabhi Tara", "Sachin-Jigar, Sumedha Karmahe & Atif Aslam", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=400", "5:05", "Romantic", "150M"),
            MusicTrack("t8", "Anyayo", "Aneesh & Krtin Kay", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400", "3:15", "Chill", "2.6M"),
            MusicTrack("t9", "Kusu Kusu", "Zahrah S Khan & Dev Negi", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400", "3:45", "Party", "907M"),
            MusicTrack("t10", "Jhol", "Maanu & Annural Khalid", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400", "3:10", "Relax", "808M"),
            MusicTrack("qp5", "no signal", "juggsi & kyra", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400", "2:50", "Chill", "12M"),
            MusicTrack("hs1", "Traag (feat. Jozo & Kraantje Pappie)", "Bizzey", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400", "3:02", "Party", "652M"),
            MusicTrack("hs2", "Phagooner Mohonaye", "Various Artist", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400", "3:40", "Relax", "78M")
        )
    }

    val filteredTracks = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFE0F2FE),
                        Color(0xFFF1F5F9)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Search Top Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A)
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.addSearchQueryToHistory(searchQuery)
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            cursorBrush = SolidColor(Color(0xFF0284C7)),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search tracks, artists, albums...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.startVoiceSearch() },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }
            }

            // Search Content Body
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (searchQuery.isBlank()) {
                    // Recent Searches Section
                    if (viewModel.recentSearches.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Clear All",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.clickable { viewModel.clearSearchHistory() }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(viewModel.recentSearches) { historyQuery ->
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.clickable {
                                                searchQuery = historyQuery
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = historyQuery,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1E293B)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { viewModel.removeSearchQueryFromHistory(historyQuery) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Trending Music Searches Section
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "Trending Music Searches",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val trending = listOf("Atif Aslam Hits", "Arijit Singh", "Lofi Sunset", "Bangla Acoustic", "Toota Jo Kabhi", "Coke Studio", "Party Beats")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(trending) { tag ->
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF0F9FF),
                                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                        modifier = Modifier.clickable {
                                            searchQuery = tag
                                            viewModel.addSearchQueryToHistory(tag)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tag,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0369A1)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Search Results List
                    if (filteredTracks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MusicOff,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No songs found for \"$searchQuery\"",
                                        fontSize = 14.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Found ${filteredTracks.size} tracks",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(filteredTracks) { track ->
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSearchQueryToHistory(searchQuery)
                                        onTrackSelect(track)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(track.albumArt),
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.duration}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE0F2FE),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
