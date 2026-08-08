package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.*
import com.example.ui.components.LOTTIE_OVERLAY_1_URL
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import com.example.ui.components.NavigationTab
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudShape
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import androidx.compose.ui.window.Dialog

@Composable
fun ProfileScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    if (viewModel.showCloudHubInProfile) {
        SitesScreen(viewModel = viewModel)
        return
    }

    when (viewModel.activeProfilePage) {
        "watch_later" -> WatchLaterScreen(viewModel = viewModel)
        "refer" -> ReferScreen(viewModel = viewModel)
        "downloads" -> DownloadsScreen(viewModel = viewModel)
        "linked_devices" -> LinkedDevicesScreen(viewModel = viewModel)
        "offline_folders" -> OfflineFoldersScreen(viewModel = viewModel)
        "private_vault" -> PrivateVaultScreen(viewModel = viewModel)
        "ai_settings" -> AiSettingsScreen(viewModel = viewModel)
        else -> MainProfileContent(viewModel = viewModel, modifier = modifier)
    }

    if (viewModel.showPrimeBadgesDialog) {
        ModernPrimeBadgesBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.showPrimeBadgesDialog = false }
        )
    }

    // Modal Bottom Sheets (with drag handles that can be slid/dragged down to dismiss)
    if (viewModel.showHistoryPopup) {
        HistoryBottomSheet(viewModel = viewModel) { viewModel.showHistoryPopup = false }
    }
    if (viewModel.showFeedbackPopup) {
        FeedbackBottomSheet(viewModel = viewModel) { viewModel.showFeedbackPopup = false }
    }
    if (viewModel.showSubscriptionPopup) {
        SubscriptionBottomSheet(viewModel = viewModel) { viewModel.showSubscriptionPopup = false }
    }

    // --- PRIVATE VAULT SETUP & UNLOCK POPUPS (AS REQUESTED) ---
    if (viewModel.showPrivateVaultPasswordTypeDialog) {
        PrivateVaultPasswordTypeDialog(viewModel = viewModel)
    }

    if (viewModel.showPrivateVaultPasswordInputDialog) {
        PrivateVaultPasswordInputDialog(viewModel = viewModel)
    }

    if (viewModel.showPrivateVaultUnlockDialog) {
        PrivateVaultUnlockDialog(viewModel = viewModel)
    }
}

@Composable
fun ProfileAnimatedBackground(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bgCloudAnim")

    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud1"
    )

    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud2"
    )

    val animalOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animal"
    )

    val cloudScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloudScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Clean White / Soft Sky White Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF8FAFC),
                                Color(0xFFEFF6FF)
                            )
                        )
                    }
                )
        )

        // Soft ambient cloud glow circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = if (isDark) 0.12f else 0.15f),
                        Color.Transparent
                    )
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.2f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF60A5FA).copy(alpha = if (isDark) 0.10f else 0.12f),
                        Color.Transparent
                    )
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.85f, size.height * 0.45f)
            )
        }

        // Floating Cloud 1 (Top Left)
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            tint = (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)).copy(alpha = 0.14f),
            modifier = Modifier
                .size(110.dp)
                .offset(x = 10.dp, y = 40.dp + cloudOffset1.dp)
                .graphicsLayer {
                    scaleX = cloudScale
                    scaleY = cloudScale
                }
        )

        // Floating Cloud Animal - Bird/Sky Pet 1 (Top Right)
        Icon(
            imageVector = Icons.Default.CrueltyFree,
            contentDescription = null,
            tint = (if (isDark) Color(0xFF818CF8) else Color(0xFF0284C7)).copy(alpha = 0.12f),
            modifier = Modifier
                .size(65.dp)
                .offset(x = 280.dp, y = 60.dp + animalOffset.dp)
                .graphicsLayer {
                    scaleX = cloudScale
                    scaleY = cloudScale
                }
        )

        // Floating Cloud 2 (Top Right)
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = null,
            tint = (if (isDark) Color(0xFF818CF8) else Color(0xFF38BDF8)).copy(alpha = 0.15f),
            modifier = Modifier
                .size(120.dp)
                .offset(x = 240.dp, y = 140.dp + cloudOffset2.dp)
                .graphicsLayer {
                    scaleX = cloudScale
                    scaleY = cloudScale
                }
        )

        // Floating Cloud Animal - Pet 2 (Mid Left)
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = null,
            tint = (if (isDark) Color(0xFF34D399) else Color(0xFF0284C7)).copy(alpha = 0.10f),
            modifier = Modifier
                .size(50.dp)
                .offset(x = 25.dp, y = 280.dp - animalOffset.dp)
        )

        // Floating Cloud 3 - Sync Cloud (Mid Right)
        Icon(
            imageVector = Icons.Default.CloudSync,
            contentDescription = null,
            tint = (if (isDark) Color(0xFF38BDF8) else Color(0xFF60A5FA)).copy(alpha = 0.13f),
            modifier = Modifier
                .size(90.dp)
                .offset(x = 270.dp, y = 380.dp - cloudOffset1.dp)
        )

        // Floating Sky Wind / Air (Bottom Left)
        Icon(
            imageVector = Icons.Default.Air,
            contentDescription = null,
            tint = (if (isDark) Color(0xFF94A3B8) else Color(0xFF0284C7)).copy(alpha = 0.11f),
            modifier = Modifier
                .size(75.dp)
                .offset(x = 15.dp, y = 520.dp + cloudOffset2.dp)
        )
    }
}

