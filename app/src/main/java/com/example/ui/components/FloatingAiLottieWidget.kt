package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.*
import com.airbnb.lottie.LottieComposition
import com.example.ui.CloudVideo
import com.example.ui.CloudihubViewModel
import com.example.ui.components.NavigationTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import android.widget.Toast
import kotlin.math.roundToInt

const val LOTTIE_OVERLAY_1_URL = "https://lottie.host/c661f04d-5ac1-4d65-89a2-402af70fe12e/fmj0sEg5jS.lottie"
const val LOTTIE_OVERLAY_2_URL = "https://lottie.host/2b5997b8-6637-4730-8755-ae2f561fc4ec/hrOvUSmkO3.lottie"
const val LOTTIE_AUDIO_3_URL = "https://lottie.host/5ce3f560-f8a8-4c82-88cf-b603d887e5c0/QBfdO9ZYAB.lottie"
const val LOTTIE_POPUP_URL = "https://lottie.host/505bf754-45e7-476b-9216-b6474f5aeefd/9zdZJW0VJ5.lottie"
const val LOTTIE_SAVE_URL = "https://lottie.host/da1f9eb6-e6ce-4bea-b018-a4856afe0bf4/KADO2toJt5.lottie"
const val LOTTIE_SHARE_URL = "https://lottie.host/0753d442-1101-40ac-a517-f21d4be97ef8/D1QEnYRtVw.lottie"

enum class AiActionType {
    NONE,
    WATCH_VIDEO,
    VISIT_SITE,
    DOWNLOAD_PREVIEW,
    SELECT_DOWNLOAD_OPTIONS,
    CONFIRMATION_CHOICE,
    FAQ_LIST
}

data class FaqQuestion(
    val question: String,
    val answer: String
)

val defaultFaqQuestions = listOf(
    FaqQuestion(
        question = "How to download 1080p HD videos?",
        answer = "To download in 1080p HD or MP3, ask me 'download [video name]' or click Download under any video preview. You can select 1080p, 720p, 480p, or 320kbps MP3 audio!"
    ),
    FaqQuestion(
        question = "How to play video audio in background?",
        answer = "Click the 'Play Audio' button under any video stream! It switches seamlessly to audio-only mode with low data consumption and animated visualizer."
    ),
    FaqQuestion(
        question = "Can AI open websites like YouTube or Facebook?",
        answer = "Yes! Simply ask 'open youtube' or 'visit facebook', and I will automatically open the site for you in Cloudihub Web Browser."
    ),
    FaqQuestion(
        question = "Where are my downloaded files saved?",
        answer = "All downloaded videos and audio files are safely stored in your 'Downloads Hub' tab, ready for offline playback anytime!"
    ),
    FaqQuestion(
        question = "How do I search using Voice Command?",
        answer = "Tap the Microphone icon in the search bar or chat input bar, speak your search query, and I'll find or play it instantly!"
    )
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionType: AiActionType = AiActionType.NONE,
    val targetVideo: CloudVideo? = null,
    val targetUrl: String? = null,
    val promptQuestion: String? = null
)

fun resolveSmartDomain(query: String): String {
    val lower = query.lowercase().trim()
    val cleanKey = lower.replace("go to", "")
        .replace("open site", "")
        .replace("open", "")
        .replace("visit", "")
        .replace("take me to", "")
        .replace("browse", "")
        .replace("show me", "")
        .replace("search", "")
        .trim()

    val knownDomains = mapOf(
        "facebook" to "https://facebook.com",
        "fb" to "https://facebook.com",
        "google" to "https://google.com",
        "youtube" to "https://youtube.com",
        "yt" to "https://youtube.com",
        "daraz" to "https://daraz.com.bd",
        "tiktok" to "https://tiktok.com",
        "instagram" to "https://instagram.com",
        "insta" to "https://instagram.com",
        "twitter" to "https://x.com",
        "x" to "https://x.com",
        "github" to "https://github.com",
        "wikipedia" to "https://wikipedia.org",
        "wiki" to "https://wikipedia.org",
        "amazon" to "https://amazon.com",
        "netflix" to "https://netflix.com",
        "spotify" to "https://spotify.com"
    )

    for ((key, url) in knownDomains) {
        if (cleanKey.contains(key)) return url
    }

    if (cleanKey.contains(".") && !cleanKey.contains(" ")) {
        return if (cleanKey.startsWith("http://") || cleanKey.startsWith("https://")) cleanKey
        else "https://$cleanKey"
    }

    return "https://google.com/search?q=${java.net.URLEncoder.encode(cleanKey.ifEmpty { query }, "UTF-8")}"
}

