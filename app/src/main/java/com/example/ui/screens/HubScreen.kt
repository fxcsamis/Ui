package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import coil.compose.SubcomposeAsyncImage
import com.example.ui.CloudVideo
import com.example.ui.CloudihubViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

data class SocialPlatform(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val brandColor: Color,
    val accentColor: Color,
    val supportedFormats: String,
    val logoUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    var selectedPlatform by remember { mutableStateOf<SocialPlatform?>(null) }
    val scope = rememberCoroutineScope()

    val platforms = listOf(
        SocialPlatform(
            name = "YouTube",
            description = "High-speed UHD video extractor",
            icon = Icons.Default.PlayArrow,
            brandColor = Color(0xFFEF4444),
            accentColor = Color(0xFFFCA5A5),
            supportedFormats = "MP4, MKV, MP3",
            logoUrl = "https://i.postimg.cc/PJYCFjjx/YOUTUBE.jpg"
        ),
        SocialPlatform(
            name = "Instagram",
            description = "Reels & stories media fetcher",
            icon = Icons.Default.CameraAlt,
            brandColor = Color(0xFFEC4899),
            accentColor = Color(0xFFFBCFE8),
            supportedFormats = "MP4, JPG",
            logoUrl = "https://i.postimg.cc/Y0h34jYP/Instagram.jpg"
        ),
        SocialPlatform(
            name = "TikTok",
            description = "Clean sound & video loop saver",
            icon = Icons.Default.Audiotrack,
            brandColor = Color(0xFF111827),
            accentColor = Color(0xFFF43F5E),
            supportedFormats = "MP4 (No Watermark)",
            logoUrl = "https://i.postimg.cc/GtHQB9GN/tiktok.jpg"
        ),
        SocialPlatform(
            name = "Facebook",
            description = "FHD social stream grabber",
            icon = Icons.Default.ThumbUp,
            brandColor = Color(0xFF3B82F6),
            accentColor = Color(0xFF93C5FD),
            supportedFormats = "MP4, MP3",
            logoUrl = "https://i.postimg.cc/PxcYDzmF/Face-Book-(1).jpg"
        ),
        SocialPlatform(
            name = "Pinterest",
            description = "Visual layout image & video saver",
            icon = Icons.Default.Bookmark,
            brandColor = Color(0xFFDC2626),
            accentColor = Color(0xFFFECACA),
            supportedFormats = "MP4, PNG",
            logoUrl = "https://i.postimg.cc/nz5BmY4b/Pinterest.jpg"
        ),
        SocialPlatform(
            name = "Snapchat",
            description = "Stories & spotlight media fetcher",
            icon = Icons.Default.ChatBubble,
            brandColor = Color(0xFFCA8A04),
            accentColor = Color(0xFFFEF08A),
            supportedFormats = "MP4, JPG",
            logoUrl = "https://i.postimg.cc/0Q3DSdpV/Snapchat-(1).jpg"
        ),
        SocialPlatform(
            name = "Twitter / X",
            description = "Fast thread video & gif archiver",
            icon = Icons.Default.DeviceHub,
            brandColor = Color(0xFF0F172A),
            accentColor = Color(0xFF94A3B8),
            supportedFormats = "MP4, GIF",
            logoUrl = "https://i.postimg.cc/SK1M07tn/Twitter.jpg"
        ),
        SocialPlatform(
            name = "Twitch clip",
            description = "Gaming highlight & clip clipper",
            icon = Icons.Default.Videocam,
            brandColor = Color(0xFF9146FF),
            accentColor = Color(0xFFE5D5FF),
            supportedFormats = "MP4",
            logoUrl = "https://i.postimg.cc/PqVwGzFN/Twitch-logo.jpg"
        ),
        SocialPlatform(
            name = "VK",
            description = "Fast social player extractor",
            icon = Icons.Default.Share,
            brandColor = Color(0xFF4C75A3),
            accentColor = Color(0xFFD3DFEE),
            supportedFormats = "MP4, MP3",
            logoUrl = "https://i.postimg.cc/ZqsdkFDC/VK.jpg"
        ),
        SocialPlatform(
            name = "Bilibili",
            description = "High-speed anime clip video loader",
            icon = Icons.Default.Tv,
            brandColor = Color(0xFF00AEEC),
            accentColor = Color(0xFFB3E7FC),
            supportedFormats = "MP4, FLV",
            logoUrl = "https://i.postimg.cc/FH9d7nMz/BILIBILI.jpg"
        ),
        SocialPlatform(
            name = "Bluesky",
            description = "Decentralized post media downloader",
            icon = Icons.Default.Cloud,
            brandColor = Color(0xFF0560FC),
            accentColor = Color(0xFFB3CFFF),
            supportedFormats = "MP4, PNG",
            logoUrl = "https://i.postimg.cc/9QWRrN6M/BLUE-SKY.jpg"
        ),
        SocialPlatform(
            name = "Rutube",
            description = "High definition stream saver",
            icon = Icons.Default.Tv,
            brandColor = Color(0xFFE11D48),
            accentColor = Color(0xFFFECDD3),
            supportedFormats = "MP4, MKV",
            logoUrl = "https://i.postimg.cc/6p92y1kQ/fc544c15-f62e-4bf5-a01a-a9877592d018.jpg"
        ),
        SocialPlatform(
            name = "Dailymotion",
            description = "Universal web player video saver",
            icon = Icons.Default.PlayArrow,
            brandColor = Color(0xFF0066DC),
            accentColor = Color(0xFFB3D1F9),
            supportedFormats = "MP4, MKV",
            logoUrl = "https://i.postimg.cc/ZqX5FfMB/Dailymotion.jpg"
        ),
        SocialPlatform(
            name = "Tumblr",
            description = "Blog media post archive utility",
            icon = Icons.Default.FormatQuote,
            brandColor = Color(0xFF35465C),
            accentColor = Color(0xFFCBD5E1),
            supportedFormats = "MP4, GIF, PNG",
            logoUrl = "https://i.postimg.cc/CK9xHvQR/bfc1b4ea-0031-4038-b36a-b751c14e7c92.jpg"
        ),
        SocialPlatform(
            name = "OK.ru",
            description = "Social network video loop downloader",
            icon = Icons.Default.People,
            brandColor = Color(0xFFF58220),
            accentColor = Color(0xFFFFD9B3),
            supportedFormats = "MP4, MP3",
            logoUrl = "https://i.postimg.cc/fTWDXnR7/OK-ru.jpg"
        ),
        SocialPlatform(
            name = "Loom",
            description = "Workspace video sharing recorder saver",
            icon = Icons.Default.Videocam,
            brandColor = Color(0xFF625DF5),
            accentColor = Color(0xFFDDD6FE),
            supportedFormats = "MP4",
            logoUrl = "https://i.postimg.cc/T2JTpYf4/loom.jpg"
        ),
        SocialPlatform(
            name = "SoundCloud",
            description = "High-fidelity cloud music grabber",
            icon = Icons.Default.MusicNote,
            brandColor = Color(0xFFFF5500),
            accentColor = Color(0xFFFFCCB3),
            supportedFormats = "MP3, WAV",
            logoUrl = "https://i.postimg.cc/RVC4f5ZQ/sound-cloud.jpg"
        )
    )

    val gridState = rememberLazyGridState()

    val density = LocalDensity.current
    var topBarOffsetHeightPx by remember { mutableStateOf(0f) }
    val maxUpOffsetPx = with(density) { (-10.dp).toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = topBarOffsetHeightPx + delta
                topBarOffsetHeightPx = newOffset.coerceIn(maxUpOffsetPx, 0f)
                return Offset.Zero
            }
        }
    }

    val isAtTop = remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE), // Soft Cloud Light Sky Top
                        Color(0xFFF1F5F9), // Light Cloud White Slate
                        Color(0xFFF8FAFC), // Pure Soft Cloud
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        // --- AMBIENT STATIC CLOUD PUFF GLOWS ---
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-40).dp, y = 30.dp)
                .clip(CircleShape)
                .background(Color(0xFFBAE6FD).copy(alpha = 0.45f))
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 140.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDD6FE).copy(alpha = 0.40f))
                .blur(50.dp)
        )

        // --- COLLAPSIBLE TOP APP BAR ---
        Box(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = topBarOffsetHeightPx.roundToInt()) }
                .fillMaxWidth()
                .zIndex(10f) // Floating above the grid cards
        ) {
            if (!isAtTop.value) {
                // Frosted blur backdrop layers for white cloud theme
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(20.dp)
                        .background(Color.White.copy(alpha = 0.92f))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            width = 0.5.dp,
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Media Download Hub",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Seamless video, audio & loop extractor for 19+ sources",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // --- GRID CONTENT (Scrolls underneath the sticky blurred TopAppBar) ---
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = 110.dp, // Give precise space for header
                bottom = 120.dp,
                start = 20.dp,
                end = 20.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(platforms, key = { it.name }) { platform ->
                SocialCard(
                    platform = platform,
                    onClick = { selectedPlatform = platform }
                )
            }
        }

        // --- BOTTOM POPUP BOTTOM SHEET FLOW ---
        if (selectedPlatform != null) {
            HubPopupDialog(
                platform = selectedPlatform!!,
                allPlatforms = platforms,
                viewModel = viewModel,
                onDismiss = { selectedPlatform = null },
                onSelectPlatform = { newPlatform -> selectedPlatform = newPlatform }
            )
        }
    }
}