@Composable
fun MainProfileContent(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = viewModel.isDarkTheme
    val scrollState = rememberScrollState()
    val storageInfo = viewModel.getDeviceStorageInfo()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showLongPressOptions by remember { mutableStateOf(false) }
    var pressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Clean White Cloud Background with Floating Clouds & Cloud Animals
        ProfileAnimatedBackground(isDark = isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
        ) {
            // --- PROFILE HEADER & SIGNUP OPTION ---
        val isSigned = viewModel.isGoogleSignedIn
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 1. TOP: PROFILE IMAGE
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clickable {
                            if (!isSigned) {
                                viewModel.showSignupScreen = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                3.dp,
                                if (isSigned) Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF9333EA)))
                                else Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1))),
                                CircleShape
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                if (isSigned && viewModel.signedInUserPhoto.isNotEmpty()) viewModel.signedInUserPhoto
                                else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
                            ),
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            colorFilter = if (!isSigned) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    // Small Rank Badge Overlay attached to Profile Logo
                    val activeBadge = viewModel.currentActiveBadge
                    if (activeBadge.imageUrl.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    1.5.dp,
                                    if (isSigned) Color(activeBadge.colorHex) else Color(0xFF94A3B8),
                                    CircleShape
                                )
                                .padding(2.dp)
                                .clickable {
                                    if (isSigned) {
                                        viewModel.showPrimeBadgesDialog = true
                                    } else {
                                        viewModel.showSignupScreen = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(activeBadge.imageUrl),
                                contentDescription = activeBadge.levelName,
                                contentScale = ContentScale.Fit,
                                colorFilter = if (!isSigned) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. NAME, USERNAME, EMAIL SECTION
                if (isSigned) {
                    val displayName = viewModel.signedInUserName.ifEmpty { "CloudiHub User" }
                    val displayUsername = if (viewModel.signedInUserName.isNotEmpty()) {
                        "@${viewModel.signedInUserName.lowercase().replace(" ", "_")}"
                    } else {
                        "@cloudihub_user"
                    }
                    val displayEmail = viewModel.signedInUserEmail.ifEmpty { "user@cloudihub.com" }

                    Text(
                        text = displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = displayUsername,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0284C7),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = displayEmail,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Guest User",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "@guest_account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0284C7),
                        modifier = Modifier.clickable { viewModel.showSignupScreen = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Sign In",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign In / Register",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 3. BADGES SECTION
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSigned) Icons.Default.EmojiEvents else Icons.Default.Lock,
                        contentDescription = "Badges",
                        tint = if (isSigned) Color(0xFFD97706) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BADGES & REWARDS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSigned) Color(0xFF475569) else Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = if (isSigned) "View Shop ➔" else "Unlock Tiers 🔒",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSigned) Color(0xFF0284C7) else Color(0xFF64748B),
                    modifier = Modifier.clickable {
                        if (isSigned) viewModel.showPrimeBadgesDialog = true
                        else viewModel.showSignupScreen = true
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isSigned) Color(0xFFE2E8F0) else Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
                    .then(if (!isSigned) Modifier.graphicsLayer { alpha = 0.65f } else Modifier)
                    .clickable {
                        if (!isSigned) viewModel.showSignupScreen = true
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSigned) Color.White else Color(0xFFF8FAFC))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(viewModel.availablePrimeBadges) { badge ->
                            val isUnlocked = isSigned && viewModel.unlockedBadges.any { it.id == badge.id }
                            val isActive = isSigned && viewModel.userPrimeLevel == badge.levelName

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isActive) Color(badge.colorHex).copy(alpha = 0.15f)
                                        else if (isUnlocked) Color(0xFFF8FAFC)
                                        else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    width = if (isActive) 1.8.dp else 1.dp,
                                    color = if (isActive) Color(badge.colorHex)
                                            else if (isUnlocked) Color(badge.colorHex).copy(alpha = 0.4f)
                                            else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.clickable {
                                    if (isSigned) viewModel.showPrimeBadgesDialog = true
                                    else viewModel.showSignupScreen = true
                                }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = rememberAsyncImagePainter(badge.imageUrl),
                                            contentDescription = badge.badgeTitle,
                                            contentScale = ContentScale.Fit,
                                            colorFilter = if (!isSigned) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        if (!isUnlocked || !isSigned) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = badge.badgeTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSigned && isUnlocked) Color(0xFF0F172A) else Color(0xFF64748B)
                                    )

                                    Text(
                                        text = if (isActive) "ACTIVE"
                                               else if (isUnlocked) "UNLOCKED"
                                               else if (!isSigned) "LOCKED"
                                               else if (badge.price == 0.0) "FREE"
                                               else "$${badge.price}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isActive) Color(badge.colorHex)
                                                else if (isUnlocked) Color(0xFF059669)
                                                else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. LEVEL SECTION
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSigned) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = "Level",
                        tint = if (isSigned) Color(0xFF0284C7) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LEVEL & RANK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSigned) Color(0xFF475569) else Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                }

                val activeBadge = viewModel.currentActiveBadge
                Text(
                    text = if (isSigned) activeBadge.levelName else "Locked 🔒",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSigned) Color(activeBadge.colorHex) else Color(0xFF64748B)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isSigned) Color(0xFFE2E8F0) else Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
                    .then(if (!isSigned) Modifier.graphicsLayer { alpha = 0.65f } else Modifier)
                    .clickable {
                        if (!isSigned) viewModel.showSignupScreen = true
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSigned) Color.White else Color(0xFFF8FAFC))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val activeBadge = viewModel.currentActiveBadge
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSigned) Color(activeBadge.colorHex).copy(alpha = 0.12f)
                                        else Color(0xFFE2E8F0)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSigned) Color(activeBadge.colorHex).copy(alpha = 0.3f)
                                        else Color(0xFFCBD5E1),
                                        CircleShape
                                    )
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(activeBadge.imageUrl),
                                    contentDescription = activeBadge.levelName,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (!isSigned) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isSigned) "Level 5 • ${activeBadge.badgeTitle}" else "Level Locked • Sign In",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSigned) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                                Text(
                                    text = if (isSigned) "XP Progress: 3,450 / 5,000 XP" else "Sign in to earn XP & unlock perks",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSigned) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSigned) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable {
                                if (isSigned) viewModel.showPrimeBadgesDialog = true
                                else viewModel.showSignupScreen = true
                            }
                        ) {
                            Text(
                                text = if (isSigned) "Upgrade ⚡" else "Sign In 🔒",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSigned) Color(0xFF059669) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = if (isSigned) 0.69f else 0f,
                        color = if (isSigned) Color(viewModel.currentActiveBadge.colorHex) else Color(0xFF94A3B8),
                        trackColor = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSigned) "Next Tier: Level 6 • Diamond Elite" else "Unlock ranks by signing in",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isSigned) "69%" else "0%",
                            fontSize = 11.sp,
                            color = if (isSigned) Color(viewModel.currentActiveBadge.colorHex) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PHYSICAL PHONE STORAGE QUOTA CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Device Storage",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CloudeHub Storage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F9FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${storageInfo.percentUsed}% Used",
                            color = Color(0xFF0284C7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = storageInfo.percentUsed / 100f,
                    color = Color(0xFF0284C7),
                    trackColor = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${storageInfo.usedGB} GB Used",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${storageInfo.totalGB} GB Total",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- STATS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard(
                title = "Offline Files",
                value = "${viewModel.downloadItems.size} files",
                icon = Icons.Default.CloudQueue,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.activeProfilePage = "offline_folders" }
            )

            ProfileStatCard(
                title = "Linked Devices",
                value = "3 Active",
                icon = Icons.Default.DeviceHub,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.biometricAuthTarget = "linked_devices"
                    viewModel.showFingerprintAuth = true
                }
            )

            AnimatedVisibility(
                visible = !viewModel.isVaultCardHidden,
                enter = fadeIn(animationSpec = spring(dampingRatio = 0.62f, stiffness = 220f)) + 
                        expandHorizontally(expandFrom = Alignment.CenterHorizontally, animationSpec = spring(dampingRatio = 0.62f, stiffness = 220f)) + 
                        scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.52f, stiffness = 160f)),
                exit = fadeOut(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)) + 
                       shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally, animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)) + 
                       scaleOut(targetScale = 0.6f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)),
                modifier = if (viewModel.isVaultCardHidden) Modifier.width(0.dp) else Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown()
                                    pressJob?.cancel()
                                    pressJob = coroutineScope.launch {
                                        delay(3000)
                                        showLongPressOptions = true
                                        try {
                                            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                            } else {
                                                vibrator.vibrate(100)
                                            }
                                        } catch (e: Exception) {}
                                    }
                                    
                                    val up = waitForUpOrCancellation()
                                    pressJob?.cancel()
                                    if (up != null && !showLongPressOptions) {
                                        // Regular click!
                                        if (!viewModel.isPrivateVaultSetup) {
                                            viewModel.showPrivateVaultPasswordTypeDialog = true
                                        } else if (viewModel.privateVaultPasswordType == "Biometric" || viewModel.privateVaultBiometricEnabled) {
                                            viewModel.biometricAuthTarget = "private_vault"
                                            viewModel.showFingerprintAuth = true
                                        } else {
                                            viewModel.showPrivateVaultUnlockDialog = true
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ProfileStatCard(
                        title = "Private Vault",
                        value = if (viewModel.isPrivateVaultSetup) "Protected" else "Locked",
                        icon = Icons.Default.Lock,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = null
                    )

                    if (showLongPressOptions) {
                        androidx.compose.ui.window.Popup(
                            alignment = Alignment.Center,
                            onDismissRequest = { showLongPressOptions = false },
                            properties = androidx.compose.ui.window.PopupProperties(
                                focusable = true,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            Card(
                                modifier = Modifier
                                    .width(136.dp)
                                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Hide Vault?",
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { showLongPressOptions = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(28.dp)
                                        ) {
                                            Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = {
                                                showLongPressOptions = false
                                                viewModel.updateVaultCardHidden(true)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Vault hidden! Tap 3 times on the Top Sign-In Card to unlock.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(28.dp)
                                        ) {
                                            Text("Hide", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- SERVICES SECTION ---
        Text(
            text = "PREMIUM UTILITIES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                ProfileMenuItem(
                    icon = if (viewModel.isGoogleSignedIn) Icons.Default.Star else Icons.Default.Lock,
                    title = "Prime Level & Badges Shop",
                    subtitle = if (viewModel.isGoogleSignedIn)
                        "Active: ${viewModel.userPrimeLevel} • Upgrade badges & unlock perks"
                    else
                        "🔒 Locked • Sign in to view level, balance & badges",
                    iconTint = if (viewModel.isGoogleSignedIn) Color(0xFFD97706) else Color(0xFF64748B),
                    onClick = {
                        if (viewModel.isGoogleSignedIn) {
                            viewModel.showPrimeBadgesDialog = true
                        } else {
                            viewModel.showSignupScreen = true
                            Toast.makeText(context, "Sign up/in required to access Level, Balance & Badges!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    lottieUrl = LOTTIE_OVERLAY_1_URL,
                    title = "AI Copilot & Permissions",
                    subtitle = if (viewModel.isAiAssistantEnabled) "Active • Interactive mascot & auto-redirect controls" else "Disabled • Click to configure permissions",
                    onClick = { viewModel.activeProfilePage = "ai_settings" }
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    imageUrl = "https://i.postimg.cc/G2tMPzZm/Fast-Delivery-icon-concept-in-black-duo-line-color.jpg",
                    title = "Watch Later",
                    subtitle = "${viewModel.watchLaterVideos.size} saved videos queued",
                    onClick = { viewModel.activeProfilePage = "watch_later" }
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    lottieUrl = "https://lottie.host/ad77edea-d745-4c02-9a37-18b18633daca/fhDPaqcl5V.lottie",
                    title = "Invite Friend",
                    subtitle = "Invite friends & earn free storage",
                    onClick = { viewModel.activeProfilePage = "refer" }
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Default.CloudQueue,
                    title = "Downloads",
                    subtitle = "Manage downloaded videos, music, files & folders",
                    iconTint = Color(0xFF0284C7),
                    onClick = {
                        viewModel.showFullScreenDownloads = true
                        viewModel.activeProfilePage = "downloads"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PREFERENCES SECTION ---
        Text(
            text = "SYSTEM PREFERENCES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.DeviceHub,
                    title = "Cloud Services Hub",
                    subtitle = "Manage your active external cloud portals",
                    iconTint = Color(0xFF6366F1),
                    onClick = { viewModel.showCloudHubInProfile = true }
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Default.Refresh,
                    title = "Activity Logs History",
                    subtitle = "Review recently played tracks & visited pages",
                    iconTint = Color(0xFF0284C7),
                    onClick = { viewModel.showHistoryPopup = true }
                )
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Feedback & Rating",
                    subtitle = "Submit star rating & improve Cloudihub",
                    iconTint = Color(0xFFEC4899),
                    onClick = { viewModel.showFeedbackPopup = true }
                )
            }
        }

        // Show Logout button at the very bottom of profile screen without background
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = {
                if (viewModel.isGoogleSignedIn) {
                    viewModel.signOutGoogle()
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.showSignupScreen = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (viewModel.isGoogleSignedIn) Icons.Default.ExitToApp else Icons.Default.Login,
                    contentDescription = if (viewModel.isGoogleSignedIn) "Logout" else "Login / Sign Up",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (viewModel.isGoogleSignedIn) "Logout" else "Log In / Sign Up",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}
}

// --- SUB-SCREEN: INVITE FRIEND ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferScreen(viewModel: CloudihubViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val inviteLink = "https://cloudihub.com/invite/aaliyah_71"
    var showQrSheet by remember { mutableStateOf(false) }
    val referPagerState = rememberPagerState(pageCount = { 2 })
    val referScope = rememberCoroutineScope()
    val selectedTab = referPagerState.currentPage

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

    // Re-key animation each time this composable enters composition so it restarts from frame 0 and freezes on last frame
    val playKey = remember { System.currentTimeMillis() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // Toolbar (Seamless background)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.activeProfilePage = "main" },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Invite Friend",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Main Top Lottie Animation Container (300x300dp) - Freezes on last frame
                key(playKey) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DotLottieAnimation(
                            source = DotLottieSource.Url("https://lottie.host/ad77edea-d745-4c02-9a37-18b18633daca/fhDPaqcl5V.lottie"),
                            autoplay = true,
                            loop = false,
                            modifier = Modifier.size(280.dp)
                        )
                    }
                }

                // Referral Earn Balance Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.5.dp, Color(0xFFA7F3D0), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Wallet",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Referral Wallet Balance",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = viewModel.userBalance,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "${viewModel.referralCount} Invites",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.addReferralReward()
                                    Toast.makeText(context, "Referral success! +$5.00 added to wallet balance!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Simulate Invite (+$5.00)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.showPrimeBadgesDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFD97706))
                            ) {
                                Text("Buy Prime Badges", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Row: Clean, NO CIRCLES / NO BORDER CONTAINERS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Copy Link Action (Clean Aesthetic Icon)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(inviteLink))
                                Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Copy Link",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }

                    // 2. QR Code Action (Lottie)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showQrSheet = true
                            }
                            .padding(12.dp)
                    ) {
                        DotLottieAnimation(
                            source = DotLottieSource.Url("https://lottie.host/3d35cfc5-5c28-46d5-8f17-326b5cfa85c3/f7QNSkcuIe.lottie"),
                            autoplay = true,
                            loop = true,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "QR Code",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }

                    // 3. Share Action (Lottie from Video Playback Share Button)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Join Cloudihub")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Join Cloudihub for high-speed cloud storage and video streaming: $inviteLink")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Invite Link"))
                            }
                            .padding(12.dp)
                    ) {
                        DotLottieAnimation(
                            source = DotLottieSource.Url("https://lottie.host/0753d442-1101-40ac-a517-f21d4be97ef8/D1QEnYRtVw.lottie"),
                            autoplay = true,
                            loop = true,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Detailed Modern Instructions & Features Card (NO EMOJIS)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Referral Program Overview",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Invite friends to expand your personal storage quota and access shared high-speed media streaming features.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Program Instructions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val instructions = listOf(
                            "Step 1: Copy your referral link or display the QR code using the action buttons above.",
                            "Step 2: Share the link or QR code with friends, family, or colleagues.",
                            "Step 3: Once your invitee registers a Cloudihub account using your link, verified quota bonus is credited automatically.",
                            "Step 4: You receive 5 GB of extra cloud storage for each verified referral, up to 100 GB total bonus."
                        )

                        instructions.forEach { item ->
                            Text(
                                text = item,
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Technical Details & Rules",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val details = listOf(
                            "Quota Credit: Instant allocation upon unique device registration.",
                            "Bandwidth Tier: Unlocks high-priority network nodes for video streaming and file transfers.",
                            "Privacy Protection: Invitee data remains private and strictly end-to-end encrypted.",
                            "Expiration: Bonus cloud storage allocated via referral is permanent and never expires."
                        )

                        details.forEach { detail ->
                            Text(
                                text = detail,
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- NEW BOTTOM SECTION: MY FRIENDS & TOP REFERRERS TABBED SLIDER ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Small dushor/grey switcher buttons with icons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .padding(3.dp)
                        ) {
                            // Tab 1: My Friends
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedTab == 0) Color(0xFFCBD5E1) else Color.Transparent
                                    )
                                    .clickable {
                                        referScope.launch { referPagerState.animateScrollToPage(0) }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = "My Friends",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "My Friends",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }

                            // Tab 2: Top Referrers
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedTab == 1) Color(0xFFCBD5E1) else Color.Transparent
                                    )
                                    .clickable {
                                        referScope.launch { referPagerState.animateScrollToPage(1) }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Top Referrers",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Top Referrers",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalPager(
                            state = referPagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            if (page == 0) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val myFriends = listOf(
                                        Triple("Sadia Islam", "https://i.postimg.cc/1zmgjdRt/f6d0aee22a954d7db19f3c210f9d876e.jpg", "Joined 2 days ago"),
                                        Triple("Tanvir Ahmed", "https://i.postimg.cc/s2xM1LhJ/avatar1.jpg", "Joined 5 days ago"),
                                        Triple("Nabil Chowdhury", "https://i.postimg.cc/44yJ3mpt/avatar2.jpg", "Joined 1 week ago")
                                    )

                                    Text(
                                        text = "Invited Friends (${myFriends.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    myFriends.forEachIndexed { index, friend ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(friend.second),
                                                contentDescription = friend.first,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = friend.first,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = friend.third,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(Color(0xFFECFDF5))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "+5 GB Active",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF059669)
                                                )
                                            }
                                        }
                                        if (index < myFriends.size - 1) {
                                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                        }
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val topReferrers = listOf(
                                        Quadruple("#1", "Zubair Rahman", "48 Invites • 240 GB Earned", Color(0xFFF59E0B)),
                                        Quadruple("#2", "Fariha Chowdhury", "35 Invites • 175 GB Earned", Color(0xFF94A3B8)),
                                        Quadruple("#3", "Mahmudul Hasan", "28 Invites • 140 GB Earned", Color(0xFFD97706)),
                                        Quadruple("#4", "Anika Tabassum", "21 Invites • 105 GB Earned", Color(0xFF64748B))
                                    )

                                    Text(
                                        text = "Global Leaderboard",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    topReferrers.forEachIndexed { index, user ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(user.fourth.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.first,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = user.fourth
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = user.second,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = user.third,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF0284C7),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        if (index < topReferrers.size - 1) {
                                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

    // Dynamic Sliding Bottom Sheet Modal for QR Code with Glowing Blue Shadow
    if (showQrSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQrSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan QR Code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(
                        onClick = { showQrSheet = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Sheet",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Highlighted floating QR code with glowing blue shadow effect
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0xFF0284C7),
                            ambientColor = Color(0xFF0284C7)
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFF0F9FF), Color.White)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VisualQRCode(modifier = Modifier.size(170.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "How to Scan & Claim Bonus",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val scanSteps = listOf(
                    "Step 1: Open the Camera application or any QR Code scanner on another mobile device.",
                    "Step 2: Position the camera lens over the highlighted QR code above.",
                    "Step 3: Tap the detected link banner to accept the invite and instantly claim 5 GB bonus cloud storage."
                )

                scanSteps.forEach { step ->
                    Text(
                        text = step,
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showQrSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun VisualQRCode(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(160.dp)
            .background(Color.White)
            .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(5) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) { colIndex ->
                        val isCorner = (rowIndex == 0 && colIndex == 0) || 
                                       (rowIndex == 0 && colIndex == 4) || 
                                       (rowIndex == 4 && colIndex == 0)
                        val isCenterDot = rowIndex == 2 && colIndex == 2
                        val isRandomDot = (rowIndex + colIndex) % 3 == 0
                        
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(if (isCorner) 6.dp else 2.dp))
                                .background(
                                    if (isCorner) Color(0xFF0F172A)
                                    else if (isCenterDot) Color(0xFF0284C7)
                                    else if (isRandomDot) Color(0xFF334155)
                                    else Color(0xFFF1F5F9)
                                )
                        ) {
                            if (isCorner) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.Center)
                                        .background(Color.White, CircleShape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .align(Alignment.Center)
                                            .background(Color(0xFF0F172A), CircleShape)
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

// --- SUB-SCREEN: OFFLINE DOWNLOADS SCREEN ---
// Handled by com.example.ui.screens.DownloadsScreen.kt

// --- SLIDING BOTTOM SHEETS (ModalBottomSheet with native drag handles to slide down & close) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionBottomSheet(
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isYearlyPlan by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = {
            // Drag handle line to dismiss popup easily
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upgrade to Sky Premium",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = "Enjoy endless speed pipelines and unlimited offline cache",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Plan switcher slider
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { isYearlyPlan = !isYearlyPlan }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isYearlyPlan) Color.White else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Monthly", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isYearlyPlan) Color(0xFF0284C7) else Color(0xFF64748B))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isYearlyPlan) Color.White else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Yearly (-20%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isYearlyPlan) Color(0xFF0284C7) else Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plans comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Plan 1
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Basic Sky", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isYearlyPlan) "$24.00/yr" else "$2.50/mo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        listOf("100 GB space", "Standard speed", "No Ad popups").forEach { feat ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(feat, fontSize = 10.sp, color = Color(0xFF475569))
                            }
                        }
                    }
                }

                // Plan 2 Premium Highlighted
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.5.dp, Color(0xFF0284C7), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Extreme Pro", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF0284C7)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text("BEST", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isYearlyPlan) "$79.00/yr" else "$7.99/mo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        listOf("2 TB cloud", "Extreme pipeline", "Security Lock", "VIP Cloud Hub").forEach { feat ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF0284C7), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(feat, fontSize = 10.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Sky Premium subscription activated successfully!", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Activate Plan Now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Recent Cloud Activities",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your local browsing, streaming & video history logs",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities recorded yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.historyItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (item.type) {
                                            "Music" -> Color(0xFFECFDF5)
                                            "Video" -> Color(0xFFEFF6FF)
                                            else -> Color(0xFFFEF3C7)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        "Music" -> Icons.Default.Star // music/song icon
                                        "Video" -> Icons.Default.PlayArrow
                                        else -> Icons.Default.Refresh // browser reload / refresh icon
                                    },
                                    contentDescription = null,
                                    tint = when (item.type) {
                                        "Music" -> Color(0xFF10B981)
                                        "Video" -> Color(0xFF3B82F6)
                                        else -> Color(0xFFD97706)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = item.timestamp,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rating by remember { mutableIntStateOf(5) }
    var description by remember { mutableStateOf("") }
    var improvementSuggestion by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Help us improve Cloudihub",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = "We value your rating & feature suggestions",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Stars
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    val isFilled = starIndex <= rating
                    Icon(
                        imageVector = if (isFilled) Icons.Default.Star else Icons.Default.Star, // we style tint
                        contentDescription = "Star $starIndex",
                        tint = if (isFilled) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { rating = starIndex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rating message description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What did you like the most?") },
                placeholder = { Text("E.g. cloud navigation speed, glass design...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Improvement box
            OutlinedTextField(
                value = improvementSuggestion,
                onValueChange = { improvementSuggestion = it },
                label = { Text("How can we improve?") },
                placeholder = { Text("E.g. add support for OneDrive sync...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Thank you! Your ${rating}-star feedback submitted successfully.", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Review", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// --- SHARED REUSABLE LAYOUT PARTS ---

@Composable
fun ProfileStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val finalModifier = if (onClick != null) {
        modifier
            .clickable { onClick() }
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
    } else {
        modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
    }
    
    Card(
        modifier = finalModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F9FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector? = null,
    imageUrl: String? = null,
    lottieUrl: String? = null,
    title: String,
    subtitle: String,
    iconTint: Color = Color(0xFF475569),
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            if (!lottieUrl.isNullOrEmpty()) {
                val lottieComp by rememberLottieComposition(LottieCompositionSpec.Url(lottieUrl))
                LottieAnimation(
                    composition = lottieComp,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(36.dp)
                )
            } else if (!imageUrl.isNullOrEmpty()) {
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
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

class WavyBannerShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val r = 24.dp.value * density.density // Corner radius
            val w = size.width
            val h = size.height
            val dip = 32.dp.value * density.density // Vertical shift for the curve
            
            // Top-left corner
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            
            // Top edge to the start of the curve (around 68% of width)
            lineTo(w * 0.68f, 0f)
            
            // Elegant S-curve down to the lower level
            cubicTo(
                w * 0.74f, 0f,
                w * 0.76f, dip,
                w * 0.82f, dip
            )
            
            // Straight edge of the lower shelf to the top-right corner
            lineTo(w - r, dip)
            quadraticTo(w, dip, w, dip + r)
            
            // Right edge down to bottom-right corner
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            
            // Bottom edge to bottom-left corner
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun CustomThemeToggle(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offset by animateDpAsState(targetValue = if (isDark) 26.dp else 2.dp, label = "toggleOffset")
    val bgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val circleColor = if (isDark) Color(0xFF64748B) else Color(0xFFFBBF24)
    
    Box(
        modifier = modifier
            .width(54.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0), CircleShape)
            .clickable { onToggle() }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Background icons
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = if (isDark) Color(0xFF64748B) else Color(0xFFFBBF24),
                modifier = Modifier.size(12.dp)
            )
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(12.dp)
            )
        }
        
        // Sliding handle
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(24.dp)
                .clip(CircleShape)
                .background(circleColor)
                .shadow(1.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.NightsStay else Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun AestheticBannerButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDark: Boolean,
    contentColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "buttonScale"
    )
    
    val accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val buttonBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF0F9FF)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE0F2FE)
    val iconColor = if (isDark) Color.White else Color(0xFF0284C7)
    val textColor = if (isDark) Color.White else Color(0xFF0284C7)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun WavySignupBanner(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = viewModel.isDarkTheme
    
    // Glowing sky color shimmer transition mimicking Gemini's premium glow in sky blue!
    val infiniteTransition = rememberInfiniteTransition(label = "SkyGlow")
    val glowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlowProgress"
    )
    
    // We animate the gradient positions to create a continuous fluid flash/glow flow in Sky Color
    val glowBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0284C7).copy(alpha = 0.2f),
            Color(0xFF38BDF8),
            Color(0xFF0EA5E9),
            Color(0xFF38BDF8),
            Color(0xFF0284C7).copy(alpha = 0.2f)
        ),
        start = Offset(glowProgress * 1000f - 500f, 0f),
        end = Offset(glowProgress * 1000f + 500f, 500f)
    )

    val cardBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.25f) else Color.White
    val contentColor = if (isDark) Color.White else Color(0xFF334155)
    val accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFE2E8F0)
    
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.8.dp, glowBrush, RoundedCornerShape(24.dp))
            .shadow(if (isDark) 12.dp else 4.dp, RoundedCornerShape(24.dp), clip = false)
            .clickable {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 500) {
                    tapCount += 1
                } else {
                    tapCount = 1
                }
                lastTapTime = currentTime
                if (tapCount == 3) {
                    tapCount = 0
                    if (viewModel.isVaultCardHidden) {
                        try {
                            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                vibrator.vibrate(100)
                            }
                        } catch (e: Exception) {}
                        
                        if (!viewModel.isPrivateVaultSetup) {
                            viewModel.showPrivateVaultPasswordTypeDialog = true
                        } else if (viewModel.privateVaultPasswordType == "Biometric" || viewModel.privateVaultBiometricEnabled) {
                            viewModel.biometricAuthTarget = "private_vault"
                            viewModel.showFingerprintAuth = true
                        } else {
                            viewModel.showPrivateVaultUnlockDialog = true
                        }
                    }
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
        ) {
            // Top row with Sign In on Left and 3 Tiny Badges on Right Edge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sign In Button (Pill shaped with accent outline)
                Box(
                    modifier = Modifier
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF0F9FF), CircleShape)
                        .border(1.5.dp, if (isDark) accentColor else Color(0xFFBAE6FD), CircleShape)
                        .clip(CircleShape)
                        .clickable { viewModel.showSignupScreen = true }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Sign Up",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sign up",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) contentColor else accentColor
                        )
                    }
                }
                
                // 3 Tiny Badges (Level, Prime, Balance) on top right edge
                UserProfileMetricsRow(viewModel = viewModel)
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Horizontal divider line
            Divider(
                color = borderColor,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Bottom buttons (Aesthetic, left-aligned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AestheticBannerButton(
                    icon = Icons.Default.FolderOpen,
                    text = "My Files",
                    onClick = { viewModel.activeProfilePage = "downloads" },
                    isDark = isDark,
                    contentColor = contentColor
                )
                
                AestheticBannerButton(
                    icon = Icons.Default.History,
                    text = "History",
                    onClick = { viewModel.showHistoryPopup = true },
                    isDark = isDark,
                    contentColor = contentColor
                )
                
                AestheticBannerButton(
                    icon = Icons.Default.QueueMusic,
                    text = "Playlist",
                    onClick = { viewModel.selectTab(NavigationTab.Music) },
                    isDark = isDark,
                    contentColor = contentColor
                )
            }
        }
    }
}


// =========================================================================
// BIO-AUTHENTICATOR & REMOTE DEVICE LOGOUT FLOW (AS REQUESTED)
// =========================================================================

@Composable
fun BiometricAuthDialog(viewModel: CloudihubViewModel) {
    var isPulsing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPulsing) 1.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    LaunchedEffect(Unit) {
        isPulsing = true
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { viewModel.showFingerprintAuth = false }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                
                // Pulse fingerprint icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                        .clickable {
                            // Simulate successful auth!
                            viewModel.showFingerprintAuth = false
                            viewModel.activeProfilePage = "linked_devices"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Scan Fingerprint",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Biometric Verification",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Touch and hold the fingerprint scanner or tap below to authenticate and manage active sessions securely.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.showFingerprintAuth = false
                        viewModel.activeProfilePage = "linked_devices"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Authenticate Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { viewModel.showFingerprintAuth = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedDevicesScreen(viewModel: CloudihubViewModel) {
    val context = LocalContext.current
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var deviceToLogOut by remember { mutableStateOf<CloudihubViewModel.LinkedDevice?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.activeProfilePage = "main" },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Linked Active Devices",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        // Subheader intro banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "These devices are currently signed into your Cloudihub account. You can remotely revoke access to secure your cloud pipelines.",
                    fontSize = 12.sp,
                    color = Color(0xFF0369A1),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Devices List
        // Devices List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.linkedDevices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .shadow(2.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Hardware device picture
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(device.imageUrl),
                                    contentDescription = "Device Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = device.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (device.isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Current",
                                                color = Color(0xFF15803D),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                // OS and details badge
                                Text(
                                    text = device.os,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7)
                                )
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Text(
                                    text = "${device.lastActive} • ${device.location}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        if (!device.isCurrent) {
                            Button(
                                onClick = {
                                    deviceToLogOut = device
                                    newPassword = ""
                                    confirmPassword = ""
                                    showPasswordChangeDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFEE2E2),
                                    contentColor = Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Logout",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // iOS-Style Bottom Sheet Verification for Logout / Password Change
    if (showPasswordChangeDialog && deviceToLogOut != null) {
        ModalBottomSheet(
            onDismissRequest = { showPasswordChangeDialog = false },
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Security Verification",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "To securely sign out '${deviceToLogOut?.name}', please set a new account password. This ensures other active sessions are safely terminated and revoked.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                // Beautiful high-contrast device detail preview card as requested
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(deviceToLogOut?.imageUrl),
                                contentDescription = "Device Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = deviceToLogOut?.name ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = deviceToLogOut?.os ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${deviceToLogOut?.lastActive ?: ""} • ${deviceToLogOut?.location ?: ""}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Secure Password", fontSize = 12.sp) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Secure Password", fontSize = 12.sp) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showPasswordChangeDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                                Toast.makeText(context, "Password cannot be empty!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Perform Logout
                            viewModel.linkedDevices.removeIf { it.id == deviceToLogOut?.id }
                            showPasswordChangeDialog = false
                            Toast.makeText(context, "Password changed. Remote session revoked successfully!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Change & Logout", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


// =========================================================================
// OFFLINE MODERN CLOUD FOLDERS (AS REQUESTED)
// =========================================================================

@Composable
fun ModernFolderIcon(
    baseColor: Color = Color(0xFFFBBF24), // Folder yellow/amber
    icon: ImageVector
) {
    Box(
        modifier = Modifier.size(54.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Drawing a real 3D looking folder!
        // 1. Folder Back Flap (Darker shade)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(baseColor.copy(alpha = 0.75f))
        )
        
        // 2. Folder Tab (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp)
                .size(width = 24.dp, height = 14.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(baseColor.copy(alpha = 0.75f))
        )
        
        // 3. Papers peeking out (White sheet)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        )
        
        // 4. Folder Front Flap (Lighter shade, slanted slightly or overlapping)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(baseColor)
                .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .shadow(2.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            // Little icon in center of front flap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun OfflineFoldersScreen(viewModel: CloudihubViewModel) {
    val context = LocalContext.current
    var activeFolderTag by remember { mutableStateOf<String?>(null) } // null means showing folder grid, non-null shows files inside folder
    var activeFolderName by remember { mutableStateOf("") }

    // Mock folder files
    val portalBackups = listOf(
        "DriveBackup_2026_07.zip" to "1.2 GB",
        "Dropbox_Synced_Media.tar" to "850 MB",
        "OneDrive_Archived.zip" to "350 MB"
    )
    val tempCaches = listOf(
        "HlsChunk_Node_18204.log" to "32 MB",
        "HlsChunk_Node_18205.log" to "28 MB",
        "ProxyPipeline_Cache.bin" to "152 MB",
        "AestheticAmbiance_Index.json" to "12 KB"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (activeFolderTag != null) {
                        activeFolderTag = null
                    } else {
                        viewModel.activeProfilePage = "main"
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (activeFolderTag != null) activeFolderName else "Cloude Offline Hub",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        if (activeFolderTag == null) {
            // FOLDERS VIEW
            Column(modifier = Modifier.fillMaxSize()) {
                // Introductory banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "CloudeHub high-speed offline directory. All folders are secure, sandboxed, and locally cached.",
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of folders with cloud-themed styling
                val folders = listOf(
                    Triple("Cloud Backups", "3 archives • 2.4 GB", "backup"),
                    Triple("Saved Streams", "${viewModel.downloadItems.filter { it.type == "Video" }.size} files • 1.8 GB", "video"),
                    Triple("Music & Melodies", "${viewModel.downloadItems.filter { it.type == "Music" }.size} tracks • 256 MB", "music"),
                    Triple("Private Keys & Certs", "${viewModel.downloadItems.filter { it.type == "Private" }.size} keys • 12 KB", "key"),
                    Triple("Temporary Cache Logs", "4 logs • 212 MB", "cache")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(folders) { folder ->
                        val (name, desc, tag) = folder
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeFolderTag = tag
                                    activeFolderName = name
                                }
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (baseColor, icon) = when (tag) {
                                    "backup" -> Color(0xFFFBBF24) to Icons.Default.CloudDownload
                                    "video" -> Color(0xFF38BDF8) to Icons.Default.Movie
                                    "music" -> Color(0xFFF472B6) to Icons.Default.MusicNote
                                    "key" -> Color(0xFF34D399) to Icons.Default.Lock
                                    else -> Color(0xFFA78BFA) to Icons.Default.FolderOpen
                                }

                                ModernFolderIcon(
                                    baseColor = baseColor,
                                    icon = icon
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // INSIDE FOLDER VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Info description of active folder
                Text(
                    text = "Directory: storage/cloudehub/$activeFolderTag/",
                    fontSize = 11.sp,
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val folderTag = activeFolderTag
                val itemsList = if (folderTag == "video") {
                    viewModel.downloadItems.filter { it.type == "Video" }
                } else if (folderTag == "music") {
                    viewModel.downloadItems.filter { it.type == "Music" }
                } else if (folderTag == "key") {
                    viewModel.downloadItems.filter { it.type == "Private" }
                } else {
                    emptyList()
                }

                if (folderTag == "backup" || folderTag == "cache") {
                    val staticList = if (folderTag == "backup") portalBackups else tempCaches
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(staticList) { item ->
                            val (filename, size) = item
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = filename,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "Local Cache Store",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                    Text(
                                        text = size,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else if (itemsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "This folder is empty.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(itemsList) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Downloaded Stream",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.size,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold
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

// ==========================================
// --- PRIVATE VAULT COMPOSABLES (M3) ---
// ==========================================

@Composable
fun IOSBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: CloudihubViewModel,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() },
            contentAlignment = Alignment.BottomCenter
        ) {
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it }
                ) + fadeOut(),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent propagation */ }
                    .fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(), // Full width to touch/blend with screen edges perfectly
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), // Premium iOS-style rounded sheet
                    colors = CardDefaults.cardColors(containerColor = if (viewModel.isDarkTheme) Color(0xFF0F172A) else Color.White),
                    border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding() // CRITICAL: Smoothly pushes keyboard content up inside card
                            .padding(bottom = 16.dp) // Comfort space so fields are well above keyboard
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateVaultPasswordTypeDialog(viewModel: CloudihubViewModel) {
    val isDark = viewModel.isDarkTheme
    val textCol = if (isDark) Color.White else Color(0xFF0F172A)
    val descCol = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentCol = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedType by remember { mutableStateOf("PIN") }
    var inputVal by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    IOSBottomSheet(
        onDismissRequest = { viewModel.showPrivateVaultPasswordTypeDialog = false },
        viewModel = viewModel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instruction section with anim entry
            var showInstructions by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                showInstructions = true
            }

            AnimatedVisibility(
                visible = showInstructions,
                enter = expandVertically() + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(accentCol.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = accentCol,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Setup Secure Private Vault",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textCol
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Encrypted local vault protect. Photos, documents, and recordings stored in this vault cannot be accessed without authorization.",
                        fontSize = 11.sp,
                        color = descCol,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Divider(color = borderCol, modifier = Modifier.padding(bottom = 16.dp))

            // Sleek narrow/small type selector box
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Lock Option",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = descCol,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (selectedType) {
                                "PIN" -> Icons.Default.Dialpad
                                "Password" -> Icons.Default.Password
                                else -> Icons.Default.Fingerprint
                            },
                            contentDescription = null,
                            tint = accentCol,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedType == "Biometric") "Biometric (Fingerprint)" else selectedType,
                            fontSize = 14.sp,
                            color = textCol,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (selectedType == "Biometric") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Recommend",
                                    color = Color(0xFF22C55E),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = descCol,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Dropdown Options
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    ) {
                        val options = listOf("PIN", "Password", "Biometric")
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedType = option
                                        inputVal = ""
                                        isExpanded = false
                                        errorMsg = null
                                        biometricEnabled = (option == "Biometric")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (option) {
                                            "PIN" -> Icons.Default.Dialpad
                                            "Password" -> Icons.Default.Password
                                            else -> Icons.Default.Fingerprint
                                        },
                                        contentDescription = null,
                                        tint = if (selectedType == option) accentCol else descCol,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (option == "Biometric") "Biometric (Fingerprint)" else option,
                                        fontSize = 13.sp,
                                        color = if (selectedType == option) accentCol else textCol,
                                        fontWeight = if (selectedType == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (option == "Biometric") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Recommend",
                                                color = Color(0xFF22C55E),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                if (selectedType == option) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accentCol,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input fields based on option selected
            AnimatedVisibility(
                visible = selectedType == "PIN" || selectedType == "Password",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = {
                            if (selectedType == "PIN") {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    inputVal = it
                                }
                            } else {
                                inputVal = it
                            }
                            errorMsg = null
                        },
                        label = { Text(if (selectedType == "PIN") "Set 6-Digit PIN" else "Set Master Password", color = descCol) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentCol) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (selectedType == "PIN") KeyboardType.Number else KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = descCol
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol,
                            focusedBorderColor = accentCol,
                            unfocusedBorderColor = borderCol
                        )
                    )

                    // No biometric toggle switch displayed here to ensure mutual exclusivity
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMsg ?: "", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.showPrivateVaultPasswordTypeDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textCol),
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (selectedType == "Biometric") {
                            viewModel.savePrivateVaultSettings("Biometric", "", true)
                            viewModel.showPrivateVaultPasswordTypeDialog = false
                            Toast.makeText(context, "Secure Private Vault configured with Biometrics!", Toast.LENGTH_LONG).show()
                        } else {
                            if (selectedType == "PIN" && inputVal.length < 4) {
                                errorMsg = "PIN must be at least 4 digits"
                            } else if (selectedType == "Password" && inputVal.length < 4) {
                                errorMsg = "Password must be at least 4 characters"
                            } else {
                                viewModel.savePrivateVaultSettings(selectedType, inputVal, biometricEnabled)
                                viewModel.showPrivateVaultPasswordTypeDialog = false
                                Toast.makeText(context, "Secure Private Vault configured successfully!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentCol)
                ) {
                    Text("Save Lock", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PatternLockGrid(
    enteredPattern: List<Int>,
    onDotClicked: (Int) -> Unit,
    onClear: () -> Unit,
    isDark: Boolean,
    accentCol: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (enteredPattern.isEmpty()) "Tap dots to connect pattern" else "Pattern sequence: ${enteredPattern.joinToString(" ➔ ")}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accentCol,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 3x3 Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (col in 0 until 3) {
                        val dotIndex = row * 3 + col + 1
                        val isSelected = enteredPattern.contains(dotIndex)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) accentCol.copy(alpha = 0.25f) else Color.Transparent
                                )
                                .border(
                                    2.dp,
                                    if (isSelected) accentCol else (if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                    CircleShape
                                )
                                .clickable { onDotClicked(dotIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentCol else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onClear) {
            Text("Clear Pattern", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PrivateVaultPasswordInputDialog(viewModel: CloudihubViewModel) {
    val isDark = viewModel.isDarkTheme
    val textCol = if (isDark) Color.White else Color(0xFF0F172A)
    val descCol = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentCol = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    var inputVal by remember { mutableStateOf("") }
    val enteredPattern = remember { mutableStateListOf<Int>() }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    IOSBottomSheet(
        onDismissRequest = { viewModel.showPrivateVaultPasswordInputDialog = false },
        viewModel = viewModel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Set Private Vault ${viewModel.tempPasswordType}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textCol
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Establish a master password/PIN/pattern. Do not share this lock credential.",
                fontSize = 12.sp,
                color = descCol
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Custom UI based on Type
            when (viewModel.tempPasswordType) {
                "PIN" -> {
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                inputVal = it
                                errorMsg = null
                            }
                        },
                        label = { Text("6-Digit PIN", color = descCol) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentCol) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol,
                            focusedBorderColor = accentCol,
                            unfocusedBorderColor = borderCol
                        )
                    )
                }
                "Password" -> {
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = {
                            inputVal = it
                            errorMsg = null
                        },
                        label = { Text("Master Password", color = descCol) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentCol) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = descCol
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol,
                            focusedBorderColor = accentCol,
                            unfocusedBorderColor = borderCol
                        )
                    )
                }
                "Pattern" -> {
                    PatternLockGrid(
                        enteredPattern = enteredPattern,
                        onDotClicked = { index ->
                            if (!enteredPattern.contains(index)) {
                                enteredPattern.add(index)
                                errorMsg = null
                            }
                        },
                        onClear = { enteredPattern.clear() },
                        isDark = isDark,
                        accentCol = accentCol
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg ?: "",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        viewModel.showPrivateVaultPasswordInputDialog = false
                        viewModel.showPrivateVaultPasswordTypeDialog = true
                    }
                ) {
                    Text("Back", color = descCol, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val finalPassword = if (viewModel.tempPasswordType == "Pattern") {
                            enteredPattern.joinToString("-")
                        } else {
                            inputVal
                        }

                        if (viewModel.tempPasswordType == "PIN" && finalPassword.length < 4) {
                            errorMsg = "PIN must be at least 4 digits long."
                        } else if (viewModel.tempPasswordType == "Password" && finalPassword.length < 4) {
                            errorMsg = "Password must be at least 4 characters."
                        } else if (viewModel.tempPasswordType == "Pattern" && enteredPattern.size < 3) {
                            errorMsg = "Connect at least 3 dots for secure pattern."
                        } else {
                            viewModel.savePrivateVaultSettings(
                                type = viewModel.tempPasswordType,
                                passwordVal = finalPassword,
                                biometric = viewModel.tempBiometricEnabled
                            )
                            // Close dialogs and enter vault!
                            viewModel.showPrivateVaultPasswordInputDialog = false
                            viewModel.showPrivateVaultPasswordTypeDialog = false
                            viewModel.activeProfilePage = "private_vault"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentCol),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Open", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrivateVaultUnlockDialog(viewModel: CloudihubViewModel) {
    val isDark = viewModel.isDarkTheme
    val textCol = if (isDark) Color.White else Color(0xFF0F172A)
    val descCol = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentCol = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    var inputVal by remember { mutableStateOf("") }
    val enteredPattern = remember { mutableStateListOf<Int>() }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Auto-trigger biometric lock on open if enabled!
    LaunchedEffect(Unit) {
        if (viewModel.privateVaultPasswordType == "Biometric" || viewModel.privateVaultBiometricEnabled) {
            viewModel.biometricAuthTarget = "private_vault"
            viewModel.showFingerprintAuth = true
            viewModel.showPrivateVaultUnlockDialog = false
        }
    }

    if (viewModel.privateVaultPasswordType == "Biometric") return

    LaunchedEffect(viewModel.activeProfilePage) {
        if (viewModel.activeProfilePage == "private_vault") {
            viewModel.showPrivateVaultUnlockDialog = false
        }
    }

    IOSBottomSheet(
        onDismissRequest = { viewModel.showPrivateVaultUnlockDialog = false },
        viewModel = viewModel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = accentCol,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Unlock Private Vault",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textCol
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enter your ${viewModel.privateVaultPasswordType} to view private files",
                fontSize = 12.sp,
                color = descCol,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Input based on setup password type
            when (viewModel.privateVaultPasswordType) {
                "PIN" -> {
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                inputVal = it
                                errorMsg = null
                            }
                        },
                        label = { Text("Enter PIN", color = descCol) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentCol) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol,
                            focusedBorderColor = accentCol,
                            unfocusedBorderColor = borderCol
                        )
                    )
                }
                "Password" -> {
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = {
                            inputVal = it
                            errorMsg = null
                        },
                        label = { Text("Enter Password", color = descCol) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentCol) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = descCol
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol,
                            focusedBorderColor = accentCol,
                            unfocusedBorderColor = borderCol
                        )
                    )
                }
                "Biometric" -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(accentCol.copy(alpha = 0.1f))
                                .clickable {
                                    viewModel.biometricAuthTarget = "private_vault"
                                    viewModel.showFingerprintAuth = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Scan Fingerprint",
                                tint = accentCol,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap fingerprint icon to unlock",
                            fontSize = 13.sp,
                            color = descCol,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                "Pattern" -> {
                    PatternLockGrid(
                        enteredPattern = enteredPattern,
                        onDotClicked = { index ->
                            if (!enteredPattern.contains(index)) {
                                enteredPattern.add(index)
                                errorMsg = null
                            }
                        },
                        onClear = { enteredPattern.clear() },
                        isDark = isDark,
                        accentCol = accentCol
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg ?: "",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biometric Retry Button if enabled
            if (viewModel.privateVaultBiometricEnabled && viewModel.privateVaultPasswordType != "Biometric") {
                TextButton(
                    onClick = {
                        viewModel.biometricAuthTarget = "private_vault"
                        viewModel.showFingerprintAuth = true
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, null, tint = accentCol, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Fingerprint Scanner", color = accentCol, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { viewModel.showPrivateVaultUnlockDialog = false }
                ) {
                    Text("Cancel", color = descCol, fontWeight = FontWeight.Bold)
                }

                if (viewModel.privateVaultPasswordType != "Biometric") {
                    Button(
                        onClick = {
                            val attempt = if (viewModel.privateVaultPasswordType == "Pattern") {
                                enteredPattern.joinToString("-")
                            } else {
                                inputVal
                            }

                            if (attempt == viewModel.privateVaultPassword) {
                                viewModel.showPrivateVaultUnlockDialog = false
                                viewModel.activeProfilePage = "private_vault"
                            } else {
                                errorMsg = "Verification failed! Incorrect lock credential."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentCol),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Unlock", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VaultLottieLoadingView(
    title: String = "Searching & Decrypting Vault Files...",
    subtitle: String = "Scanning local storage for encrypted items...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            DotLottieAnimation(
                source = DotLottieSource.Url("https://lottie.host/8de591c2-9e48-4634-b9a3-793c7ab3d0f2/7MenLSHWm0.lottie"),
                autoplay = true,
                loop = true,
                modifier = Modifier.size(240.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SelectVaultFilesDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<CloudihubViewModel.VaultItem>) -> Unit
) {
    val sampleAvailableFiles = remember {
        listOf(
            CloudihubViewModel.VaultItem(folderId = "sample", title = "Family_Vacation_2026.mp4", size = "45.2 MB", type = "Videos", date = "2026-07-24"),
            CloudihubViewModel.VaultItem(folderId = "sample", title = "Confidential_Client_Clip.mp4", size = "88.0 MB", type = "Videos", date = "2026-07-24"),
            CloudihubViewModel.VaultItem(folderId = "sample", title = "Private_Voice_Recording.m4a", size = "8.4 MB", type = "Audio", date = "2026-07-24"),
            CloudihubViewModel.VaultItem(folderId = "sample", title = "Bank_Account_Passcode.pdf", size = "2.1 MB", type = "Documents", date = "2026-07-24"),
            CloudihubViewModel.VaultItem(folderId = "sample", title = "Secret_Photo_Album.jpg", size = "5.6 MB", type = "Photos", date = "2026-07-24"),
            CloudihubViewModel.VaultItem(folderId = "sample", title = "Crypto_Vault_Backup.key", size = "12 KB", type = "Notes & Keys", date = "2026-07-24")
        )
    }

    val selectedIndices = remember { mutableStateListOf<Int>() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Files & Videos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Choose items to import into Private Vault",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    TextButton(
                        onClick = {
                            if (selectedIndices.size == sampleAvailableFiles.size) {
                                selectedIndices.clear()
                            } else {
                                selectedIndices.clear()
                                selectedIndices.addAll(sampleAvailableFiles.indices)
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedIndices.size == sampleAvailableFiles.size) "Clear" else "Select All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleAvailableFiles.size) { index ->
                        val item = sampleAvailableFiles[index]
                        val isChecked = selectedIndices.contains(index)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChecked) Color(0xFFE0F2FE) else Color(0xFFF8FAFC))
                                .border(
                                    1.dp,
                                    if (isChecked) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isChecked) selectedIndices.remove(index)
                                    else selectedIndices.add(index)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIndices.add(index)
                                    else selectedIndices.remove(index)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.type} • ${item.size}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        enabled = selectedIndices.isNotEmpty(),
                        onClick = {
                            val items = selectedIndices.map { sampleAvailableFiles[it] }
                            onConfirm(items)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Confirm (${selectedIndices.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImportingToVaultDialog(itemCount: Int) {
    androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DotLottieAnimation(
                    source = DotLottieSource.Url("https://lottie.host/8de591c2-9e48-4634-b9a3-793c7ab3d0f2/7MenLSHWm0.lottie"),
                    autoplay = true,
                    loop = true,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Encrypting & Importing $itemCount File(s)...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Securing into local AES-256 encrypted Private Vault",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVaultFolderDialog(
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    val types = listOf("Photos", "Videos", "Audio", "Documents", "Notes & Keys")
    var selectedType by remember { mutableStateOf("Photos") }
    val isDark = viewModel.isDarkTheme

    val previewImageUrl = when (selectedType) {
        "Photos" -> viewModel.folderImagePhotos
        "Videos" -> viewModel.folderImageVideos
        "Audio" -> viewModel.folderImageAudio
        "Documents" -> viewModel.folderImageDocuments
        else -> viewModel.folderImageNotes
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create New Private Folder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = previewImageUrl),
                    contentDescription = "Folder Preview Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") },
                placeholder = { Text("e.g. My Secret Vault") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select Folder Type:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(types) { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                text = type,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            labelColor = if (isDark) Color.White else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    enabled = folderName.isNotBlank(),
                    onClick = {
                        viewModel.createNewVaultFolder(folderName.trim(), selectedType)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create Folder", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VaultFolderDetailView(
    folder: CloudihubViewModel.VaultFolder,
    viewModel: CloudihubViewModel
) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkTheme
    val textCol = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val descCol = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentCol = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    var searchQuery by remember { mutableStateOf("") }
    var showSelectFilesModal by remember { mutableStateOf(false) }
    var isImportingFiles by remember { mutableStateOf(false) }
    var pendingSelectedFiles by remember { mutableStateOf<List<CloudihubViewModel.VaultItem>>(emptyList()) }
    var isFolderLoading by remember { mutableStateOf(true) }

    val folderItems = viewModel.vaultItems.filter {
        (it.folderId == folder.id || it.type == folder.type) &&
                (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
    }

    LaunchedEffect(folder.id) {
        isFolderLoading = true
        delay(1200)
        isFolderLoading = false
    }

    if (isFolderLoading) {
        VaultLottieLoadingView(
            title = "Loading ${folder.name}...",
            subtitle = "Decrypting files in folder...",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.selectedVaultFolder = null },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textCol)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Image(
                    painter = rememberAsyncImagePainter(model = folder.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textCol,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folderItems.size} encrypted ${folder.type.lowercase()} item(s)",
                        fontSize = 12.sp,
                        color = descCol
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search inside ${folder.name}...", fontSize = 13.sp, color = descCol) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = descCol) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = descCol)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = accentCol,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = textCol,
                    unfocusedTextColor = textCol
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (folderItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = descCol.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No files in this folder yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = descCol
                        )
                        Text(
                            text = "Tap the + button below to import files or videos",
                            fontSize = 12.sp,
                            color = descCol.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(folderItems) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accentCol.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            "Photos" -> Icons.Default.Image
                                            "Videos" -> Icons.Default.PlayCircle
                                            "Audio" -> Icons.Default.Audiotrack
                                            "Documents" -> Icons.Default.Description
                                            else -> Icons.Default.Key
                                        },
                                        contentDescription = null,
                                        tint = accentCol,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.size} • Encrypted AES-256 • ${item.date}",
                                        fontSize = 11.sp,
                                        color = descCol
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Opening decrypted preview for ${item.title}", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Decrypted",
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showSelectFilesModal = true },
            icon = { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White) },
            text = { Text("Add ${folder.type}", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = accentCol,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 8.dp)
        )

        if (showSelectFilesModal) {
            SelectVaultFilesDialog(
                onDismiss = { showSelectFilesModal = false },
                onConfirm = { items ->
                    showSelectFilesModal = false
                    pendingSelectedFiles = items.map { it.copy(folderId = folder.id, type = folder.type) }
                    isImportingFiles = true
                }
            )
        }

        if (isImportingFiles) {
            ImportingToVaultDialog(itemCount = pendingSelectedFiles.size)

            LaunchedEffect(isImportingFiles) {
                delay(2800)
                pendingSelectedFiles.forEach { item ->
                    viewModel.addVaultItemToFolder(item)
                }
                isImportingFiles = false
                Toast.makeText(
                    context,
                    "${pendingSelectedFiles.size} Item(s) successfully imported into ${folder.name}!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
fun PrivateVaultScreen(viewModel: CloudihubViewModel) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkTheme
    val bgGradient = if (isDark) {
        listOf(Color(0xFF0B1329), Color(0xFF1C2541))
    } else {
        listOf(Color(0xFFF0F6FF), Color(0xFFD6E4FF))
    }
    val textCol = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val descCol = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentCol = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    var showMenu by remember { mutableStateOf(false) }
    var isVaultScanning by remember { mutableStateOf(true) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVaultScanning = true
        delay(1800)
        isVaultScanning = false
    }

    if (viewModel.selectedVaultFolder != null) {
        VaultFolderDetailView(
            folder = viewModel.selectedVaultFolder!!,
            viewModel = viewModel
        )
        return
    }

    if (isVaultScanning) {
        VaultLottieLoadingView(
            title = "Searching & Decrypting Vault Folders...",
            subtitle = "Scanning local encrypted vault database...",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(bgGradient)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.activeProfilePage = "main" },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = textCol
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Private Vault",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textCol
                        )
                        Text(
                            text = "End-to-end encrypted storage",
                            fontSize = 12.sp,
                            color = descCol
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = textCol
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(cardBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rescan Vault Files", color = textCol, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Refresh, null, tint = accentCol) },
                            onClick = {
                                showMenu = false
                                isVaultScanning = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Unhide Vault", color = textCol, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Visibility, null, tint = accentCol) },
                            onClick = {
                                showMenu = false
                                viewModel.updateVaultCardHidden(false)
                                viewModel.activeProfilePage = "main"
                                Toast.makeText(context, "Vault card is now visible on the Profile dashboard!", Toast.LENGTH_LONG).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Lock Type", color = textCol) },
                            leadingIcon = { Icon(Icons.Default.Settings, null, tint = descCol) },
                            onClick = {
                                showMenu = false
                                viewModel.showPrivateVaultPasswordTypeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Vault Help & FAQ", color = textCol) },
                            leadingIcon = { Icon(Icons.Default.Info, null, tint = descCol) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "All files are stored locally and encrypted.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lock & Exit", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                viewModel.activeProfilePage = "main"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Folders Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Existing Folders
                items(viewModel.vaultFolders) { folder ->
                    val itemCount = viewModel.vaultItems.count { it.folderId == folder.id || it.type == folder.type }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable {
                                viewModel.selectedVaultFolder = folder
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = folder.imageUrl),
                                    contentDescription = folder.name,
                                    modifier = Modifier.size(90.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = folder.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$itemCount item(s)",
                                    fontSize = 11.sp,
                                    color = descCol
                                )
                            }
                        }
                    }
                }

                // More / Create New Folder Card using custom vector icon
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFE0F2FE).copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.5.dp, accentCol.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable {
                                showCreateFolderDialog = true
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Designed Folder Creation Icon Badge
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(accentCol.copy(alpha = 0.12f))
                                        .border(1.5.dp, accentCol.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(accentCol, Color(0xFF0284C7))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreateNewFolder,
                                            contentDescription = "Create New Folder",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Create Folder",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to add new",
                                    fontSize = 11.sp,
                                    color = descCol
                                )
                            }
                        }
                    }
                }
            }
        }

        // Create Folder Dialog
        if (showCreateFolderDialog) {
            CreateVaultFolderDialog(
                viewModel = viewModel,
                onDismiss = { showCreateFolderDialog = false }
            )
        }
    }
}

@Composable
fun WatchLaterScreen(viewModel: CloudihubViewModel) {
    val watchLaterList = viewModel.watchLaterVideos
    val isDark = viewModel.isDarkTheme

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.activeProfilePage = "" },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Watch Later",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9100).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${watchLaterList.size} Saved",
                    color = Color(0xFFFF9100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (watchLaterList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter("https://i.postimg.cc/G2tMPzZm/Fast-Delivery-icon-concept-in-black-duo-line-color.jpg"),
                            contentDescription = "Watch Later Empty",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Watch Later Videos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the Watch Later icon on any video thumbnail on the Home screen to save videos here for later viewing.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(watchLaterList, key = { it.id }) { video ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.playVideo(video) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 100.dp, height = 62.dp)
                                    .clip(RoundedCornerShape(10.dp))
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
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(video.duration, color = Color.White, fontSize = 9.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = video.creator,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleWatchLater(video) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFEF4444),
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

// --- SUB-SCREEN: AI ASSISTANT CONTROL & PERMISSIONS ---
@Composable
fun AiSettingsScreen(viewModel: CloudihubViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F9FF))
            .statusBarsPadding()
    ) {
        // Header Bar - Seamless background attached with page
        Surface(
            color = Color.Transparent,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = { viewModel.activeProfilePage = "main" },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Copilot & Control Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Permissions & Smart Automation",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF6366F1))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (viewModel.isAiAssistantEnabled) "ONLINE" else "OFFLINE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card with Cloud Theme Banner & Description
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val lottieComp by rememberLottieComposition(LottieCompositionSpec.Url(LOTTIE_OVERLAY_1_URL))
                            LottieAnimation(
                                composition = lottieComp,
                                iterations = LottieConstants.IterateForever,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI COPILOT CONTROL CENTER",
                                color = Color(0xFF0284C7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Smart Automation Engine",
                                color = Color(0xFF0F172A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0F2FE))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("CLOUD THEME", color = Color(0xFF0369A1), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cloud Theme Description Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE0F2FE), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "• Dynamic Animated Mascot • Smart Site Domain Resolver • Auto Video Downloader",
                                color = Color(0xFF0284C7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ask AI to visit sites by name (e.g. 'open daraz', 'facebook', 'search github') without full URLs!",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // MASTER SWITCH CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Assistant Power",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Enable floating mascot & AI copilot across all screens",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Switch(
                        checked = viewModel.isAiAssistantEnabled,
                        onCheckedChange = { viewModel.isAiAssistantEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFDC2626)
                        )
                    )
                }
            }

            // CONDITIONAL SUB-TOGGLES (Visible only when Master AI Assistant Switch is ON)
            if (viewModel.isAiAssistantEnabled) {
                Text(
                    text = "OPTIONAL FEATURE SETTINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        // Feature 1: Music Visualizer Effect (Defaulted to OFF)
                        AiToggleItem(
                            icon = Icons.Default.GraphicEq,
                            title = "Music Wave Effect",
                            description = "Play audio wave animation when streaming music or video. [Default OFF - Enable manually]",
                            checked = viewModel.isAiMusicEffectEnabled,
                            onCheckedChange = { viewModel.isAiMusicEffectEnabled = it },
                            iconColor = Color(0xFFEC4899)
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        // Feature 2: Thinking Mode Effect
                        AiToggleItem(
                            icon = Icons.Default.Psychology,
                            title = "Thinking Mode Effect",
                            description = "Display animated visual state when AI is processing responses.",
                            checked = viewModel.isAiThinkingModeEnabled,
                            onCheckedChange = { viewModel.isAiThinkingModeEnabled = it },
                            iconColor = Color(0xFF8B5CF6)
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        // Feature 3: Smart Web Redirect & Domain Navigation
                        AiToggleItem(
                            icon = Icons.Default.Language,
                            title = "Smart Web Domain Redirect",
                            description = "Auto-detect site names (e.g. Daraz, Facebook, Google) and redirect or search via Cloudihub Browser.",
                            checked = viewModel.isAiSiteNavigationEnabled,
                            onCheckedChange = { viewModel.isAiSiteNavigationEnabled = it },
                            iconColor = Color(0xFF6366F1)
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        // Feature 4: AI Auto-Downloader Assistant
                        AiToggleItem(
                            icon = Icons.Default.Download,
                            title = "Auto Video Downloader Assistant",
                            description = "Provide clean video preview cards with resolution options (1080p, 720p, 480p, MP3) in AI chat.",
                            checked = viewModel.isAiAutoDownloaderEnabled,
                            onCheckedChange = { viewModel.isAiAutoDownloaderEnabled = it },
                            iconColor = Color(0xFF22C55E)
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        // Feature 5: Domain Intent Verification
                        AiToggleItem(
                            icon = Icons.Default.VerifiedUser,
                            title = "Domain Intent Verification",
                            description = "Verify intent if asking for TikTok video download with a YouTube link provided.",
                            checked = viewModel.isAiIntentVerificationEnabled,
                            onCheckedChange = { viewModel.isAiIntentVerificationEnabled = it },
                            iconColor = Color(0xFFF59E0B)
                        )
                    }
                }
            }

            // Quick Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.isAiAssistantEnabled = true
                        viewModel.isAiMusicEffectEnabled = true
                        viewModel.isAiLinkDetectionEnabled = true
                        viewModel.isAiSiteNavigationEnabled = true
                        viewModel.isAiAutoDownloaderEnabled = true
                        viewModel.isAiIntentVerificationEnabled = true
                        Toast.makeText(context, "All AI permissions reset to Defaults", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Text("Reset Defaults", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AiToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconColor
            )
        )
    }
}

@Composable
fun UserProfileMetricsRow(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (!viewModel.isGoogleSignedIn) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.clickable {
                    viewModel.showSignupScreen = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🔒 Level & Balance Locked",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Tiny Level Badge Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFF0F9FF),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Level: ${viewModel.userLevel}", Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "User Level",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = viewModel.userLevel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            // 2. Tiny Prime Badge Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.clickable {
                    viewModel.showPrimeBadgesDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Prime Level",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = viewModel.userPrimeLevel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            // 3. Tiny Balance Badge Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Balance: ${viewModel.userBalance}", Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Balance",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = viewModel.userBalance,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

data class BadgeCardInfo(
    val icon: String,
    val tag: String,
    val desc: String,
    val perks: List<String>
)

@Composable
fun ModernPrimeBadgesBottomSheet(
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val closeSheet = {
        isVisible = false
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(280)
            onDismiss()
        }
    }

    BackHandler(enabled = isVisible) {
        closeSheet()
    }

    var selectedBadgeId by remember { 
        mutableStateOf(
            viewModel.availablePrimeBadges.find { it.levelName == viewModel.userPrimeLevel }?.id 
                ?: viewModel.availablePrimeBadges.firstOrNull()?.id 
                ?: "b3"
        ) 
    }

    var selectedFilterCategory by remember { mutableStateOf("All Badges") }

    val filteredBadges = remember(selectedFilterCategory) {
        when (selectedFilterCategory) {
            "Popular" -> viewModel.availablePrimeBadges.filter { it.id in listOf("b1", "b2", "b3") }
            "VIP Tiers" -> viewModel.availablePrimeBadges.filter { it.id in listOf("b3", "b4", "b5") }
            else -> viewModel.availablePrimeBadges
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Scrim backdrop with fade animation
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { closeSheet() }
            )
        }

        // Sheet content with slide-up animation
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(250))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false) {}, // Prevent closing when tapping inside sheet
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 90.dp)
                ) {
                    // Top Drag Indicator & Header Row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag Bar (Pull down or tap to dismiss sheet)
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .width(56.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCBD5E1))
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount > 6f) {
                                            closeSheet()
                                        }
                                    }
                                }
                                .clickable { closeSheet() }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Prime Badge",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Prime Badges Store",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Select & highlight cards to activate perks",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dynamic Filter Category Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All Badges", "Popular", "VIP Tiers").forEach { cat ->
                                val isCatSelected = (selectedFilterCategory == cat)
                                FilterChip(
                                    selected = isCatSelected,
                                    onClick = { selectedFilterCategory = cat },
                                    label = {
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCatSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0284C7),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = Color(0xFF475569)
                                    ),
                                    border = null
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scrollable Cards List with Dynamic Highlighting
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = 0,
                        pageCount = { filteredBadges.size }
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp
                    ) { pageIndex ->
                        val badge = filteredBadges.getOrNull(pageIndex) ?: return@HorizontalPager
                        val isCardSelected = (selectedBadgeId == badge.id)
                        val isCardActive = (viewModel.userPrimeLevel == badge.levelName)

                        // Animated scale & border highlight
                        val scale by animateFloatAsState(
                            targetValue = if (isCardSelected) 1.02f else 0.97f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "cardScale"
                        )

                        val cardBorderColor by animateColorAsState(
                            targetValue = when {
                                isCardActive -> Color(0xFF10B981)
                                isCardSelected -> Color(0xFF0284C7)
                                else -> Color(0xFFE2E8F0)
                            },
                            label = "cardBorderColor"
                        )

                        val cardContainerColor by animateColorAsState(
                            targetValue = when {
                                isCardActive -> Color(0xFFECFDF5)
                                isCardSelected -> Color(0xFFF0F9FF)
                                else -> Color(0xFFFAFAFC)
                            },
                            label = "cardBgColor"
                        )

                        val cardInfo = when (badge.iconType) {
                            "Shield" -> BadgeCardInfo("🛡️", "STARTER PASS", "Essential cloud tools for fast browsing & file access.", listOf("Instant Cloud Sync", "1.2x Speed Download", "Ad-Lite Search", "Encrypted Vault"))
                            "Star" -> BadgeCardInfo("⭐", "MOST POPULAR", "Enhanced speed and storage for active power users.", listOf("1.5x Accelerated Speed", "20 GB Cloud Vault", "Auto Ad & Popup Blocker", "Silver Badge Ring"))
                            "Crown" -> BadgeCardInfo("👑", "BEST VALUE", "Ultimate cloud experience with turbo speed & VIP perks.", listOf("2x Turbo Speed Downloads", "Unlimited Encrypted Vault", "Golden Glowing Badge", "AI Summarizer"))
                            "Diamond" -> BadgeCardInfo("💎", "ULTIMATE VIP", "Unrestricted diamond power with ultra bandwidth.", listOf("4x Ultra Speed Pipeline", "Unlimited AI Copilot", "Diamond Glowing Avatar", "Zero Ads Guarantee"))
                            else -> BadgeCardInfo("🏆", "SUPREME CROWN", "Highest tier lifetime privilege pass with master badge.", listOf("Maximum Server Bandwidth", "Lifetime VIP All-Access", "Exclusive Crown Master", "24/7 Priority Support"))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Light Glow Aura Effect behind card (Bronze, Purple, Emerald, Azure, Inferno glowing lights)
                            val glowAlphaCenter = if (isCardSelected || isCardActive) 0.70f else 0.30f
                            val glowAlphaEdge = if (isCardSelected || isCardActive) 0.35f else 0.12f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.96f)
                                    .height(290.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(badge.glowColorHex).copy(alpha = glowAlphaCenter),
                                                Color(badge.glowColorHex).copy(alpha = glowAlphaEdge),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(36.dp)
                                    )
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .border(
                                        width = if (isCardSelected || isCardActive) 2.5.dp else 1.dp,
                                        color = cardBorderColor,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        selectedBadgeId = badge.id
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isCardSelected) 8.dp else 2.dp)
                            ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = if (isCardSelected) Color(0xFF38BDF8).copy(alpha = 0.18f) else Color(0xFFCBD5E1).copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 20.dp, y = (-12).dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Tag Banner & Price Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = when {
                                                isCardActive -> Color(0xFF10B981)
                                                isCardSelected -> Color(0xFF0284C7)
                                                else -> Color(0xFFE2E8F0)
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                if (isCardActive || isCardSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = when {
                                                        isCardActive -> "ACTIVE PLAN ✓"
                                                        isCardSelected -> "SELECTED PLAN ★"
                                                        else -> cardInfo.tag
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isCardActive || isCardSelected) Color.White else Color(0xFF475569)
                                                )
                                            }
                                        }

                                        // Price Chip
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, Color(0xFF4ADE80))
                                        ) {
                                            Text(
                                                text = String.format(java.util.Locale.US, "$%.2f", badge.price),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Badge Image, Title & Level Name
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Color(badge.colorHex).copy(alpha = 0.15f))
                                                .border(1.5.dp, Color(badge.colorHex).copy(alpha = 0.6f), CircleShape)
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(badge.imageUrl),
                                                contentDescription = badge.levelName,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = badge.badgeTitle,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = badge.levelName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(badge.colorHex)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = cardInfo.desc,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B),
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0xFFE2E8F0))
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "HIGHLIGHTED PERKS & PRIVILEGES:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCardSelected) Color(0xFF0284C7) else Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        cardInfo.perks.forEach { perk ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Check",
                                                    tint = if (isCardSelected) Color(0xFF0284C7) else Color(0xFF10B981),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = perk,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF1E293B),
                                                    fontWeight = if (isCardSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Indicator Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(filteredBadges.size) { idx ->
                            val isCurrentPage = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(6.dp)
                                    .width(if (isCurrentPage) 22.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrentPage) Color(0xFF0284C7) else Color(0xFFCBD5E1))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Section
                    val currentActiveBadge = filteredBadges.getOrNull(pagerState.currentPage)
                        ?: viewModel.availablePrimeBadges.find { it.id == selectedBadgeId }
                        ?: viewModel.availablePrimeBadges.first()

                    val isPageBadgeOwned = viewModel.unlockedBadges.any { it.id == currentActiveBadge.id }
                    val isPageBadgeActive = viewModel.userPrimeLevel == currentActiveBadge.levelName

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPageBadgeActive) {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color(0xFFECFDF5),
                                    disabledContentColor = Color(0xFF10B981)
                                )
                            ) {
                                Text("Active Badge Plan ✓", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else if (isPageBadgeOwned) {
                            Button(
                                onClick = {
                                    viewModel.userPrimeLevel = currentActiveBadge.levelName
                                    Toast.makeText(context, "Switched to ${currentActiveBadge.badgeTitle}!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Activate ${currentActiveBadge.badgeTitle}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            val canAffordCredit = viewModel.userBalanceAmount >= currentActiveBadge.price

                            // Unlock with Credit
                            Button(
                                onClick = {
                                    if (viewModel.buyPrimeBadge(currentActiveBadge)) {
                                        Toast.makeText(context, "Unlocked ${currentActiveBadge.badgeTitle} with Credit!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Insufficient credit balance! Top up or invite friends.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = canAffordCredit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    disabledContainerColor = Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Credit",
                                        tint = if (canAffordCredit) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (canAffordCredit) "Unlock with Credit ($${String.format(java.util.Locale.US, "%.2f", currentActiveBadge.price)})" else "Low Credit Balance ($${String.format(java.util.Locale.US, "%.2f", viewModel.userBalanceAmount)})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAffordCredit) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Pay Direct
                            OutlinedButton(
                                onClick = {
                                    if (!isPageBadgeOwned) {
                                        viewModel.unlockedBadges.add(currentActiveBadge)
                                        viewModel.userPrimeLevel = currentActiveBadge.levelName
                                        Toast.makeText(context, "Payment Successful! ${currentActiveBadge.badgeTitle} Unlocked!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF0284C7)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF0284C7)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Pay",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "Pay $%.2f to Unlock", currentActiveBadge.price),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0284C7)
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

