package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudSkyBackground
import com.example.ui.components.DownloadsHub
import com.example.ui.components.GlassmorphicNavBar
import com.example.ui.components.NavigationTab
import com.example.ui.components.VoiceSearchDialog
import com.example.ui.components.VideoStreamingPlayer
import com.example.ui.components.MusicBubblePlayer
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SitesScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import android.widget.Toast

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen edge-to-edge setup
        enableEdgeToEdge()
        
        setContent {
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Wrap all background and screen content in a blurred Box when voice search is active
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(if (viewModel.showVoiceDialog) 12.dp else 0.dp)
                        ) {
                            // 1. Organic drifting cloud background layers
                            CloudSkyBackground()

                            // 2. Active Screen content router
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (viewModel.showSignupScreen) {
                                    SignupScreen(viewModel = viewModel)
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

                            // 3. Floating glassmorphic navigation bar
                            val shouldHideNavBar = viewModel.showSignupScreen || isVaultActive || (viewModel.activeTab == NavigationTab.Browser && 
                                    (viewModel.isBrowserFullscreen || viewModel.browserUrl.isNotEmpty()))
                            if (!shouldHideNavBar) {
                                GlassmorphicNavBar(
                                    activeTab = viewModel.activeTab,
                                    onTabSelected = { viewModel.selectTab(it) },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }

                        // 4. Floating speech-recognition voice dialog (Perfectly sharp and clean)
                        VoiceSearchDialog(viewModel = viewModel)

                        // 5. Sliding downloads hub list panel
                        DownloadsHub(viewModel = viewModel)

                        // 6. Full-screen Video Streaming Player
                        if (viewModel.playingVideo != null) {
                            VideoStreamingPlayer(viewModel = viewModel)
                        }

                        // 7. Floating Music Bubble Player (Available across pages when playing music)
                        MusicBubblePlayer(
                            viewModel = viewModel,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 85.dp, end = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