@Composable
fun VideoPreviewPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoURI(android.net.Uri.parse(videoUrl))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        start()
                    }
                }
            },
            update = { view ->
                if (isPlaying) {
                    if (!view.isPlaying) view.start()
                } else {
                    if (view.isPlaying) view.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Red.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("AUTO PREVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FloatingAiLottieWidget(
    viewModel: CloudihubViewModel,
    isMediaPlaying: Boolean,
    onSearchRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!viewModel.isAiAssistantEnabled) return

    var showAiChatModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Determine current active animation URL
    val currentAnimationUrl = when {
        isMediaPlaying && viewModel.isAiMusicEffectEnabled -> LOTTIE_AUDIO_3_URL // When audio/video plays & music effect toggle is ON
        else -> LOTTIE_OVERLAY_1_URL // 1st Lottie mascot exclusively on display
    }

    // Preload Lottie compositions for fast & smooth transitions
    val comp1 by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_OVERLAY_1_URL))
    val comp2 by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_OVERLAY_2_URL))
    val comp3 by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_AUDIO_3_URL))

    val activeComposition = when (currentAnimationUrl) {
        LOTTIE_AUDIO_3_URL -> comp3
        else -> comp1
    }

    // Animation speed - default normal speed
    val animSpeed = 1.0f

    // Natural full loop without frame restriction
    val clipSpec = null
    val iterations = LottieConstants.IterateForever

    // Magnetic snap-to-edge drag state
    val offsetX = remember { Animatable(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val widgetSizePx = with(density) { 150.dp.toPx() }
    
    // Position shift rightward by 20dp so Lottie animation touches right edge
    val maxRightSnapDisplacement = with(density) { 20.dp.toPx() }
    val maxLeftSnapDisplacement = -(screenWidthPx - widgetSizePx - with(density) { 20.dp.toPx() })
    
    // Strict vertical bounds so the widget never goes off-screen
    val minOffsetY = -(with(density) { (screenHeightDp - 220.dp).coerceAtLeast(100.dp).toPx() })
    val maxOffsetY = with(density) { 15.dp.toPx() }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Magnetic Snap to nearest screen edge (Left or Right)
                        val snapTarget = if (offsetX.value < (maxLeftSnapDisplacement + maxRightSnapDisplacement) / 2f) {
                            maxLeftSnapDisplacement // Fast snap to Left Edge
                        } else {
                            maxRightSnapDisplacement // Fast snap to Right Edge (shifted further right)
                        }
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = snapTarget,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = (offsetX.value + dragAmount.x).coerceIn(maxLeftSnapDisplacement, maxRightSnapDisplacement)
                        val newY = (offsetY + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)
                        scope.launch {
                            offsetX.snapTo(newX)
                        }
                        offsetY = newY
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showAiChatModal = true
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            Crossfade(
                targetState = activeComposition,
                animationSpec = tween(600),
                label = "LottieCrossfade"
            ) { targetComp ->
                if (targetComp != null) {
                    val progress by animateLottieCompositionAsState(
                        composition = targetComp,
                        clipSpec = clipSpec,
                        iterations = iterations,
                        isPlaying = true,
                        speed = animSpeed
                    )

                    val lottieItemSize = 150.dp
                    LottieAnimation(
                        composition = targetComp,
                        progress = { progress },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(lottieItemSize)
                    )
                } else {
                    CircularProgressIndicator(
                        color = Color(0xFF0284C7),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // AI Dynamic Chat Popup Overlay
    if (showAiChatModal) {
        AiAssistantChatDialog(
            viewModel = viewModel,
            headerLottieComposition = comp2,
            onDismiss = { showAiChatModal = false },
            onSearchRequested = onSearchRequested
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatDialog(
    viewModel: CloudihubViewModel,
    headerLottieComposition: LottieComposition?,
    onDismiss: () -> Unit,
    onSearchRequested: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val sampleVideos = viewModel.videos.ifEmpty {
        listOf(
            CloudVideo(
                id = "sintel_trailer",
                title = "Sintel Movie Official HD Trailer [Edge Gaming]",
                duration = "00:52",
                creator = "Blender Foundation",
                imageUrl = "https://i.postimg.cc/k4G6jJq2/sintel.jpg",
                views = "118.2M",
                fileUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                sizeMb = 85.0
            ),
            CloudVideo(
                id = "ocean_scenic",
                title = "Deep Ocean Scenic Exploration [Aesthetics]",
                duration = "03:15",
                creator = "VideoJS Ocean Labs",
                imageUrl = "https://i.postimg.cc/85zC3qJg/ocean.jpg",
                views = "24.5M",
                fileUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                sizeMb = 140.0
            )
        )
    }

    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                text = "",
                isUser = false,
                actionType = AiActionType.FAQ_LIST
            )
        )
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 1-Hour Chat History Auto Cleanup
    LaunchedEffect(Unit) {
        val oneHourAgo = System.currentTimeMillis() - 3_600_000L
        messages.removeAll { it.timestampMillis < oneHourAgo }
    }

    // Auto-scroll chat to bottom when new messages or response state changes
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(forcedQuery: String? = null) {
        keyboardController?.hide()
        focusManager.clearFocus()

        val query = (forcedQuery ?: inputText).trim()
        if (query.isEmpty() || isThinking) return

        if (forcedQuery == null) {
            messages.add(AiChatMessage(text = query, isUser = true))
            inputText = ""
        }

        // Check user message count (Threshold: 3rd message sent opens Full Screen AI Console)
        val userMsgCount = messages.count { it.isUser }
        if (userMsgCount >= 3) {
            onDismiss()
            viewModel.showAiFullChatScreen = true
            return
        }

        isThinking = true

        scope.launch {
            delay(100)
            listState.animateScrollToItem(if (isThinking) messages.size else messages.size - 1)
        }

        // Generate AI response with thinking animation
        scope.launch {
            delay(1100) // Simulated AI thinking time
            val lower = query.lowercase()

            val matchedFaq = defaultFaqQuestions.find { 
                lower.contains(it.question.lowercase()) || 
                it.question.lowercase().contains(lower) 
            }

            if (matchedFaq != null) {
                isThinking = false
                messages.add(
                    AiChatMessage(
                        text = matchedFaq.answer,
                        isUser = false
                    )
                )
            }
            // 1. Resolution option download selection response
            else if (lower.contains("download in") || lower.contains("1080p") || lower.contains("720p") || lower.contains("480p") || lower.contains("320kbps") || lower.contains("mp3") || lower.contains("mp4")) {
                isThinking = false
                val target = sampleVideos.find { lower.contains(it.title.lowercase()) } ?: sampleVideos.first()
                viewModel.triggerVideoDownload(target)
                messages.add(
                    AiChatMessage(
                        text = "Initializing cloud download for '${target.title}' ($query)...",
                        isUser = false
                    )
                )
                scope.launch {
                    delay(1200)
                    messages.add(
                        AiChatMessage(
                            text = "Download Completed! '${target.title}' saved to Offline Downloads. Status: Success (100%)",
                            isUser = false
                        )
                    )
                    delay(100)
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
            // 2. Mismatched domain check
            else if (viewModel.isAiIntentVerificationEnabled && (lower.contains("tiktok") || lower.contains("tik tok") || lower.contains("instagram")) && (lower.contains("youtube") || lower.contains("youtu.be") || lower.contains("sintel") || lower.contains("http") || lower.contains("video"))) {
                isThinking = false
                messages.add(
                    AiChatMessage(
                        text = "Domain Intent Verification Warning",
                        isUser = false,
                        actionType = AiActionType.CONFIRMATION_CHOICE,
                        promptQuestion = "You requested a TikTok video download, but provided a YouTube/Cloudihub video link. Do you want me to proceed with downloading this YouTube video instead?",
                        targetVideo = sampleVideos.first()
                    )
                )
            }
            // 3. Auto Downloader Assistant Request
            else if (viewModel.isAiAutoDownloaderEnabled && (lower.contains("download") || lower.contains("save") || lower.contains("get video"))) {
                isThinking = false
                val target = sampleVideos.find { lower.contains(it.title.lowercase()) } ?: sampleVideos.first()
                messages.add(
                    AiChatMessage(
                        text = "Video preview ready! You can watch or start download below:",
                        isUser = false,
                        actionType = AiActionType.DOWNLOAD_PREVIEW,
                        targetVideo = target
                    )
                )
            }
            // 4. Smart Site Navigation / Instant Direct Browser Auto-Redirect
            else if (viewModel.isAiSiteNavigationEnabled && (
                lower.contains("go to") || lower.contains("open") || lower.contains("visit") || lower.contains("take me to") ||
                lower.contains("facebook") || lower.contains("google") || lower.contains("youtube") || lower.contains("daraz") ||
                lower.contains("tiktok") || lower.contains("instagram") || lower.contains("github") || lower.contains("wiki") ||
                lower.contains("http") || lower.contains(".com") || lower.contains(".org")
            )) {
                val url = resolveSmartDomain(query)
                isThinking = false
                viewModel.openUrl(url)
                Toast.makeText(context, "Redirecting to $url...", Toast.LENGTH_SHORT).show()
                onDismiss()
                return@launch
            }
            // 4. Auto Link Detection & Watch Redirect
            else if (viewModel.isAiLinkDetectionEnabled && (lower.contains("watch") || lower.contains("play") || lower.contains("see video") || lower.contains("sintel") || lower.contains("ocean") || lower.contains("http"))) {
                isThinking = false
                val target = sampleVideos.find { lower.contains(it.title.lowercase()) || lower.contains(it.creator.lowercase()) } ?: sampleVideos.first()
                messages.add(
                    AiChatMessage(
                        text = "Video link detected! Here is your video preview:",
                        isUser = false,
                        actionType = AiActionType.WATCH_VIDEO,
                        targetVideo = target
                    )
                )
            }
            // 5. General Search / Question
            else {
                val replyText = when {
                    lower.contains("search") || lower.contains("find") -> {
                        val searchKey = query.replace("search", "", ignoreCase = true).replace("find", "", ignoreCase = true).trim()
                        if (searchKey.isNotEmpty()) {
                            onSearchRequested(searchKey)
                            "I searched for '$searchKey' in Cloudihub for you!"
                        } else {
                            "What would you like me to search for on Cloudihub?"
                        }
                    }
                    lower.contains("hello") || lower.contains("hi") -> "Hello! I am ready to assist with cloud video streaming, site navigation, and smart downloads!"
                    lower.contains("music") -> "Check out our Lofi & Ambient music stream in Cloudihub Music tab!"
                    else -> "Cloudihub AI is online! Ask me to find videos, open websites, or preview downloads."
                }
                isThinking = false
                messages.add(AiChatMessage(text = replyText, isUser = false))
            }

            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Modal BottomSheet matching full Light Blue background (0xFFF0F9FF)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF0F9FF),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF0284C7).copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(Color(0xFFF0F9FF))
                .padding(horizontal = 16.dp)
        ) {
            // Top Header sitting natively on original popup theme background Color(0xFFF0F9FF)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Lottie Mascot Animation (Transparent background sitting on popup theme)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(115.dp)
                    ) {
                        if (headerLottieComposition != null) {
                            val headerProgress by animateLottieCompositionAsState(
                                composition = headerLottieComposition,
                                iterations = LottieConstants.IterateForever,
                                isPlaying = true,
                                speed = 0.5f
                            )
                            LottieAnimation(
                                composition = headerLottieComposition,
                                progress = { headerProgress },
                                modifier = Modifier.size(115.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Avatar",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Cloudihub AI Copilot",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                    }

                    Text(
                        text = "Smart Automation & Media Assistant",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Description lines under header Lottie
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✨ Intelligent Assistant for Cloudihub Video Streamer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• Search & stream videos • Auto 1080p/MP3 downloads • Smart site redirects",
                            fontSize = 10.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chat Messages List
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages, key = { it.id }) { msg ->
                    Column(
                        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (msg.text.isNotEmpty()) {
                            Surface(
                                color = if (msg.isUser) Color(0xFF0284C7) else Color.White,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                                ),
                                shadowElevation = if (msg.isUser) 0.dp else 1.dp,
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 14.sp,
                                        color = if (msg.isUser) Color.White else Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.timestamp,
                                        fontSize = 10.sp,
                                        color = if (msg.isUser) Color.White.copy(alpha = 0.7f) else Color(0xFF94A3B8),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }

                        // --- RICH ACTION CARDS BASED ON AI PERMISSIONS ---
                        if (!msg.isUser) {
                            when (msg.actionType) {
                                AiActionType.WATCH_VIDEO -> {
                                    val vid = msg.targetVideo
                                    if (vid != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Card(
                                            modifier = Modifier
                                                .widthIn(max = 300.dp)
                                                .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(110.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                ) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(vid.imageUrl),
                                                        contentDescription = vid.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
                                                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(vid.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(vid.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = Color(0xFF0F172A))
                                                Text(vid.creator, fontSize = 11.sp, color = Color(0xFF64748B))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.playVideo(vid)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Watch Video Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                AiActionType.VISIT_SITE -> {
                                    val targetUrl = msg.targetUrl
                                    if (targetUrl != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Card(
                                            modifier = Modifier
                                                .widthIn(max = 300.dp)
                                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Cloudihub Web Redirect", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4338CA))
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(targetUrl, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1)
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.openUrl(targetUrl)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                                ) {
                                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Redirect to Site", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                AiActionType.DOWNLOAD_PREVIEW -> {
                                    val vid = msg.targetVideo
                                    if (vid != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Card(
                                            modifier = Modifier
                                                .widthIn(max = 300.dp)
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                // Large Video Auto-Preview Frame
                                                VideoPreviewPlayer(videoUrl = vid.fileUrl)

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(vid.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = Color(0xFF0F172A))
                                                Text("${vid.creator} • ${vid.duration} • ${vid.sizeMb} MB", fontSize = 11.sp, color = Color(0xFF64748B))

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // 2 White Theme Buttons
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // Button 1: Watch List
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.addToWatchLater(vid)
                                                            Toast.makeText(context, "Added to Watchlist!", Toast.LENGTH_SHORT).show()
                                                            messages.add(
                                                                AiChatMessage(
                                                                    text = "Added '${vid.title}' to your Watchlist!",
                                                                    isUser = false
                                                                )
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
                                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Watchlist", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Button 2: Download
                                                    OutlinedButton(
                                                        onClick = {
                                                            messages.add(
                                                                AiChatMessage(
                                                                    text = "Which resolution and format would you like to download for '${vid.title}'?",
                                                                    isUser = false,
                                                                    actionType = AiActionType.SELECT_DOWNLOAD_OPTIONS,
                                                                    targetVideo = vid
                                                                )
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
                                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    ) {
                                                        val lottieComp by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_OVERLAY_1_URL))
                                                        LottieAnimation(
                                                            composition = lottieComp,
                                                            iterations = LottieConstants.IterateForever,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                AiActionType.SELECT_DOWNLOAD_OPTIONS -> {
                                    val vid = msg.targetVideo
                                    if (vid != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Card(
                                            modifier = Modifier
                                                .widthIn(max = 300.dp)
                                                .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("Select Resolution & Format:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                listOf(
                                                    "1080p Full HD (MP4)" to "245 MB",
                                                    "720p HD (MP4)" to "110 MB",
                                                    "480p SD (MP4)" to "55 MB",
                                                    "320kbps Audio (MP3)" to "8 MB"
                                                ).forEach { (resLabel, resSize) ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp)
                                                            .clickable {
                                                                sendMessage("Download in $resLabel")
                                                            },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (resLabel.contains("MP3")) Icons.Default.MusicNote else Icons.Default.Hd,
                                                                contentDescription = null,
                                                                tint = Color(0xFF22C55E),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(resLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.weight(1f))
                                                            Text(resSize, fontSize = 10.sp, color = Color(0xFF64748B))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                AiActionType.CONFIRMATION_CHOICE -> {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        modifier = Modifier
                                            .widthIn(max = 300.dp)
                                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Domain Mismatch Warning", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                msg.promptQuestion ?: "Are you sure you want to proceed with downloading this video?",
                                                fontSize = 12.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val target = msg.targetVideo
                                                        if (target != null) {
                                                            viewModel.triggerVideoDownload(target)
                                                            Toast.makeText(context, "Download confirmed & started!", Toast.LENGTH_SHORT).show()
                                                            messages.add(
                                                                AiChatMessage(
                                                                    text = "Confirmed! Downloading ${target.title}. You can track it in Downloads Hub.",
                                                                    isUser = false
                                                                )
                                                            )
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("Yes, Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        messages.add(
                                                            AiChatMessage(
                                                                text = "Download cancelled. Please paste a valid link.",
                                                                isUser = false
                                                            )
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("No, Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                                }
                                            }
                                        }
                                    }
                                }

                                AiActionType.FAQ_LIST -> {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier
                                            .widthIn(max = 310.dp)
                                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "💡 Frequently Asked Questions (Tap to ask):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                defaultFaqQuestions.forEach { faq ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                sendMessage(faq.question)
                                                            },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                                                        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.HelpOutline,
                                                                contentDescription = null,
                                                                tint = Color(0xFF0284C7),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = faq.question,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color(0xFF0369A1),
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Icon(
                                                                imageVector = Icons.Default.ChevronRight,
                                                                contentDescription = null,
                                                                tint = Color(0xFF38BDF8),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }

                // AI Thinking State Indicator with Cloud Processing Icon
                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 2.dp,
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF38BDF8),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = "Cloud AI Thinking",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Cloudihub AI is thinking...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Cloudihub AI...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Default
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        sendMessage()
                    },
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
