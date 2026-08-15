package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudSkyBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Preset avatar options for instant custom selection
val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300",
    "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=300",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
    "https://images.unsplash.com/photo-1628157582853-a796fa650a6a?w=300",
    "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=300"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: CloudihubViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = viewModel.isDarkTheme
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Editable form state initialized from ViewModel
    var fullName by remember { mutableStateOf(viewModel.userProfileFullName) }
    var username by remember { mutableStateOf(viewModel.userProfileUsername.removePrefix("@")) }
    var email by remember { mutableStateOf(viewModel.userProfileEmail) }
    var phone by remember { mutableStateOf(viewModel.userProfilePhone) }
    var bio by remember { mutableStateOf(viewModel.userProfileBio) }
    var avatarUrl by remember { mutableStateOf(viewModel.userProfileAvatar) }

    var showAvatarPickerModal by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Staggered Animation Sequence Controller
    var isVisible0 by remember { mutableStateOf(false) } // Header & Avatar
    var isVisible1 by remember { mutableStateOf(false) } // Username & Name Box
    var isVisible2 by remember { mutableStateOf(false) } // Email & Phone Box
    var isVisible3 by remember { mutableStateOf(false) } // Badge & Rank Box
    var isVisible4 by remember { mutableStateOf(false) } // Storage Box
    var isVisible5 by remember { mutableStateOf(false) } // Downloads Box
    var isVisible6 by remember { mutableStateOf(false) } // Save Button Bar

    LaunchedEffect(Unit) {
        isVisible0 = true
        delay(70)
        isVisible1 = true
        delay(70)
        isVisible2 = true
        delay(70)
        isVisible3 = true
        delay(70)
        isVisible4 = true
        delay(70)
        isVisible5 = true
        delay(70)
        isVisible6 = true
    }

    val activeBadge = viewModel.currentActiveBadge
    val badgeColor = Color(activeBadge.colorHex)
    val storageInfo = viewModel.getDeviceStorageInfo()
    val activeDownloads by viewModel.downloads.collectAsState()
    val completedDownloadsCount = viewModel.downloadItems.size
    val activeDownloadingCount = activeDownloads.count { 
        it.status == com.example.ui.DownloadStatus.DOWNLOADING || it.status == com.example.ui.DownloadStatus.QUEUED 
    }

    // Avatar Glow Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlowTransition")
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "avatarGlowRotation"
    )

    val backgroundColor = if (isDark) Color(0xFF0B1120) else Color(0xFFF8FAFC)
    val cardBackground = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        CloudSkyBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // --- TOP NAVIGATION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f))
                        .border(1.dp, borderColor, CircleShape)
                        .testTag("edit_profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Cloud Identity & Stats",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }

                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.saveUserProfile(
                            name = fullName,
                            username = username,
                            email = email,
                            phone = phone,
                            bio = bio,
                            avatar = avatarUrl
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.12f))
                        .testTag("top_save_profile_button")
                ) {
                    Text(
                        text = "Save",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            // --- SCROLLABLE MAIN CONTENT ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {

                // ==========================================
                // 1. TOP USER PROFILE LOGO & BADGE SECTION
                // ==========================================
                AnimatedVisibility(
                    visible = isVisible0,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400, easing = EaseOutBack)) { -40 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with glowing rotating ring & edit camera badge
                        Box(
                            modifier = Modifier
                                .size(118.dp)
                                .clickable { showAvatarPickerModal = true },
                            contentAlignment = Alignment.Center
                        ) {
                            // Rotating sweep gradient halo
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(glowRotation)
                            ) {
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            badgeColor.copy(alpha = 0.2f),
                                            badgeColor,
                                            Color(0xFF0284C7),
                                            badgeColor.copy(alpha = 0.2f)
                                        )
                                    ),
                                    radius = size.minDimension / 2f,
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                            }

                            // Main Avatar Image
                            Image(
                                painter = rememberAsyncImagePainter(avatarUrl),
                                contentDescription = "Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, Color.White, CircleShape)
                            )

                            // Camera / Edit Badge Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7))
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable { showAvatarPickerModal = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Active Prime Badge Tag on Top Left
                            if (activeBadge.imageUrl.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.TopStart)
                                        .offset(x = (-4).dp, y = (-4).dp)
                                        .shadow(3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.5.dp, badgeColor, CircleShape)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(activeBadge.imageUrl),
                                        contentDescription = activeBadge.levelName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // User Full Name Display
                        Text(
                            text = fullName.ifEmpty { "Alex Skyward" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Username Handle with copyable affordance
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.10f))
                                .clickable {
                                    val fullHandle = if (username.startsWith("@")) username else "@$username"
                                    clipboardManager.setText(AnnotatedString(fullHandle))
                                    Toast.makeText(context, "Copied $fullHandle", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (username.startsWith("@")) username else "@$username",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Active Badge & Level Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✨ ${activeBadge.levelName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = badgeColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = viewModel.userLevel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 2. BASIC INFO BOX (Username, Full Name, Bio)
                // ==========================================
                AnimatedVisibility(
                    visible = isVisible1,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = EaseOutQuart)) { 60 }
                ) {
                    InfoCardContainer(
                        title = "Basic Information",
                        subtitle = "Your public identity on CloudiHub",
                        icon = Icons.Default.Person,
                        cardBackground = cardBackground,
                        borderColor = borderColor,
                        isDark = isDark
                    ) {
                        // Full Name Input
                        CustomFormField(
                            label = "Full Name",
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = "Enter your full name",
                            leadingIcon = Icons.Default.Badge,
                            isDark = isDark,
                            testTag = "input_full_name"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Username Input
                        CustomFormField(
                            label = "Username",
                            value = username,
                            onValueChange = { 
                                username = it.replace(" ", "_").replace("@", "") 
                            },
                            placeholder = "username",
                            prefixText = "@",
                            leadingIcon = Icons.Default.AlternateEmail,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("@$username"))
                                        Toast.makeText(context, "Username copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            isDark = isDark,
                            testTag = "input_username"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bio Input
                        CustomFormField(
                            label = "Bio & Tagline",
                            value = bio,
                            onValueChange = { bio = it },
                            placeholder = "Tell the cloud about yourself...",
                            leadingIcon = Icons.Default.EditNote,
                            singleLine = false,
                            maxLines = 3,
                            isDark = isDark,
                            testTag = "input_bio"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 3. CONTACT & SECURITY BOX (Email, Phone)
                // ==========================================
                AnimatedVisibility(
                    visible = isVisible2,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = EaseOutQuart)) { 60 }
                ) {
                    InfoCardContainer(
                        title = "Contact & Authentication",
                        subtitle = "Verified recovery and communication channels",
                        icon = Icons.Default.Security,
                        cardBackground = cardBackground,
                        borderColor = borderColor,
                        isDark = isDark
                    ) {
                        // Email Field
                        CustomFormField(
                            label = "Email Address",
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "user@cloudihub.io",
                            leadingIcon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email,
                            trailingBadge = "Verified",
                            trailingBadgeColor = Color(0xFF10B981),
                            isDark = isDark,
                            testTag = "input_email"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone Number Field
                        CustomFormField(
                            label = "Phone Number",
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = "+880 1712-345678",
                            leadingIcon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone,
                            trailingBadge = "Linked",
                            trailingBadgeColor = Color(0xFF0284C7),
                            isDark = isDark,
                            testTag = "input_phone"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 4. BADGE & RANK BOX (Prime Level & Perks)
                // ==========================================
                AnimatedVisibility(
                    visible = isVisible3,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = EaseOutQuart)) { 60 }
                ) {
                    InfoCardContainer(
                        title = "Prime Badge & Rank",
                        subtitle = "Active subscription perks and level achievements",
                        icon = Icons.Default.EmojiEvents,
                        cardBackground = cardBackground,
                        borderColor = borderColor,
                        isDark = isDark
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(badgeColor.copy(alpha = 0.08f))
                                .border(1.dp, badgeColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, badgeColor, CircleShape)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(activeBadge.imageUrl),
                                    contentDescription = activeBadge.levelName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeBadge.badgeTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Rank: ${viewModel.userLevel} • Balance: ${viewModel.userBalance}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✨ ${activeBadge.benefits.take(2).joinToString(" • ")}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button to open prime badges dialog
                        OutlinedButton(
                            onClick = { viewModel.showPrimeBadgesDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF0284C7)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Badges",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View All Prime Badges & Ranks",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 5. STORAGE AVAILABLE BOX (Meter & Quota)
                // ==========================================
                AnimatedVisibility(
                    visible = isVisible4,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = EaseOutQuart)) { 60 }
                ) {
                    val availableGB = try {
                        val tot = storageInfo.totalGB.toDoubleOrNull() ?: 64.0
                        val used = storageInfo.usedGB.toDoubleOrNull() ?: 38.5
                        String.format("%.1f", (tot - used).coerceAtLeast(0.0))
                    } catch (e: Exception) {
                        "25.5"
                    }

                    InfoCardContainer(
                        title = "Storage Available",
                        subtitle = "Cloud and high-speed device storage quota",
                        icon = Icons.Default.CloudQueue,
                        cardBackground = cardBackground,
                        borderColor = borderColor,
                        isDark = isDark
                    ) {
                        // Storage highlighted header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "$availableGB GB Free",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "${storageInfo.usedGB} GB used of ${storageInfo.totalGB} GB total",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${100 - storageInfo.percentUsed}% Available",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Gauge Bar
                        val animatedProgress by animateFloatAsState(
                            targetValue = storageInfo.percentUsed / 100f,
                            animationSpec = tween(1000, easing = FastOutSlowInEasing),
                            label = "storageProgress"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF0284C7), Color(0xFF10B981))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Storage Breakdown Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StorageChip(label = "Videos", size = "1.8 GB", color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                            StorageChip(label = "Music", size = "320 MB", color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                            StorageChip(label = "Vault", size = "12 KB", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 6. DOWNLOADS STATS BOX
                // ==========================================
                AnimatedVisibility(
                    visible = isVisible5,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = EaseOutQuart)) { 60 }
                ) {
                    InfoCardContainer(
                        title = "Downloads & Media",
                        subtitle = "Saved video streams and offline audio cache",
                        icon = Icons.Default.FileDownload,
                        cardBackground = cardBackground,
                        borderColor = borderColor,
                        isDark = isDark
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBox(
                                title = "Saved Files",
                                value = "$completedDownloadsCount Files",
                                subtitle = "Ready offline",
                                icon = Icons.Default.Folder,
                                tint = Color(0xFF0284C7),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Active Tasks",
                                value = if (activeDownloadingCount > 0) "$activeDownloadingCount Active" else "Idle",
                                subtitle = "Download queue",
                                icon = Icons.Default.CloudDownload,
                                tint = if (activeDownloadingCount > 0) Color(0xFF10B981) else Color(0xFF94A3B8),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.showFullScreenDownloads = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Downloads",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Full Downloads Center",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // 7. BOTTOM SAVE BUTTON
                // ==========================================
                AnimatedVisibility(
                    visible = isVisible6,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = EaseOutQuart)) { 60 }
                ) {
                    Button(
                        onClick = {
                            isSaving = true
                            keyboardController?.hide()
                            coroutineScope.launch {
                                viewModel.saveUserProfile(
                                    name = fullName,
                                    username = username,
                                    email = email,
                                    phone = phone,
                                    bio = bio,
                                    avatar = avatarUrl
                                )
                                delay(300)
                                isSaving = false
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF0284C7).copy(alpha = 0.4f))
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                    ),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save Profile Changes",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // ==========================================
        // AVATAR PICKER MODAL DIALOG
        // ==========================================
        if (showAvatarPickerModal) {
            Dialog(onDismissRequest = { showAvatarPickerModal = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Avatar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Pick a preset avatar style or customize",
                            fontSize = 12.sp,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Presets Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(PRESET_AVATARS) { preset ->
                                val isSelected = avatarUrl == preset
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .border(
                                            if (isSelected) 3.dp else 1.dp,
                                            if (isSelected) Color(0xFF0284C7) else borderColor,
                                            CircleShape
                                        )
                                        .clickable {
                                            avatarUrl = preset
                                        }
                                        .padding(2.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(preset),
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom URL textfield
                        OutlinedTextField(
                            value = avatarUrl,
                            onValueChange = { avatarUrl = it },
                            label = { Text("Image URL") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = borderColor
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAvatarPickerModal = false }) {
                                Text("Done", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------
// HELPER COMPOSABLE CONTAINERS & REUSABLE FORM COMPONENTS
// -----------------------------------------------------------

@Composable
private fun InfoCardContainer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    cardBackground: Color,
    borderColor: Color,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.04f))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row of the Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun CustomFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    prefixText: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    trailingBadge: String? = null,
    trailingBadgeColor: Color = Color(0xFF10B981),
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    isDark: Boolean,
    testTag: String = ""
) {
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val inputBackground = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, start = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            )

            if (trailingBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(trailingBadgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = trailingBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = trailingBadgeColor
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.5.sp, color = Color(0xFF94A3B8)) },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(18.dp)
                )
            },
            prefix = if (prefixText != null) {
                { Text(prefixText, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7)) }
            } else null,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBackground,
                unfocusedContainerColor = inputBackground,
                focusedBorderColor = Color(0xFF0284C7),
                unfocusedBorderColor = borderColor,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary
            )
        )
    }
}

@Composable
private fun StorageChip(
    label: String,
    size: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = label, fontSize = 10.5.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text(text = size, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val border = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 11.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
            Text(text = subtitle, fontSize = 10.5.sp, color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))
        }
    }
}