@Composable
fun SocialCard(
    platform: SocialPlatform,
    onClick: () -> Unit
) {
    // Beautiful large card with custom asymmetrical cuts and vibrant gradient highlights
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp)
            .shadow(4.dp, RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp))
            .border(
                width = 1.dp, 
                color = platform.brandColor.copy(alpha = 0.25f), 
                shape = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp)
            )
            .clip(RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp))
            .clickable { onClick() }
            .testTag("social_card_${platform.name}"),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Branded Platform Icon Bubble using Original High Quality Logo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = platform.logoUrl,
                        contentDescription = platform.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = {
                            Icon(
                                imageVector = platform.icon,
                                contentDescription = platform.name,
                                tint = platform.brandColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }

                // Tiny supported pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(platform.brandColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "READY",
                        color = platform.brandColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = platform.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = platform.description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = platform.supportedFormats,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = platform.brandColor,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubPopupDialog(
    platform: SocialPlatform,
    allPlatforms: List<SocialPlatform> = emptyList(),
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit,
    onSelectPlatform: (SocialPlatform) -> Unit = {}
) {
    // Fixed: Starts completely empty and automatically trims typed/pasted content
    var urlInput by remember { mutableStateOf("") }
    var extractionStep by remember { mutableStateOf(0) } // 0: Input, 1: Extracting/Loading, 2: Preview & Options
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Download Settings States
    var selectedFormat by remember { mutableStateOf("MP4") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedBitrate by remember { mutableStateOf("320 kbps") }
    var selectedFramerate by remember { mutableStateOf("60 fps") }

    // Auto-transition to Step 2 when direct CDN stream extraction completes
    LaunchedEffect(viewModel.isExtracting) {
        if (!viewModel.isExtracting && extractionStep == 1) {
            if (viewModel.extractedVideoForHub != null) {
                extractionStep = 2
            } else {
                extractionStep = 0
            }
        }
    }

    // Consistent Cloud-theme color palette used for all popups
    val cloudThemeColor = Color(0xFF0284C7)
    val cloudLightBg = Color(0xFFE0F2FE)
    val cloudTextDark = Color(0xFF0F172A)
    val cloudSubtitle = Color(0xFF64748B)

    val dismissWithAnimation = {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {

                        // --- HEADER (Single Cloud Theme Color) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(cloudLightBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SubcomposeAsyncImage(
                                        model = platform.logoUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        error = {
                                            Icon(
                                                imageVector = platform.icon,
                                                contentDescription = null,
                                                tint = cloudThemeColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${platform.name} Portal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cloudTextDark
                                )
                            }

                            IconButton(
                                onClick = { dismissWithAnimation() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = cloudSubtitle,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable dynamic content container
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        AnimatedContent(
                            targetState = extractionStep,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "PopupStateCrossfade"
                        ) { step ->
                            when (step) {
                                0 -> {
                                    // --- STEP 0: LINK INPUT SCREEN ---
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // URL Input Textbox: Perfectly compact, auto-trims, high contrast text
                                        OutlinedTextField(
                                            value = urlInput,
                                            onValueChange = { urlInput = it.trim() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("hub_url_input"),
                                            placeholder = { 
                                                Text(
                                                    text = "Paste video link here...", 
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 14.sp
                                                ) 
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Uri,
                                                imeAction = ImeAction.Done
                                            ),
                                            leadingIcon = {
                                                // Platform logo on the absolute left kinarai with minimal margins
                                                Box(
                                                    modifier = Modifier
                                                        .padding(start = 8.dp, end = 2.dp)
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    SubcomposeAsyncImage(
                                                        model = platform.logoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop,
                                                        error = {
                                                            Icon(
                                                                imageVector = platform.icon,
                                                                contentDescription = null,
                                                                tint = cloudThemeColor,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.getText()?.let {
                                                            urlInput = it.text.trim()
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentPaste,
                                                        contentDescription = "Paste Clipboard",
                                                        tint = cloudThemeColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color(0xFF0F172A), // Dark text to stand out
                                                unfocusedTextColor = Color(0xFF0F172A), // Dark text to stand out
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = cloudThemeColor,
                                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                                cursorColor = cloudThemeColor
                                            ),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A)
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Instructions and description placed BELOW the URL box, disappearing in Step 1 & 2
                                        Text(
                                            text = "Verify your video URL",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = cloudTextDark
                                        )
                                        Text(
                                            text = "Cloudihub will automatically verify & parse stream channels from ${platform.name} for extraction.",
                                            fontSize = 11.sp,
                                            color = cloudSubtitle,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = cloudLightBg),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = "Extraction Guide:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0369A1)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "1. Navigate to ${platform.name} and select the media clip.\n" +
                                                           "2. Click share & copy the target URL link.\n" +
                                                           "3. Paste the clean link into the input box above.\n" +
                                                           "4. Press 'Extract Stream' below to select dynamic settings.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF0284C7),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // --- RELATED & TOP RANKING PLATFORMS SUGGESTIONS ---
                                        Text(
                                            text = "🔥 Suggested Related & Top Ranking Platforms",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        val relatedTopPlatforms = remember(platform, allPlatforms) {
                                            allPlatforms.filter { it.name != platform.name }.take(6)
                                        }

                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            relatedTopPlatforms.forEach { relPlatform ->
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFF1F5F9),
                                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                    modifier = Modifier.clickable {
                                                        onSelectPlatform(relPlatform)
                                                        urlInput = ""
                                                        extractionStep = 0
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(18.dp)
                                                                .clip(CircleShape)
                                                                .background(relPlatform.brandColor),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = relPlatform.icon,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = relPlatform.name,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF334155)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // --- BASIC INFORMATIVE DESCRIPTION SECTION ---
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = platform.brandColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${platform.name} Overview & Extraction Info",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E293B)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "• Platform Focus: ${platform.description}\n" +
                                                           "• Available Output Formats: ${platform.supportedFormats}\n" +
                                                           "• Extraction engine supports high speed lossless 1080p/4K video streams, high bitrate audio (320kbps MP3), and clean link parsing without watermark.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF475569),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // --- NETWORK PLATFORMS SECTION ---
                                        Text(
                                            text = "🌐 Network Platform Logos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            allPlatforms.forEach { p ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clickable {
                                                            onSelectPlatform(p)
                                                            urlInput = ""
                                                            extractionStep = 0
                                                        }
                                                        .padding(2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                            .border(
                                                                width = if (p.name == platform.name) 2.dp else 1.dp,
                                                                color = if (p.name == platform.name) p.brandColor else Color(0xFFE2E8F0),
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        SubcomposeAsyncImage(
                                                            model = p.logoUrl,
                                                            contentDescription = p.name,
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape),
                                                            contentScale = ContentScale.Crop,
                                                            error = {
                                                                Icon(
                                                                    imageVector = p.icon,
                                                                    contentDescription = p.name,
                                                                    tint = p.brandColor,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = p.name.take(8),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF64748B),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Analyze Trigger Button (Cloud Theme)
                                        Button(
                                            onClick = {
                                                if (urlInput.isNotBlank()) {
                                                    scope.launch {
                                                        extractionStep = 1
                                                        viewModel.extractFromHubUrl(urlInput, platform.name)
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .testTag("analyze_button"),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = cloudThemeColor)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudSync,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Extract Stream",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                1 -> {
                                    // --- STEP 1: MODERN LOADING SCREEN ---
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        val infiniteLoading = rememberInfiniteTransition(label = "ExtractionLoading")
                                        val spinAngle by infiniteLoading.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "SpinLoading"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .rotate(spinAngle)
                                                .border(
                                                    width = 4.dp,
                                                    brush = Brush.sweepGradient(
                                                        listOf(cloudThemeColor, Color.Transparent)
                                                    ),
                                                    shape = CircleShape
                                                )
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Text(
                                            text = "Connecting Cloud Extractor...",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cloudTextDark
                                        )
                                        Text(
                                            text = "Extracting video, audio codecs, and metadata...",
                                            fontSize = 12.sp,
                                            color = cloudSubtitle,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)
                                        )
                                    }
                                }

                                2 -> {
                                    // --- STEP 2: VIDEO DOWNLOAD PREVIEW & LINE EXPANDABLE OPTIONS ---
                                    val extractedVideo = viewModel.extractedVideoForHub
                                    val videoTitle = extractedVideo?.title ?: "Extracted Video Content"
                                    val videoDuration = extractedVideo?.duration ?: "01:25"
                                    val videoSize = extractedVideo?.sizeMb ?: 18.5
                                    val videoImageUrl = extractedVideo?.imageUrl ?: "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=600"

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Video Player Cover Preview Card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF0F172A))
                                                .clickable {
                                                    extractedVideo?.let {
                                                        viewModel.playVideo(it)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = videoImageUrl,
                                                contentDescription = "Cover Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                error = {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Preview Play",
                                                            tint = Color.White.copy(alpha = 0.9f),
                                                            modifier = Modifier
                                                                .size(54.dp)
                                                                .border(2.dp, Color.White, CircleShape)
                                                                .padding(8.dp)
                                                        )
                                                    }
                                                }
                                            )

                                            // Play Icon overlay
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                    .border(2.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Preview Play",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomStart)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                        )
                                                    )
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = videoTitle,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Duration: $videoDuration • Size: ~$videoSize MB",
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Text(
                                            text = "DOWNLOAD OPTIONS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = cloudThemeColor,
                                            letterSpacing = 1.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Line-by-line expandable options list instead of boxes
                                        ExpandableOptionRow(
                                            label = "Select Format",
                                            options = listOf("MP4", "MKV", "MP3"),
                                            selected = selectedFormat,
                                            onSelect = { selectedFormat = it },
                                            themeColor = cloudThemeColor
                                        )

                                        ExpandableOptionRow(
                                            label = "Resolution quality",
                                            options = listOf("1080p", "720p", "480p"),
                                            selected = selectedResolution,
                                            onSelect = { selectedResolution = it },
                                            themeColor = cloudThemeColor
                                        )

                                        ExpandableOptionRow(
                                            label = "Audio Bitrate Speed",
                                            options = listOf("320 kbps", "256 kbps", "128 kbps"),
                                            selected = selectedBitrate,
                                            onSelect = { selectedBitrate = it },
                                            themeColor = cloudThemeColor
                                        )

                                        ExpandableOptionRow(
                                            label = "Video Framerate",
                                            options = listOf("60 fps", "30 fps"),
                                            selected = selectedFramerate,
                                            onSelect = { selectedFramerate = it },
                                            themeColor = cloudThemeColor
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        var downloadingState by remember { mutableStateOf(false) }

                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    downloadingState = true
                                                    delay(1200)
                                                    downloadingState = false
                                                    
                                                    val mockVideo = CloudVideo(
                                                        id = "cloud_extractor_${System.currentTimeMillis()}",
                                                        title = "[Downloaded] Nature Timelapse (${selectedResolution}_${selectedFramerate}_$selectedFormat)",
                                                        duration = "08:45",
                                                        creator = platform.name,
                                                        imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=600",
                                                        views = "1 view",
                                                        fileUrl = "https://example.com/v3.mp4",
                                                        sizeMb = 32.2
                                                    )
                                                    
                                                    val finalVideo = extractedVideo?.copy(
                                                        title = "${extractedVideo.title} [${selectedResolution}_${selectedFramerate}_$selectedFormat]",
                                                        fileUrl = viewModel.activeStreamingUrl
                                                    ) ?: mockVideo
                                                    viewModel.triggerVideoDownload(finalVideo)
                                                    dismissWithAnimation()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .testTag("download_now_button"),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = cloudThemeColor)
                                        ) {
                                            if (downloadingState) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Download Now",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                }
            }
        }
    }
}

@Composable
fun ExpandableOptionRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    themeColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selected,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand options",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 4.dp)
                ) {
                    options.forEach { option ->
                        val isSelected = option == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(option)
                                    expanded = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) themeColor else Color(0xFF334155)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = themeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
