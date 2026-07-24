package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.airbnb.lottie.LottieComposition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val LOTTIE_OVERLAY_1_URL = "https://lottie.host/c8ff3299-4055-482b-88dc-1da2950188cb/1knzg1NsRU.lottie"
const val LOTTIE_OVERLAY_2_URL = "https://lottie.host/2b5997b8-6637-4730-8755-ae2f561fc4ec/hrOvUSmkO3.lottie"
const val LOTTIE_AUDIO_3_URL = "https://lottie.host/eb5fcad6-9e03-4ae1-92c3-1521d49cc014/Kx8NZOf9M7.lottie"

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
)

@Composable
fun FloatingAiLottieWidget(
    isMediaPlaying: Boolean,
    onSearchRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAiChatModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Smooth auto-alternating state between 1st and 2nd Lottie when idle
    var idleToggleIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isMediaPlaying) {
        if (!isMediaPlaying) {
            while (true) {
                delay(4500)
                idleToggleIndex = (idleToggleIndex + 1) % 2
            }
        }
    }

    // Determine current active animation URL
    val currentAnimationUrl = when {
        isMediaPlaying -> LOTTIE_AUDIO_3_URL
        idleToggleIndex == 0 -> LOTTIE_OVERLAY_1_URL
        else -> LOTTIE_OVERLAY_2_URL
    }

    // Preload Lottie compositions for fast & smooth transitions
    val comp1 by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_OVERLAY_1_URL))
    val comp2 by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_OVERLAY_2_URL))
    val comp3 by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_AUDIO_3_URL))

    val activeComposition = when (currentAnimationUrl) {
        LOTTIE_AUDIO_3_URL -> comp3
        LOTTIE_OVERLAY_1_URL -> comp1
        else -> comp2
    }

    // Magnetic snap-to-edge drag state
    val offsetX = remember { Animatable(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val widgetSizePx = with(density) { 150.dp.toPx() }
    val maxLeftSnapDisplacement = -(screenWidthPx - widgetSizePx - with(density) { 16.dp.toPx() })

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Magnetic Snap to nearest screen edge (Left or Right)
                        val snapTarget = if (offsetX.value < maxLeftSnapDisplacement / 2f) {
                            maxLeftSnapDisplacement // Fast snap to Left Edge
                        } else {
                            0f // Fast snap to Right Edge
                        }
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = snapTarget,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount.x).coerceIn(maxLeftSnapDisplacement, 0f))
                        }
                        offsetY += dragAmount.y
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
                        iterations = LottieConstants.IterateForever,
                        isPlaying = true,
                        speed = 0.5f
                    )
                    LottieAnimation(
                        composition = targetComp,
                        progress = { progress },
                        modifier = Modifier.size(150.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        color = Color(0xFF0284C7),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Audio playing visual green indicator
            if (isMediaPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
            }
        }
    }

    // AI Dynamic Chat Popup Overlay
    if (showAiChatModal) {
        AiAssistantChatDialog(
            headerLottieComposition = comp2,
            onDismiss = { showAiChatModal = false },
            onSearchRequested = onSearchRequested
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatDialog(
    headerLottieComposition: LottieComposition?,
    onDismiss: () -> Unit,
    onSearchRequested: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            AiChatMessage(text = "Hello! I am Cloudihub AI. How can I assist you today? Ask me to find videos, music, or search anything!", isUser = false)
        )
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun sendMessage() {
        val query = inputText.trim()
        if (query.isEmpty() || isThinking) return

        messages.add(AiChatMessage(text = query, isUser = true))
        inputText = ""
        isThinking = true

        scope.launch {
            delay(100)
            listState.animateScrollToItem(if (isThinking) messages.size else messages.size - 1)
        }

        // Generate AI response with thinking animation
        scope.launch {
            delay(1300) // Simulated AI thinking time
            val lower = query.lowercase()
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
                lower.contains("hello") || lower.contains("hi") -> "Hello! Ready to explore trending cloud videos, lofi music, or private vault downloads?"
                lower.contains("download") -> "You can download any video using the download icon on the top bar or video card!"
                lower.contains("music") -> "Check out our Lofi & Ambient music stream in Cloudihub Music tab!"
                else -> "Cloudihub AI is here to help! You can search videos, manage private vault files, or stream videos effortlessly."
            }
            isThinking = false
            messages.add(AiChatMessage(text = replyText, isUser = false))
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
                .fillMaxHeight(0.88f)
                .background(Color(0xFFF0F9FF))
                .padding(horizontal = 16.dp)
        ) {
            // Header with 2nd Lottie Animation centered, enlarged, fully matching 0xFFF0F9FF background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
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
                                modifier = Modifier.size(160.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Avatar",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Cloudihub AI Assistant",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
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
                        text = "Smart cloud video & media copilot",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = Color(0xFFE0F2FE),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Online • Ready to help",
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Quick suggestion chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        inputText = "Trending videos"
                        sendMessage()
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White
                    ),
                    label = { Text("Trending", fontSize = 12.sp, color = Color(0xFF0369A1)) }
                )
                SuggestionChip(
                    onClick = {
                        inputText = "Lofi music"
                        sendMessage()
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White
                    ),
                    label = { Text("Lofi Music", fontSize = 12.sp, color = Color(0xFF0369A1)) }
                )
                SuggestionChip(
                    onClick = {
                        inputText = "How to download?"
                        sendMessage()
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White
                    ),
                    label = { Text("Download guide", fontSize = 12.sp, color = Color(0xFF0369A1)) }
                )
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages, key = { it.id }) { msg ->
                    Box(
                        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = if (msg.isUser) Color(0xFF0284C7) else Color.White,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                bottomEnd = if (msg.isUser) 4.dp else 16.dp
                            ),
                            shadowElevation = if (msg.isUser) 0.dp else 1.dp,
                            modifier = Modifier.widthIn(max = 280.dp)
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
                }

                // AI Thinking State Indicator
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
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF0284C7),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Cloudihub AI is thinking...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
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
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = { sendMessage() },
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
