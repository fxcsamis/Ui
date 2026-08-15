@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.example

import android.os.Bundle
import android.content.Intent
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.components.LocalSharedTransitionScope
import com.example.ui.components.CloudeHubSplashScreen
import com.example.ui.components.FloatingAiLottieWidget
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudSkyBackground
import com.example.ui.components.DownloadsHub
import com.example.ui.components.GlassmorphicNavBar
import com.example.ui.components.NavigationTab
import com.example.ui.components.VoiceSearchDialog
import com.example.ui.components.VideoStreamingPlayer
import com.example.ui.components.MusicBubblePlayer
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.AiCopilotFullChatScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SitesScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size

class MainActivity : FragmentActivity() {
    
    private val viewModel: CloudihubViewModel by viewModels()

    fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Verification required: $errString", Toast.LENGTH_SHORT).show()
                    viewModel.showFingerprintAuth = false
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@MainActivity, "Authentication Succeeded", Toast.LENGTH_SHORT).show()
                    viewModel.showFingerprintAuth = false
                    if (viewModel.biometricAuthTarget == "private_vault") {
                        viewModel.activeProfilePage = "private_vault"
                    } else {
                        viewModel.activeProfilePage = "linked_devices"
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@MainActivity, "Biometric failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            })

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Identity")
            .setSubtitle("Authenticate to access linked devices securely")

        try {
            promptBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } catch (e: Exception) {
            promptBuilder.setNegativeButtonText("Cancel")
        }

        val promptInfo = promptBuilder.build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Security Error: Please setup phone password/PIN to secure your data.", Toast.LENGTH_LONG).show()
            viewModel.showFingerprintAuth = false
            viewModel.activeProfilePage = "main"
        }
    }

    private fun handleWidgetIntent(intent: Intent?) {
        val target = intent?.getStringExtra("WIDGET_TARGET") ?: return
        when (target) {
            "VOICE" -> {
                viewModel.startVoiceSearch()
            }
            "SEARCH", "HOME_SEARCH" -> {
                viewModel.selectTab(NavigationTab.Home)
            }
            "MUSIC_SEARCH" -> {
                viewModel.selectTab(NavigationTab.Music)
            }
            "MUSIC", "MUSIC_PLAY", "MUSIC_PREV", "MUSIC_NEXT" -> {
                viewModel.selectTab(NavigationTab.Music)
            }
            "DOWNLOADS" -> {
                viewModel.showFullScreenDownloads = true
            }
            "VIDEO" -> {
                viewModel.selectTab(NavigationTab.Home)
            }
            "AI" -> {
                viewModel.showAiFullChatScreen = true
            }
            "SITE", "HUB", "GAME" -> {
                viewModel.selectTab(NavigationTab.Hub)
            }
            "BROWSER" -> {
                viewModel.selectTab(NavigationTab.Browser)
            }
            "PROFILE" -> {
                viewModel.selectTab(NavigationTab.Profile)
            }
            "YOUTUBE" -> {
                viewModel.openUrl("https://m.youtube.com")
                viewModel.selectTab(NavigationTab.Browser)
            }
            "HOME" -> {
                viewModel.selectTab(NavigationTab.Home)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleWidgetIntent(intent)
        
        // Fullscreen edge-to-edge setup
        enableEdgeToEdge()
        
        setContent {
            var showSplashScreen by remember { mutableStateOf(true) }
            var showExitConfirmDialog by remember { mutableStateOf(false) }

            BackHandler {
                if (showSplashScreen) {
                    showSplashScreen = false
                } else if (showExitConfirmDialog) {
                    showExitConfirmDialog = false
                } else if (viewModel.showPrivateVaultUnlockDialog || viewModel.showPrivateVaultPasswordInputDialog || viewModel.showPrivateVaultPasswordTypeDialog) {
                    viewModel.showPrivateVaultUnlockDialog = false
                    viewModel.showPrivateVaultPasswordInputDialog = false
                    viewModel.showPrivateVaultPasswordTypeDialog = false
                } else if (viewModel.selectedVaultFolder != null) {
                    viewModel.selectedVaultFolder = null
                } else if (viewModel.showVoiceDialog) {
                    viewModel.stopVoiceSearch()
                } else if (viewModel.showFullScreenDownloads) {
                    viewModel.showFullScreenDownloads = false
                } else if (viewModel.showEditProfileScreen) {
                    viewModel.showEditProfileScreen = false
                } else if (viewModel.showSignupScreen) {
                    viewModel.showSignupScreen = false
                } else if (viewModel.showAiFullChatScreen) {
                    viewModel.showAiFullChatScreen = false
                } else if (viewModel.showShortsScreen) {
                    viewModel.closeShortsScreen()
                } else if (viewModel.isFullMusicPlayerOpen) {
                    viewModel.isFullMusicPlayerOpen = false
                } else if (viewModel.playingVideo != null && viewModel.isVideoPlayerExpanded) {
                    viewModel.isVideoPlayerExpanded = false
                } else if (viewModel.isPlaylistOverlayOpen) {
                    viewModel.isPlaylistOverlayOpen = false
                } else if (viewModel.activeProfilePage != "main") {
                    viewModel.activeProfilePage = "main"
                } else if (viewModel.activeTab == NavigationTab.Browser && viewModel.browserUrl.isNotEmpty()) {
                    viewModel.openUrl("")
                } else if (viewModel.activeTab != NavigationTab.Home) {
                    viewModel.selectTab(NavigationTab.Home)
                } else {
                    showExitConfirmDialog = true
                }
            }

            val isVaultActive = viewModel.activeProfilePage == "private_vault" && viewModel.activeTab == NavigationTab.Profile
            LaunchedEffect(isVaultActive) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                if (isVaultActive) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }

            LaunchedEffect(viewModel.showFingerprintAuth) {
                if (viewModel.showFingerprintAuth) {
                    showBiometricPrompt()
                }
            }

            MyApplicationTheme(darkTheme = viewModel.isDarkTheme, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    @OptIn(ExperimentalSharedTransitionApi::class)
                    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                            Box(modifier = Modifier.fillMaxSize()) {
                        // 0. Premium CloudeHub Animated Splash Screen Overlay
                        AnimatedVisibility(
                            visible = showSplashScreen,
                            enter = fadeIn(),
                            exit = fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 1.08f, animationSpec = tween(600)),
                            modifier = Modifier.zIndex(2000f)
                        ) {
                            CloudeHubSplashScreen(
                                onSplashFinished = {
                                    showSplashScreen = false
                                }
                            )
                        }

                        // 1. Organic drifting cloud background layers
                        CloudSkyBackground()

                        // 2. Active Screen content router
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (viewModel.showSignupScreen) {
                                SignupScreen(viewModel = viewModel)
                            } else if (viewModel.showAiFullChatScreen) {
                                AiCopilotFullChatScreen(viewModel = viewModel)
                            } else {
                                when (viewModel.activeTab) {
                                    NavigationTab.Profile -> ProfileScreen(viewModel = viewModel)
                                    NavigationTab.Home -> HomeScreen(viewModel = viewModel)
                                    NavigationTab.Music -> MusicScreen(viewModel = viewModel)
                                    NavigationTab.Hub -> HubScreen(viewModel = viewModel)
                                    NavigationTab.Browser -> BrowserScreen(viewModel = viewModel)
                                }
                            }
                        }

                        // 3. Floating glassmorphic navigation bar with scroll hide/show animation
                        val shouldHideNavBar = viewModel.showSignupScreen || 
                                viewModel.showEditProfileScreen ||
                                viewModel.showAiFullChatScreen || 
                                isVaultActive || 
                                (viewModel.activeTab == NavigationTab.Browser && (viewModel.isBrowserFullscreen || viewModel.browserUrl.isNotEmpty())) ||
                                viewModel.isPlaylistOverlayOpen ||
                                viewModel.isFullMusicPlayerOpen ||
                                !viewModel.isNavBarVisible

                        AnimatedVisibility(
                            visible = !shouldHideNavBar,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            GlassmorphicNavBar(
                                activeTab = viewModel.activeTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }

                        // 4. Edge-to-edge solid full screen Voice View
                        if (viewModel.showVoiceDialog) {
                            VoiceSearchDialog(
                                viewModel = viewModel,
                                modifier = Modifier.zIndex(600f)
                            )
                        }

                        // 5. Sliding downloads hub list panel
                        DownloadsHub(viewModel = viewModel)

                        // 6. Shared Element Spring Animated Video Streaming Player
                        val isHiddenScreen = (viewModel.activeTab == NavigationTab.Profile || viewModel.activeTab == NavigationTab.Browser)
                        val isPlayerVisible = viewModel.playingVideo != null && !viewModel.isFullMusicPlayerOpen && (viewModel.isVideoPlayerExpanded || !isHiddenScreen)

                        AnimatedVisibility(
                            visible = isPlayerVisible,
                            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.8f)) +
                                    scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = 350f, dampingRatio = 0.8f)) +
                                    fadeIn(animationSpec = spring(stiffness = 350f)),
                            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.8f)) +
                                   scaleOut(targetScale = 0.85f, animationSpec = spring(stiffness = 350f, dampingRatio = 0.8f)) +
                                   fadeOut(animationSpec = spring(stiffness = 350f))
                        ) {
                            VideoStreamingPlayer(
                                viewModel = viewModel,
                                animatedVisibilityScope = this@AnimatedVisibility
                            )
                        }

                        // 7. Fullscreen Shorts Screen
                        if (viewModel.showShortsScreen) {
                            ShortsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.zIndex(400f)
                            )
                        }

                        // 8. Fullscreen Downloads Screen (Hidden status bar, horizontal slide sections: Download, Videos, Music, Folders)
                        if (viewModel.showFullScreenDownloads) {
                            DownloadsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.zIndex(500f)
                            )
                        }

                        // Fullscreen Signup & Bind Screen (Hidden status bar like Downloads screen)
                        if (viewModel.showSignupScreen) {
                            SignupScreen(
                                viewModel = viewModel,
                                modifier = Modifier.zIndex(500f)
                            )
                        }

                        // Fullscreen Edit Profile Screen
                        if (viewModel.showEditProfileScreen) {
                            EditProfileScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.showEditProfileScreen = false },
                                modifier = Modifier.zIndex(500f)
                            )
                        }

                        // 7. Floating Music Bubble Player (Available across pages when playing music)
                        MusicBubblePlayer(
                            viewModel = viewModel,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 85.dp, end = 10.dp)
                        )

                        // 8. Global Floating AI Lottie Assistant Overlay (Available across all screens except Private Vault, Signup, Edit Profile, Full AI Chat, Active Website View & Full Music Player)
                        val isBrowsingActiveSite = (viewModel.activeTab == NavigationTab.Browser && viewModel.browserUrl.isNotEmpty())
                        if (!isVaultActive && !viewModel.showSignupScreen && !viewModel.showEditProfileScreen && !viewModel.showAiFullChatScreen && !isBrowsingActiveSite && !viewModel.isFullMusicPlayerOpen) {
                            FloatingAiLottieWidget(
                                viewModel = viewModel,
                                isMediaPlaying = (viewModel.activeStreamingUrl.isNotEmpty() || viewModel.isPlaying),
                                onSearchRequested = { query ->
                                    viewModel.updateSearchQuery(query)
                                    viewModel.selectTab(NavigationTab.Home)
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 90.dp, end = 0.dp)
                                    .zIndex(300f)
                            )
                        }

                        // 9. Exit Confirmation Popup Dialog
                        if (showExitConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showExitConfirmDialog = false },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "Exit App",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        text = "Exit Application?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Are you sure you want to quit CloudiHub?",
                                        fontSize = 14.sp,
                                        color = Color(0xFF64748B)
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showExitConfirmDialog = false
                                            finish()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4444)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Quit App", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(
                                        onClick = { showExitConfirmDialog = false },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Text("Cancel", color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                containerColor = Color.White,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.zIndex(1000f)
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
