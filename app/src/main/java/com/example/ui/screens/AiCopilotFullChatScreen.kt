package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.*
import com.example.ui.CloudVideo
import com.example.ui.CloudihubViewModel
import com.example.ui.components.AiActionType
import com.example.ui.components.AiChatMessage
import com.example.ui.components.LOTTIE_OVERLAY_1_URL
import com.example.ui.components.NavigationTab
import com.example.ui.components.resolveSmartDomain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCopilotFullChatScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val videosList = viewModel.videos

    // Persistent Chat History for Full Screen Mode
    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                text = "Welcome to Cloudihub AI Copilot Full Screen Console! I can help you search videos, auto-redirect to sites, preview downloads, and control background playback.",
                isUser = false
            )
        )
    }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val mascotComposition by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_OVERLAY_1_URL))

    fun sendMessage(forcedQuery: String? = null) {
        keyboardController?.hide()
        focusManager.clearFocus()
        val query = (forcedQuery ?: inputText).trim()
        if (query.isEmpty() || isThinking) return

        if (forcedQuery == null) {
            messages.add(AiChatMessage(text = query, isUser = true))
            inputText = ""
        }

        isThinking = true

        scope.launch {
            delay(1100) // Simulated AI thinking time
            val lower = query.lowercase()

            if (lower.contains("download in") || lower.contains("1080p") || lower.contains("720p") || lower.contains("480p") || lower.contains("mp3")) {
                isThinking = false
                val target = videosList.find { lower.contains(it.title.lowercase()) } ?: videosList.firstOrNull()
                val quality = if (lower.contains("1080p")) "1080p Full HD" else if (lower.contains("720p")) "720p HD" else if (lower.contains("480p")) "480p SD" else "320kbps MP3 Audio"
                
                messages.add(
                    AiChatMessage(
                        text = "Starting download process for '${target?.title ?: "Video"}' in $quality format...",
                        isUser = false,
                        actionType = AiActionType.DOWNLOAD_PREVIEW,
                        targetVideo = target,
                        promptQuestion = quality
                    )
                )
                target?.let {
                    viewModel.triggerVideoDownloadWithOptions(
                        video = it,
                        customTitle = it.title,
                        qualityLabel = quality,
                        estimatedSizeMb = it.sizeMb,
                        isAudioOnly = lower.contains("mp3")
                    )
                }
                return@launch
            }

            if (lower.contains("download") || lower.contains("save video")) {
                isThinking = false
                val matched = videosList.find { lower.contains(it.title.lowercase()) } ?: videosList.firstOrNull()
                messages.add(
                    AiChatMessage(
                        text = "Found video: '${matched?.title ?: "Selected Video"}'. Please select your preferred resolution or audio format:",
                        isUser = false,
                        actionType = AiActionType.SELECT_DOWNLOAD_OPTIONS,
                        targetVideo = matched
                    )
                )
                return@launch
            }

            if (lower.contains("open") || lower.contains("visit") || lower.contains("go to")) {
                isThinking = false
                val targetUrl = resolveSmartDomain(query)
                messages.add(
                    AiChatMessage(
                        text = "Redirecting you to $targetUrl...",
                        isUser = false,
                        actionType = AiActionType.VISIT_SITE,
                        targetUrl = targetUrl
                    )
                )
                viewModel.openUrl(targetUrl)
                return@launch
            }

            if (lower.contains("search") || lower.contains("video") || lower.contains("find")) {
                isThinking = false
                val queryTerm = query.replace("search", "").replace("video", "").trim()
                val found = videosList.find { it.title.lowercase().contains(queryTerm.lowercase()) } ?: videosList.firstOrNull()
                messages.add(
                    AiChatMessage(
                        text = "Here is the top matched video for '$queryTerm':",
                        isUser = false,
                        actionType = AiActionType.WATCH_VIDEO,
                        targetVideo = found
                    )
                )
                return@launch
            }

            // Default AI Assistant Response
            isThinking = false
            messages.add(
                AiChatMessage(
                    text = "I'm on it! Command processed: '$query'. You can ask me to play any video, download in high resolution, or open any web page.",
                    isUser = false
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE),
                        Color(0xFFF0F9FF),
                        Color.White
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Full Screen Header (Seamless background matching top bar gradient)
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.showAiFullChatScreen = false }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color(0xFF0284C7)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Lottie Mascot Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mascotComposition != null) {
                            val progress by animateLottieCompositionAsState(
                                composition = mascotComposition,
                                iterations = LottieConstants.IterateForever
                            )
                            LottieAnimation(
                                composition = mascotComposition,
                                progress = { progress },
                                modifier = Modifier.size(42.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Mascot",
                                tint = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloudihub AI Copilot",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Home Console & Full History",
                            fontSize = 11.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 3-dot Options Menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color(0xFF0284C7)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(Color.White)
                                .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Chat History", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE11D48)) },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMenu = false
                                    messages.clear()
                                    messages.add(
                                        AiChatMessage(
                                            text = "Chat history cleared. How can I assist you now?",
                                            isUser = false
                                        )
                                    )
                                    Toast.makeText(context, "Chat History Reset", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("FAQ & Help Guide", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0369A1)) },
                                leadingIcon = {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMenu = false
                                    sendMessage("Show FAQ list")
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Back to Home", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.showAiFullChatScreen = false
                                }
                            )
                        }
                    }
                }
            }

            // 2. Chat Messages Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    FullChatMessageItem(
                        msg = msg,
                        viewModel = viewModel,
                        onOptionSelected = { option -> sendMessage(option) }
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
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
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Cloudihub AI is processing...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick Suggestion Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf("Search Videos", "Open Youtube", "Download Video", "Play Music")
                chips.forEach { chipText ->
                    Surface(
                        onClick = { sendMessage(chipText) },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 4. Bottom Chat Input Bar
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE0F2FE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.startVoiceSearch() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = Color(0xFF0284C7)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask AI to play, download or visit...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        singleLine = false,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            sendMessage()
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullChatMessageItem(
    msg: AiChatMessage,
    viewModel: CloudihubViewModel,
    onOptionSelected: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (msg.isUser) Color(0xFF0284C7) else Color.White,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (msg.isUser) 18.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 18.dp
            ),
            shadowElevation = if (msg.isUser) 2.dp else 1.dp,
            border = if (!msg.isUser) BorderStroke(1.dp, Color(0xFFBAE6FD)) else null
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (msg.text.isNotEmpty()) {
                    Text(
                        text = msg.text,
                        fontSize = 14.sp,
                        color = if (msg.isUser) Color.White else Color(0xFF0F172A),
                        lineHeight = 20.sp
                    )
                }

                // Interactive Content Widgets based on Action Type
                when (msg.actionType) {
                    AiActionType.WATCH_VIDEO -> {
                        msg.targetVideo?.let { video ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.playVideo(video)
                                        Toast.makeText(context, "Playing ${video.title}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(video.imageUrl),
                                        contentDescription = video.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text(video.creator, fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = Color(0xFF0284C7), modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }

                    AiActionType.VISIT_SITE -> {
                        msg.targetUrl?.let { url ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.openUrl(url)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open $url", fontSize = 12.sp)
                            }
                        }
                    }

                    AiActionType.SELECT_DOWNLOAD_OPTIONS -> {
                        msg.targetVideo?.let { video ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Available Quality Formats:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("1080p", "720p", "480p", "MP3").forEach { opt ->
                                    Surface(
                                        onClick = { onOptionSelected("Download in $opt ${video.title}") },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE0F2FE),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                                    ) {
                                        Text(opt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.timestamp,
                    fontSize = 9.sp,
                    color = if (msg.isUser) Color.White.copy(alpha = 0.7f) else Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
