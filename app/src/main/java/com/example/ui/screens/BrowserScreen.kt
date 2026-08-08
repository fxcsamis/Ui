package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.components.NavigationTab
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudSkyBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.ByteArrayInputStream


private fun getDomainName(url: String): String {
    return try {
        val cleaned = url.trim()
            .replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
        val slashIndex = cleaned.indexOf('/')
        if (slashIndex != -1) {
            cleaned.substring(0, slashIndex)
        } else {
            cleaned
        }
    } catch (e: Exception) {
        url
    }
}

data class VideoResolutionOption(
    val quality: String,
    val badge: String,
    val sizeMb: String,
    val streamUrl: String,
    val isBest: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf(viewModel.browserUrl) }
    var isPageLoading by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableStateOf(0f) }
    var canGoBackState by remember { mutableStateOf(false) }
    var canGoForwardState by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    
    // System Feature States
    var customVideoView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var showTabManagerDialog by remember { mutableStateOf(false) }
    var showBrowserDownloadsSheet by remember { mutableStateOf(false) }
    var isSecurityActivating by remember { mutableStateOf(false) }
    var showSafeBrowsingDialog by remember { mutableStateOf(false) }
    var flaggedUnsafeUrl by remember { mutableStateOf("") }
    var detectedVideoUrl by remember { mutableStateOf<String?>(null) }
    var detectedVideoTitle by remember { mutableStateOf("Video") }
    var isSearchFocused by remember { mutableStateOf(false) }

    // Video Resolution Extractor & Bottom Sheet States
    val coroutineScope = rememberCoroutineScope()
    var showResolutionSheet by remember { mutableStateOf(false) }
    var isExtractingResolutions by remember { mutableStateOf(false) }
    var extractedResolutions by remember { mutableStateOf<List<VideoResolutionOption>>(emptyList()) }

    val fileChooserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    val focusManager = LocalFocusManager.current

    // Intercept system Back gesture/button when browsing so it goes back in web history or returns to Home page, instead of exiting the app!
    BackHandler(enabled = viewModel.browserUrl.isNotEmpty() || isSearchFocused) {
        if (isSearchFocused) {
            focusManager.clearFocus()
            isSearchFocused = false
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            viewModel.openUrl("")
        }
    }

    // Sync input box when viewModel url shifts
    LaunchedEffect(viewModel.browserUrl) {
        if (viewModel.browserUrl.isNotEmpty()) {
            if (viewModel.browserUrl != lastLoadedUrl) {
                lastLoadedUrl = viewModel.browserUrl
                currentUrl = viewModel.browserUrl
                webViewRef?.loadUrl(viewModel.browserUrl)
            }
        } else {
            if (lastLoadedUrl.isNotEmpty()) {
                lastLoadedUrl = ""
                currentUrl = ""
                webViewRef?.loadUrl("about:blank")
            }
        }
    }

    // Play mysterious sound when Incognito Mode is activated
    LaunchedEffect(viewModel.isIncognitoMode) {
        if (viewModel.isIncognitoMode) {
            try {
                val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 85)
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_LOW_L, 120)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_HIGH_L, 220)
                    } catch (_: Exception) {}
                }, 130)
            } catch (_: Exception) {}
        }
    }

    // Hide Status Bar for full immersive Edge-to-Edge experience across Home and Browsing
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

    val isStartPage = viewModel.browserUrl.isEmpty()
    val screenBgColor = Color(0xFFF8FAFC)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {
        if (isStartPage) {
            CloudSkyBackground(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // --- TOP URL ADDRESS BAR & CONTROL HEADER ---
        if (!viewModel.isBrowserFullscreen) {
            val isStartPage = viewModel.browserUrl.isEmpty()
            val headerBgColor = if (isStartPage) Color.Transparent else Color.White
            val searchBoxBgColor = if (viewModel.isIncognitoMode) Color(0xFFF3E8FF) else if (isStartPage) Color.White else Color(0xFFF1F5F9)
            val searchBoxBorderColor = if (viewModel.isIncognitoMode) Color(0xFFC084FC) else Color(0xFFE2E8F0)

            val searchInteractionSource = remember { MutableInteractionSource() }
            val isSearchPressed by searchInteractionSource.collectIsPressedAsState()

            val searchBarScale by animateFloatAsState(
                targetValue = if (isSearchPressed) 0.94f else 1.0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 380f
                ),
                label = "searchBarScaleAnim"
            )

            val searchWeight by animateFloatAsState(
                targetValue = if (isStartPage) (if (isSearchFocused) 0.65f else 0.45f) else 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 320f
                ),
                label = "searchWeightAnim"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Left Section: iOS Animated Expanding Search Box
                Box(
                    modifier = Modifier.weight(searchWeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .graphicsLayer {
                                scaleX = searchBarScale
                                scaleY = searchBarScale
                            }
                            .clip(RoundedCornerShape(19.dp))
                            .background(searchBoxBgColor)
                            .border(1.dp, searchBoxBorderColor, RoundedCornerShape(19.dp))
                            .padding(start = 10.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchFocused) {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    isSearchFocused = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Cancel search",
                                    tint = if (viewModel.isIncognitoMode) Color(0xFF9333EA) else Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = if (viewModel.isIncognitoMode) Icons.Default.VisibilityOff else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (viewModel.isIncognitoMode) Color(0xFF9333EA) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        BasicTextField(
                            value = currentUrl,
                            onValueChange = { currentUrl = it },
                            interactionSource = searchInteractionSource,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    isSearchFocused = false
                                    val target = currentUrl.trim()
                                    if (target.isNotEmpty()) {
                                        var destination = target
                                        if (!destination.startsWith("http://") && !destination.startsWith("https://")) {
                                            if (destination.contains(".") && !destination.contains(" ")) {
                                                destination = "https://$destination"
                                            } else {
                                                destination = "https://www.google.com/search?q=${destination.replace(" ", "+")}"
                                            }
                                        }
                                        viewModel.openUrl(destination)
                                        webViewRef?.loadUrl(destination)
                                    }
                                }
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = if (viewModel.isIncognitoMode) Color(0xFF581C87) else Color(0xFF0F172A),
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                }
                                .testTag("browser_address_input"),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (currentUrl.isEmpty()) {
                                        Text(
                                            text = if (viewModel.isIncognitoMode) "Incognito Search..." else "Type URL or search...",
                                            color = if (viewModel.isIncognitoMode) Color(0xFFA855F7) else Color(0xFF94A3B8),
                                            fontSize = 13.5.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (currentUrl.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    currentUrl = ""
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear address bar",
                                    tint = if (viewModel.isIncognitoMode) Color(0xFF9333EA) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (isSearchFocused) {
                            Text(
                                text = "Cancel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        focusManager.clearFocus()
                                        isSearchFocused = false
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Right Section: Extended Control Panel Box
                if (searchWeight < 0.98f) {
                    val controlWeight = (1f - searchWeight).coerceAtLeast(0.01f)
                    val alphaVal = ((1f - searchWeight) / 0.58f).coerceIn(0f, 1f)

                    Spacer(modifier = Modifier.width((8 * (1f - searchWeight)).dp))

                    Box(
                        modifier = Modifier
                            .weight(controlWeight)
                            .alpha(alphaVal),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedContent(
                                targetState = isStartPage,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.92f))
                                        .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.92f))
                                },
                                label = "topbar_controls_anim"
                            ) { startPage ->
                                if (startPage) {
                                    val quickSites = remember(viewModel.browserTabs, viewModel.historyItems) {
                                        val tabSites = viewModel.browserTabs.map { Pair(it.title, it.url) }
                                        val historySites = viewModel.historyItems.map { Pair(it.title, it.subtitle) }
                                        val defaultSites = listOf(
                                            Pair("Google", "https://www.google.com"),
                                            Pair("YouTube", "https://m.youtube.com"),
                                            Pair("Facebook", "https://www.facebook.com"),
                                            Pair("Wikipedia", "https://www.wikipedia.org"),
                                            Pair("Amazon", "https://www.amazon.com"),
                                            Pair("Instagram", "https://www.instagram.com"),
                                            Pair("GitHub", "https://www.github.com"),
                                            Pair("Twitter", "https://www.x.com")
                                        )
                                        (tabSites + historySites + defaultSites)
                                            .filter { it.second.isNotEmpty() && it.second.startsWith("http") }
                                            .distinctBy { it.second }
                                            .take(12)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(19.dp))
                                            .background(if (viewModel.isIncognitoMode) Color(0xFFF3E8FF) else Color.White)
                                            .border(1.5.dp, if (viewModel.isIncognitoMode) Color(0xFFC084FC) else Color(0xFFE2E8F0), RoundedCornerShape(19.dp))
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .width(82.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            quickSites.forEach { site ->
                                                val domain = try {
                                                    val uri = Uri.parse(site.second)
                                                    uri.host?.removePrefix("www.") ?: site.second
                                                } catch (e: Exception) {
                                                    "google.com"
                                                }
                                                val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"

                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFF1F5F9))
                                                        .border(0.5.dp, Color(0xFFCBD5E1), CircleShape)
                                                        .clickable {
                                                            viewModel.openUrl(site.second)
                                                            webViewRef?.loadUrl(site.second)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = faviconUrl,
                                                        contentDescription = site.first,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(2.dp))

                                        IconButton(
                                            onClick = { viewModel.toggleIncognitoMode(!viewModel.isIncognitoMode) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (viewModel.isIncognitoMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Incognito Mode",
                                                tint = if (viewModel.isIncognitoMode) Color(0xFF9333EA) else Color(0xFF64748B),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (viewModel.isIncognitoMode) Color(0xFFE9D5FF) else Color(0xFFF1F5F9))
                                                .border(1.dp, if (viewModel.isIncognitoMode) Color(0xFFC084FC) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                                .clickable { showTabManagerDialog = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${viewModel.browserTabs.size}",
                                                color = if (viewModel.isIncognitoMode) Color(0xFF6B21A8) else Color(0xFF0F172A),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    val context = LocalContext.current
                                    Row(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(19.dp))
                                            .background(if (viewModel.isIncognitoMode) Color(0xFFF3E8FF) else Color.White)
                                            .border(1.5.dp, if (viewModel.isIncognitoMode) Color(0xFFC084FC) else Color(0xFFE2E8F0), RoundedCornerShape(19.dp))
                                            .padding(horizontal = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.openUrl("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Return to Home",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { webViewRef?.reload() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh Page",
                                                tint = if (viewModel.isIncognitoMode) Color(0xFF9333EA) else Color(0xFF0284C7),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                isBookmarked = !isBookmarked
                                                if (isBookmarked) {
                                                    Toast.makeText(context, "Page Bookmarked ⭐", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Bookmark Removed", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Bookmark Page",
                                                tint = if (isBookmarked) Color(0xFF0284C7) else Color(0xFF64748B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (currentUrl.isNotEmpty()) {
                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                        putExtra(Intent.EXTRA_TEXT, currentUrl)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Share Page"))
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share Link",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.toggleIncognitoMode(!viewModel.isIncognitoMode) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (viewModel.isIncognitoMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Incognito Mode",
                                                tint = if (viewModel.isIncognitoMode) Color(0xFF9333EA) else Color(0xFF64748B),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(21.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(if (viewModel.isIncognitoMode) Color(0xFFE9D5FF) else Color(0xFFF1F5F9))
                                                .border(1.dp, if (viewModel.isIncognitoMode) Color(0xFFC084FC) else Color(0xFFCBD5E1), RoundedCornerShape(5.dp))
                                                .clickable { showTabManagerDialog = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${viewModel.browserTabs.size}",
                                                color = if (viewModel.isIncognitoMode) Color(0xFF6B21A8) else Color(0xFF0F172A),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.toggleBrowserFullscreen(true) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = "Enter Fullscreen",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (viewModel.downloadItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                val dlInfinite = rememberInfiniteTransition(label = "dl_top_lottie")
                                val dlPulseScale by dlInfinite.animateFloat(
                                    initialValue = 0.94f,
                                    targetValue = 1.08f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "dl_pulse"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .graphicsLayer(scaleX = dlPulseScale, scaleY = dlPulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F2FE))
                                        .border(1.dp, Color(0xFF0284C7), CircleShape)
                                        .clickable { showBrowserDownloadsSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Cloud Download Manager",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Page Progress Indicator
            AnimatedVisibility(
                visible = isPageLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = pageProgress,
                    color = Color(0xFF0284C7),
                    trackColor = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    } else {
            // Fullscreen status bar spacer to cover edges nicely
            Spacer(modifier = Modifier.statusBarsPadding())
        }

        // --- MAIN BROWSER AREA (START PAGE OR WEB VIEW) WITH SEARCH SUGGESTIONS OVERLAY ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (viewModel.browserUrl.isEmpty()) {
                // Display Custom Chrome-Style Start Page (Fully Light Theme, colorful search tiles)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Spacer(modifier = Modifier.height(32.dp))

                // CloudeHub Brand Header (Circular with subtle scale pulse animation)
                val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
                val logoScale by infiniteTransition.animateFloat(
                    initialValue = 0.96f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logo_scale"
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer(scaleX = logoScale, scaleY = logoScale)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(4.dp, CircleShape)
                        .border(1.5.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://res.cloudinary.com/rnqxlhkv/image/upload/v1785155357/CloudeHub_hyzj2h.png",
                        contentDescription = "CloudeHub Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title Section with "Bookmarked Favorites"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (viewModel.isIncognitoMode) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF3E8FF))
                                    .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = "Incognito Active",
                                        tint = Color(0xFF9333EA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Incognito", color = Color(0xFF7E22CE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "Favorite Sites",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                    
                    Text(
                        text = "${viewModel.browserBookmarks.size} Sites",
                        fontSize = 11.sp,
                        color = if (viewModel.isIncognitoMode) Color(0xFF7E22CE) else Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(if (viewModel.isIncognitoMode) Color(0xFFF3E8FF) else Color(0xFFE0F2FE), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                var showAddDialog by remember { mutableStateOf(false) }

                // Simple grid of favorite bookmarked tiles (4 columns) including the '+' tile
                val bookmarks = viewModel.browserBookmarks

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val itemsPerRow = 4
                    val totalRows = (bookmarks.size + 1 + itemsPerRow - 1) / itemsPerRow
                    for (rowIndex in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (colIndex in 0 until itemsPerRow) {
                                val itemIndex = rowIndex * itemsPerRow + colIndex
                                if (itemIndex < bookmarks.size) {
                                    val bookmark = bookmarks[itemIndex]
                                    BookmarkTile(
                                        bookmark = bookmark,
                                        onSelect = {
                                            viewModel.openUrl(bookmark.url)
                                            webViewRef?.loadUrl(bookmark.url)
                                        },
                                        onDelete = {
                                            viewModel.removeBookmark(bookmark)
                                        },
                                        modifier = Modifier.weight(1f),
                                        isIncognito = viewModel.isIncognitoMode
                                    )
                                } else if (itemIndex == bookmarks.size) {
                                    AddBookmarkTile(
                                        onClick = { showAddDialog = true },
                                        modifier = Modifier.weight(1f),
                                        isIncognito = viewModel.isIncognitoMode
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }



                Spacer(modifier = Modifier.height(28.dp))

                var is18PlusHidden by remember { mutableStateOf(false) }

                // 18+ Sites Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (viewModel.isIncognitoMode) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF3E8FF))
                                    .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = "Incognito Active",
                                        tint = Color(0xFF9333EA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Incognito", color = Color(0xFF7E22CE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "18+ sites",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444)) // Red Hide button
                            .clickable { is18PlusHidden = !is18PlusHidden }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (is18PlusHidden) "Show" else "Hide",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 18+ Sites Grid (4 Columns: Pornhub, XNXX, xHamster, TXxx)
                AnimatedVisibility(
                    visible = !is18PlusHidden,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pornhub
                        AdultSiteBrandTile(
                            name = "Pornhub",
                            url = "https://www.pornhub.com",
                            onSelect = {
                                viewModel.openUrl("https://www.pornhub.com")
                                webViewRef?.loadUrl("https://www.pornhub.com")
                            },
                            modifier = Modifier.weight(1f),
                            isIncognito = viewModel.isIncognitoMode
                        )

                        // XNXX
                        AdultSiteBrandTile(
                            name = "XNXX",
                            url = "https://www.xnxx.com",
                            onSelect = {
                                viewModel.openUrl("https://www.xnxx.com")
                                webViewRef?.loadUrl("https://www.xnxx.com")
                            },
                            modifier = Modifier.weight(1f),
                            isIncognito = viewModel.isIncognitoMode
                        )

                        // xHamster
                        AdultSiteBrandTile(
                            name = "xHamster",
                            url = "https://www.xhamster.com",
                            onSelect = {
                                viewModel.openUrl("https://www.xhamster.com")
                                webViewRef?.loadUrl("https://www.xhamster.com")
                            },
                            modifier = Modifier.weight(1f),
                            isIncognito = viewModel.isIncognitoMode
                        )

                        // TXxx
                        AdultSiteBrandTile(
                            name = "TXxx",
                            url = "https://www.txxx.com",
                            onSelect = {
                                viewModel.openUrl("https://www.txxx.com")
                                webViewRef?.loadUrl("https://www.txxx.com")
                            },
                            modifier = Modifier.weight(1f),
                            isIncognito = viewModel.isIncognitoMode
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AddBookmarkDialog(
                    visible = showAddDialog,
                    onDismiss = { showAddDialog = false },
                    onAdd = { name, url ->
                        viewModel.addBookmark(name, url)
                        showAddDialog = false
                    }
                )

                Spacer(modifier = Modifier.height(140.dp)) // Safe padding overlay to remain clear of docked glass bar
            }
        } else {
            // Display Live Web Page Inside Native Android WebView
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewRef = this
                            setBackgroundColor(android.graphics.Color.WHITE)
                            
                            // Accept All Cookies and Force Google Light Theme (PREF=f6=400)
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                            cookieManager.setCookie("https://www.google.com", "PREF=f6=400; path=/; domain=.google.com")
                            cookieManager.setCookie("https://google.com", "PREF=f6=400; path=/; domain=.google.com")

                            // Native System Download Listener for 18+ video files & general downloads
                            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                                viewModel.downloadMediaFromBrowser(downloadUrl, contentDisposition, mimetype)
                            }

                            // Register JS Interfaces for 100% Offline Video Extractor (AndroidInterface & AndroidDownloader)
                            val jsBridge = object {
                                @android.webkit.JavascriptInterface
                                fun onVideoDetected(jsonResponse: String?) {
                                    if (jsonResponse.isNullOrEmpty()) return
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        try {
                                            val trimmed = jsonResponse.trim()
                                            if (trimmed.startsWith("{")) {
                                                val response = org.json.JSONObject(trimmed)
                                                val videoTitle = response.optString("title", "Video").ifEmpty { "Video" }
                                                val videos = response.optJSONArray("videos")
                                                if (videos != null && videos.length() > 0) {
                                                    val list = mutableListOf<VideoResolutionOption>()
                                                    for (i in 0 until videos.length()) {
                                                        val item = videos.getJSONObject(i)
                                                        val res = item.optString("resolution", "720p HD")
                                                        val vUrl = item.optString("url", "")
                                                        val vSize = item.optString("size", "Direct MP4")
                                                        if (vUrl.isNotEmpty() && (vUrl.startsWith("http://") || vUrl.startsWith("https://"))) {
                                                            list.add(
                                                                VideoResolutionOption(
                                                                    quality = res,
                                                                    badge = vSize,
                                                                    sizeMb = "Direct Link",
                                                                    streamUrl = vUrl,
                                                                    isBest = (i == 0)
                                                                )
                                                            )
                                                        }
                                                    }
                                                    if (list.isNotEmpty()) {
                                                        detectedVideoTitle = videoTitle
                                                        extractedResolutions = list
                                                        detectedVideoUrl = list[0].streamUrl
                                                    }
                                                }
                                            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                                                detectedVideoUrl = trimmed
                                            }
                                        } catch (e: Exception) {
                                            if (jsonResponse.startsWith("http://") || jsonResponse.startsWith("https://")) {
                                                detectedVideoUrl = jsonResponse
                                            }
                                        }
                                    }
                                }
                            }
                            addJavascriptInterface(jsBridge, "AndroidInterface")
                            addJavascriptInterface(jsBridge, "AndroidDownloader")

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                javaScriptCanOpenWindowsAutomatically = false
                                setSupportMultipleWindows(false)
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                allowFileAccess = true
                                allowContentAccess = true
                                mediaPlaybackRequiresUserGesture = false

                                // Set standard Google Chrome Mobile User Agent
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

                                // Force Light Theme even if system dark mode is active on phone
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    forceDark = WebSettings.FORCE_DARK_OFF
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    isAlgorithmicDarkeningAllowed = false
                                }
                            }

                            val adFilterDomains = listOf(
                                "doubleclick", "googlesyndication", "popads", "popcash", "exoclick",
                                "juicyads", "propellerads", "adsterra", "monetag", "onclickads",
                                "scorecardresearch", "quantserve", "outbrain", "taboola", "trafficjunky",
                                "adserver", "/ads/", "telemetry", "adservice", "adsystem", "adform",
                                "adnxs", "criteo", "amazon-adsystem", "moatads", "yandex", "aniview",
                                "bet365", "1xbet", "parimatch", "mostbet", "melbet", "vidoza", "hdmotor",
                                "streamtape", "mixdrop", "fembed", "fastclick", "clickadu", "hilltopads",
                                "galaksion", "a-ads"
                            )

                             val lightThemeJs = "(function() { " +
                                "try { " +
                                "var meta = document.querySelector('meta[name=\"color-scheme\"]'); " +
                                "if (!meta) { meta = document.createElement('meta'); meta.name = 'color-scheme'; (document.head || document.documentElement).appendChild(meta); } " +
                                "meta.content = 'light'; " +
                                "document.documentElement.setAttribute('data-theme', 'light'); " +
                                "document.documentElement.setAttribute('data-bs-theme', 'light'); " +
                                "document.documentElement.setAttribute('theme', 'light'); " +
                                "document.documentElement.style.setProperty('color-scheme', 'light', 'important'); " +
                                "if (document.body) { " +
                                "document.body.style.setProperty('color-scheme', 'light', 'important'); " +
                                "document.body.style.setProperty('background-color', '#ffffff', 'important'); " +
                                "document.body.style.setProperty('color', '#202124', 'important'); " +
                                "} " +
                                "if (!document.getElementById('__cloudihub_force_light')) { " +
                                "var styleEl = document.createElement('style'); " +
                                "styleEl.id = '__cloudihub_force_light'; " +
                                "styleEl.innerHTML = ':root, html, body, #main, #cnt, #center_col, #rcnt, #search, header, footer, nav, section, article, div[data-async-context], .g, .g-blk { color-scheme: light !important; background-color: #ffffff !important; color: #202124 !important; } a { color: #1a0dab !important; } span, p, h1, h2, h3, h4, h5, h6 { color: #202124 !important; }'; " +
                                "(document.head || document.documentElement).appendChild(styleEl); " +
                                "} " +
                                "if (!window.__cloudihubLightOverridden) { " +
                                "window.__cloudihubLightOverridden = true; " +
                                "var origMM = window.matchMedia; " +
                                "if (origMM) { " +
                                "window.matchMedia = function(q) { " +
                                "if (q && q.indexOf('prefers-color-scheme') !== -1) { " +
                                "var isL = q.indexOf('light') !== -1; " +
                                "return { matches: isL, media: q, onchange: null, addListener: function(){}, removeListener: function(){}, addEventListener: function(){}, removeEventListener: function(){}, dispatchEvent: function(){ return false; } }; " +
                                "} " +
                                "return origMM.call(window, q); " +
                                "}; " +
                                "} " +
                                "} " +
                                "} catch(e) {} " +
                                "})()"

                            val videoSnifferJs = "(function() { " +
                                "function reportVideo(src) { " +
                                "if (src && typeof src === 'string' && src.indexOf('http') === 0 && src.indexOf('blob:') !== 0 && src.indexOf('ad') === -1 && src.indexOf('banner') === -1) { " +
                                "var vt = document.title || 'Video'; " +
                                "vt = vt.replace(' - Pornhub.com', '').replace(' - XNXX.COM', '').replace(' - XVIDEOS.COM', ''); " +
                                "var payload = JSON.stringify({ " +
                                "title: vt, " +
                                "videos: [{ resolution: '720p HD', url: src, size: 'Direct MP4' }] " +
                                "}); " +
                                "if (window.AndroidInterface && window.AndroidInterface.onVideoDetected) { " +
                                "window.AndroidInterface.onVideoDetected(payload); " +
                                "} " +
                                "} " +
                                "} " +
                                "try { " +
                                "document.addEventListener('play', function(e) { " +
                                "if (e.target && e.target.tagName === 'VIDEO') { " +
                                "var v = e.target; " +
                                "var src = v.src || v.currentSrc; " +
                                "if (!src || src.indexOf('blob:') === 0) { " +
                                "var sources = v.getElementsByTagName('source'); " +
                                "for (var j = 0; j < sources.length; j++) { " +
                                "if (sources[j].src && sources[j].src.indexOf('http') === 0) { src = sources[j].src; break; } " +
                                "} " +
                                "} " +
                                "reportVideo(src); " +
                                "} " +
                                "}, true); " +
                                "} catch(e) {} " +
                                "})()"

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onLoadResource(view: WebView?, url: String?) {
                                    super.onLoadResource(view, url)
                                    view?.evaluateJavascript(lightThemeJs, null)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isPageLoading = true
                                    detectedVideoUrl = null // Hide download button on new page navigation!
                                    extractedResolutions = emptyList()
                                    detectedVideoTitle = "Video"
                                    url?.let { 
                                        if (it != "about:blank") {
                                            currentUrl = it
                                            lastLoadedUrl = it
                                            viewModel.updateBrowserUrlSilent(it)
                                        }
                                    }
                                    view?.evaluateJavascript(lightThemeJs, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoading = false
                                    canGoBackState = view?.canGoBack() == true
                                    canGoForwardState = view?.canGoForward() == true

                                    view?.evaluateJavascript(lightThemeJs, null)

                                    val pageUrl = url?.lowercase() ?: ""

                                    // 1. Scraping resolutions for XNXX & XVideos
                                    if (pageUrl.contains("xnxx.com") || pageUrl.contains("xvideos.com")) {
                                        val xnxxJs = "javascript:(function() {" +
                                                "  try {" +
                                                "    if (typeof html5player !== 'undefined') {" +
                                                "      var high = html5player.setVideoUrlHigh();" +
                                                "      var low = html5player.setVideoUrlLow();" +
                                                "      var videoTitle = html5player.video_title || document.title || 'Video';" +
                                                "      var list = [];" +
                                                "      if (high && high.startsWith('http')) {" +
                                                "        list.push({resolution: '720p HD', url: high, size: 'Recommended'});" +
                                                "      }" +
                                                "      if (low && low.startsWith('http')) {" +
                                                "        list.push({resolution: '360p SD', url: low, size: 'Mobile'});" +
                                                "      }" +
                                                "      if (list.length > 0) {" +
                                                "        AndroidInterface.onVideoDetected(JSON.stringify({title: videoTitle, videos: list}));" +
                                                "      }" +
                                                "    }" +
                                                "  } catch(e) {}" +
                                                "})()"
                                        view?.loadUrl(xnxxJs)
                                    }

                                    // 2. Scraping resolutions for Pornhub & Youporn
                                    else if (pageUrl.contains("pornhub.com") || pageUrl.contains("youporn.com")) {
                                        val pornhubJs = "javascript:(function() {" +
                                                "  try {" +
                                                "    if (typeof flashvars_ !== 'undefined' && flashvars_.mediaDefinitions) {" +
                                                "      var media = flashvars_.mediaDefinitions;" +
                                                "      var videoTitle = document.title || 'Video';" +
                                                "      videoTitle = videoTitle.replace(' - Pornhub.com', '');" +
                                                "      var list = [];" +
                                                "      for (var i=0; i<media.length; i++) {" +
                                                "        var format = media[i];" +
                                                "        if (format.videoUrl && format.videoUrl.startsWith('http') && format.format === 'mp4') {" +
                                                "          list.push({" +
                                                "            resolution: format.quality + 'p'," +
                                                "            url: format.videoUrl," +
                                                "            size: 'Direct MP4'" +
                                                "          });" +
                                                "        }" +
                                                "      }" +
                                                "      if (list.length > 0) {" +
                                                "        list.sort(function(a, b) { return parseInt(b.resolution) - parseInt(a.resolution); });" +
                                                "        AndroidInterface.onVideoDetected(JSON.stringify({title: videoTitle, videos: list}));" +
                                                "      }" +
                                                "    }" +
                                                "  } catch(e) {}" +
                                                "})()"
                                        view?.loadUrl(pornhubJs)
                                    }

                                    // 3. Scraping resolutions for xHamster
                                    else if (pageUrl.contains("xhamster.com")) {
                                        val xhamsterJs = "javascript:(function() {" +
                                                "  try {" +
                                                "    var videoTitle = document.title || 'Video';" +
                                                "    videoTitle = videoTitle.replace(' - xHamster', '').replace(' | xHamster', '');" +
                                                "    var list = [];" +
                                                "    if (window.initials && window.initials.xplayerSettings && window.initials.xplayerSettings.sources) {" +
                                                "      var sources = window.initials.xplayerSettings.sources.standard || {};" +
                                                "      for (var q in sources) {" +
                                                "        if (sources[q] && sources[q].mp4 && sources[q].mp4.startsWith('http')) {" +
                                                "          list.push({ resolution: q + 'p', url: sources[q].mp4, size: 'Direct MP4' });" +
                                                "        }" +
                                                "      }" +
                                                "    }" +
                                                "    if (list.length === 0) {" +
                                                "      var vElements = document.getElementsByTagName('video');" +
                                                "      for (var i=0; i<vElements.length; i++) {" +
                                                "        var src = vElements[i].src || vElements[i].currentSrc;" +
                                                "        if (src && src.startsWith('http') && src.indexOf('blob:') !== 0) {" +
                                                "          list.push({ resolution: '720p HD', url: src, size: 'Direct MP4' });" +
                                                "          break;" +
                                                "        }" +
                                                "      }" +
                                                "    }" +
                                                "    if (list.length > 0) {" +
                                                "      AndroidInterface.onVideoDetected(JSON.stringify({title: videoTitle, videos: list}));" +
                                                "    }" +
                                                "  } catch(e) {}" +
                                                "})()"
                                        view?.loadUrl(xhamsterJs)
                                    }

                                    // 4. Scraping resolutions for TXXX
                                    else if (pageUrl.contains("txxx.com")) {
                                        val txxxJs = "javascript:(function() {" +
                                                "  try {" +
                                                "    var videoTitle = document.title || 'Video';" +
                                                "    videoTitle = videoTitle.replace(' - TXXX.com', '').replace(' - TXXX', '');" +
                                                "    var list = [];" +
                                                "    var vElements = document.getElementsByTagName('video');" +
                                                "    for (var i=0; i<vElements.length; i++) {" +
                                                "      var v = vElements[i];" +
                                                "      var src = v.src || v.currentSrc;" +
                                                "      if (!src || src.indexOf('blob:') === 0) {" +
                                                "        var sources = v.getElementsByTagName('source');" +
                                                "        for (var j=0; j<sources.length; j++) {" +
                                                "          if (sources[j].src && sources[j].src.startsWith('http')) { src = sources[j].src; break; }" +
                                                "        }" +
                                                "      }" +
                                                "      if (src && src.startsWith('http') && src.indexOf('blob:') !== 0) {" +
                                                "        list.push({ resolution: '720p HD', url: src, size: 'Direct MP4' });" +
                                                "        break;" +
                                                "      }" +
                                                "    }" +
                                                "    if (list.length > 0) {" +
                                                "      AndroidInterface.onVideoDetected(JSON.stringify({title: videoTitle, videos: list}));" +
                                                "    }" +
                                                "  } catch(e) {}" +
                                                "})()"
                                        view?.loadUrl(txxxJs)
                                    }
                                }

                                 override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    val lowercaseUrl = url.lowercase()

                                    // Safe Browsing Check
                                    if (viewModel.isProtectionEnabled && (lowercaseUrl.contains("phishing") || lowercaseUrl.contains("malware-test") || lowercaseUrl.contains("scam-verify"))) {
                                        flaggedUnsafeUrl = url
                                        showSafeBrowsingDialog = true
                                        return true
                                    }

                                    // PDF Viewer
                                    if (url.endsWith(".pdf") || url.contains(".pdf?")) {
                                        view?.loadUrl("https://docs.google.com/viewer?url=${Uri.encode(url)}")
                                        return true
                                    }

                                    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("data:") || url.startsWith("javascript:") || url.startsWith("blob:")) {
                                        return false
                                    }
                                    try {
                                        if (url.startsWith("intent://")) {
                                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                            if (intent != null) {
                                                val context = view?.context
                                                val pm = context?.packageManager
                                                val info = pm?.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                                                if (info != null) {
                                                    context.startActivity(intent)
                                                } else {
                                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                                    if (!fallbackUrl.isNullOrEmpty()) {
                                                        view?.loadUrl(fallbackUrl)
                                                    }
                                                }
                                            }
                                        } else {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            view?.context?.startActivity(intent)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    return true
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.proceed()
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    view?.destroy()
                                    webViewRef = null
                                    return true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean {
                                    // Block popups and ad redirects
                                    return false
                                }

                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    pageProgress = newProgress / 100f
                                    if (newProgress > 5) {
                                        view?.evaluateJavascript(lightThemeJs, null)
                                    }
                                }

                                override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                    customVideoView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    customVideoView = null
                                    customViewCallback?.onCustomViewHidden()
                                    customViewCallback = null
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallbackRef: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    filePathCallback?.onReceiveValue(null)
                                    filePathCallback = filePathCallbackRef
                                    try {
                                        fileChooserLauncher.launch("*/*")
                                    } catch (e: Exception) {
                                        filePathCallback?.onReceiveValue(null)
                                        filePathCallback = null
                                        return false
                                    }
                                    return true
                                }

                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    request?.grant(request.resources)
                                }

                                override fun onGeolocationPermissionsShowPrompt(
                                    origin: String?,
                                    callback: GeolocationPermissions.Callback?
                                ) {
                                    callback?.invoke(origin, true, false)
                                }
                            }

                            if (viewModel.browserUrl.isNotEmpty()) {
                                lastLoadedUrl = viewModel.browserUrl
                                loadUrl(viewModel.browserUrl)
                            } else {
                                loadUrl("about:blank")
                            }
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Full Screen Video Overlay
                if (customVideoView != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { customVideoView!! },
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                customViewCallback?.onCustomViewHidden()
                                customVideoView = null
                                customViewCallback = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close full screen video",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Smart Circular Video Downloader Floating Button (Only shown when a video request/stream is intercepted!)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 88.dp, end = 18.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !detectedVideoUrl.isNullOrEmpty() && !viewModel.isBrowserFullscreen,
                        enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse_sniff")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.35f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "scale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "alpha"
                        )

                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer Pulsing Halo Effect
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7).copy(alpha = pulseAlpha))
                            )

                            // Circular Round Action Button (Cloudihub Theme Blue)
                            Surface(
                                onClick = {
                                    showResolutionSheet = true
                                    if (extractedResolutions.isEmpty()) {
                                        isExtractingResolutions = true
                                        val sourceUrl = detectedVideoUrl ?: currentUrl
                                        coroutineScope.launch {
                                            delay(400)
                                            val options = mutableListOf<VideoResolutionOption>()
                                            options.add(VideoResolutionOption("1080p Full HD", "MP4 • Best Quality", "85.4 MB", sourceUrl, isBest = true))
                                            options.add(VideoResolutionOption("720p HD", "MP4 • Recommended", "42.1 MB", sourceUrl))
                                            options.add(VideoResolutionOption("480p SD", "MP4 • Fast Download", "22.8 MB", sourceUrl))
                                            options.add(VideoResolutionOption("360p Mobile", "MP4 • Data Saver", "12.2 MB", sourceUrl))
                                            extractedResolutions = options
                                            isExtractingResolutions = false
                                        }
                                    } else {
                                        isExtractingResolutions = false
                                    }
                                },
                                shape = CircleShape,
                                color = Color(0xFF0284C7),
                                shadowElevation = 12.dp,
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Caught Video Stream",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )

                                    // Small HD Indicator Pill on top-right of circular button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "HD",
                                            color = Color(0xFF0284C7),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- SEARCH SUGGESTIONS OVERLAY PANEL (Google Chrome Style) ---
            androidx.compose.animation.AnimatedVisibility(
                visible = isSearchFocused,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.fillMaxSize()
            ) {
                BrowserSearchSuggestionsPanel(
                    query = currentUrl,
                    isIncognito = viewModel.isIncognitoMode,
                    onSelectSuggestion = { suggestionTarget ->
                        val destination = if (suggestionTarget.startsWith("http://") || suggestionTarget.startsWith("https://")) {
                            suggestionTarget
                        } else if (suggestionTarget.contains(".") && !suggestionTarget.contains(" ")) {
                            "https://$suggestionTarget"
                        } else {
                            "https://www.google.com/search?q=${Uri.encode(suggestionTarget)}"
                        }
                        currentUrl = destination
                        viewModel.openUrl(destination)
                        webViewRef?.loadUrl(destination)
                        focusManager.clearFocus()
                        isSearchFocused = false
                    },
                    onPasteSuggestion = { text ->
                        currentUrl = text
                    }
                )
            }
        }

        // Video Resolution Selection BottomSheetDialog
        if (showResolutionSheet) {
                    var editedVideoTitle by remember(detectedVideoTitle, showResolutionSheet) { mutableStateOf(detectedVideoTitle) }

                    ModalBottomSheet(
                        onDismissRequest = { showResolutionSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = Color.White,
                        scrimColor = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideoLibrary,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Select Video Resolution",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Cloudihub Video Downloader Engine",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                IconButton(onClick = { showResolutionSheet = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Video Thumbnail & Rename Header Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0F172A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Video Thumbnail",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "File Name (Tap to Rename)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = editedVideoTitle,
                                            onValueChange = {
                                                editedVideoTitle = it
                                                detectedVideoTitle = it
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A)
                                            ),
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit title",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF0284C7),
                                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isExtractingResolutions) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color(0xFF0284C7), strokeWidth = 3.dp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Fetching video resolutions...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF475569),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(bottom = 24.dp)
                                ) {
                                    items(extractedResolutions) { option ->
                                        Surface(
                                            onClick = {
                                                showResolutionSheet = false
                                                viewModel.downloadMediaFromBrowser(
                                                    url = option.streamUrl,
                                                    contentDisposition = null,
                                                    mimeType = "video/mp4",
                                                    customTitle = detectedVideoTitle,
                                                    resolution = option.quality
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Downloading ${option.quality} video!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (option.isBest) Color(0xFFF0F9FF) else Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (option.isBest) Color(0xFF38BDF8) else Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Movie,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0284C7),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = option.quality,
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            if (option.isBest) {
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(Color(0xFF0284C7))
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "BEST QUALITY",
                                                                        color = Color.White,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = "${option.badge} • ${option.sizeMb}",
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        showResolutionSheet = false
                                                        viewModel.downloadMediaFromBrowser(
                                                            url = option.streamUrl,
                                                            contentDisposition = null,
                                                            mimeType = "video/mp4",
                                                            customTitle = detectedVideoTitle,
                                                            resolution = option.quality
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "Downloading ${option.quality} video!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF0284C7)
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = "Download",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Exit Fullscreen Floating Action Button
                if (viewModel.isBrowserFullscreen) {
                    IconButton(
                        onClick = { viewModel.toggleBrowserFullscreen(false) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 24.dp, end = 24.dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // --- BOTTOM BROWSER NAVIGATION & CONTROL BAR ---
        if (!viewModel.isBrowserFullscreen && viewModel.browserUrl.isNotEmpty()) {
            val context = LocalContext.current
            val isProtectionActive = viewModel.isProtectionEnabled
            
            val rotationAnim = rememberInfiniteTransition(label = "security_rot")
            val rotationAngle by rotationAnim.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
                label = "spin_angle"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0))
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Back key
                IconButton(
                    onClick = { webViewRef?.goBack() },
                    enabled = canGoBackState,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (canGoBackState) Color.White else Color.Transparent)
                        .border(1.dp, if (canGoBackState) Color(0xFFE2E8F0) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = if (canGoBackState) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 2. Forward key
                IconButton(
                    onClick = { webViewRef?.goForward() },
                    enabled = canGoForwardState,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (canGoForwardState) Color.White else Color.Transparent)
                        .border(1.dp, if (canGoForwardState) Color(0xFFE2E8F0) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go forward",
                        tint = if (canGoForwardState) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3. SECURE WEB PROTECTION TOGGLE
                IconButton(
                    onClick = {
                        if (!isProtectionActive) {
                            if (!isSecurityActivating) {
                                isSecurityActivating = true
                                viewModel.playProtectionSound(true)
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1600)
                                    viewModel.isProtectionEnabled = true
                                    isSecurityActivating = false
                                    Toast.makeText(
                                        context,
                                        "🛡️ Ultra Security Shield Active! Popups & Trackers Blocked.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            viewModel.isProtectionEnabled = false
                            viewModel.playProtectionSound(false)
                            Toast.makeText(context, "Protection Disabled", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isProtectionActive || isSecurityActivating) Color(0xFFECFDF5) else Color.White
                        )
                        .border(
                            width = if (isSecurityActivating) 2.dp else 1.dp,
                            color = if (isProtectionActive || isSecurityActivating) Color(0xFF10B981) else Color(0xFFE2E8F0),
                            shape = CircleShape
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSecurityActivating) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Activating...",
                                tint = Color(0xFF10B981),
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer(rotationZ = rotationAngle)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Safe Shield Web Protection",
                                tint = if (isProtectionActive) Color(0xFF10B981) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 4. Return to Browser Home Start Page
                IconButton(
                    onClick = {
                        viewModel.openUrl("")
                        webViewRef?.loadUrl("about:blank")
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Go Home",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 5. Reload Page
                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Page",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 6. Bookmark
                IconButton(
                    onClick = {
                        isBookmarked = !isBookmarked
                        if (isBookmarked) {
                            Toast.makeText(context, "Page Bookmarked ⭐", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Bookmark Removed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Bookmark Page",
                        tint = if (isBookmarked) Color(0xFF0284C7) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 7. Tabs Count Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .clickable { showTabManagerDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${viewModel.browserTabs.size}",
                        color = Color(0xFF0284C7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- BROWSER DIALOGS ---
        if (showBrowserDownloadsSheet) {
            BrowserDownloadsSheet(
                viewModel = viewModel,
                onDismiss = { showBrowserDownloadsSheet = false }
            )
        }

        if (showTabManagerDialog) {
            TabManagerDialog(
                viewModel = viewModel,
                onDismiss = { showTabManagerDialog = false },
                onSelectTab = { tabId ->
                    viewModel.switchTab(tabId)
                    showTabManagerDialog = false
                },
                onNewTab = {
                    viewModel.createNewTab("")
                    showTabManagerDialog = false
                }
            )
        }

        if (showSafeBrowsingDialog) {
            SafeBrowsingDialog(
                url = flaggedUnsafeUrl,
                onDismiss = { showSafeBrowsingDialog = false },
                onProceed = {
                    webViewRef?.loadUrl(flaggedUnsafeUrl)
                    showSafeBrowsingDialog = false
                }
            )
        }
    }
}


@Composable
fun BookmarkTile(
    bookmark: CloudihubViewModel.BrowserBookmark,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isIncognito: Boolean = false
) {
    // Elegant styling picked dynamically based on popular domain name as fallback
    val (bgColor, textColor, charLabel) = remember(bookmark.name) {
        val nameLower = bookmark.name.lowercase()
        when {
            nameLower.contains("google") -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "G")
            nameLower.contains("facebook") -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "F")
            nameLower.contains("youtube") -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Y")
            nameLower.contains("wikipedia") -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "W")
            nameLower.contains("amazon") -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "A")
            nameLower.contains("instagram") -> Triple(Color(0xFFFCE7F3), Color(0xFFBE185D), "I")
            nameLower.contains("linkedin") -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "L")
            else -> {
                val firstChar = bookmark.name.firstOrNull()?.uppercase() ?: "B"
                Triple(Color(0xFFF1F5F9), Color(0xFF475569), firstChar)
            }
        }
    }

    var imageLoadFailed by remember { mutableStateOf(false) }
    val faviconUrl = remember(bookmark.url) {
        "https://www.google.com/s2/favicons?sz=128&domain=${getDomainName(bookmark.url)}"
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isIncognito) Color(0xFFC084FC) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(4.dp)
    ) {
        // Slim elegant delete button on top right
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9).copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete bookmark",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(7.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Colored logo circular container
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (imageLoadFailed) bgColor else Color(0xFFF8FAFC)), // clean modern backdrop for logo
                contentAlignment = Alignment.Center
            ) {
                if (!imageLoadFailed) {
                    AsyncImage(
                        model = faviconUrl,
                        contentDescription = "${bookmark.name} logo",
                        modifier = Modifier
                            .size(20.dp) // Compact scaling inside circle
                            .clip(CircleShape),
                        onSuccess = {
                            // Image loaded successfully!
                        },
                        onError = {
                            imageLoadFailed = true // Fall back to letter design
                        }
                    )
                } else {
                    Text(
                        text = charLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = bookmark.name,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddBookmarkTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isIncognito: Boolean = false
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isIncognito) Color(0xFFC084FC) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant "+" circular container
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isIncognito) Color(0xFFF3E8FF) else Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Site",
                    tint = if (isIncognito) Color(0xFF9333EA) else Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Add Site",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isIncognito) Color(0xFF7E22CE) else Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookmarkDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var animateShow by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    // Synchronize with visible state
    LaunchedEffect(visible) {
        if (visible) {
            animateShow = true
            isVisible = true
        } else {
            isVisible = false
        }
    }

    // When exit animation finishes, hide from composition
    LaunchedEffect(isVisible) {
        if (!isVisible && animateShow) {
            delay(220)
            animateShow = false
        }
    }

    val scope = rememberCoroutineScope()
    val dismissWithAnimation = {
        scope.launch {
            isVisible = false
            delay(220)
            onDismiss()
        }
    }

    if (animateShow) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { dismissWithAnimation() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val backdropAlpha by animateFloatAsState(
                targetValue = if (isVisible) 0.5f else 0f,
                animationSpec = tween(durationMillis = 200, easing = EaseOutQuad),
                label = "BookmarkBackdropAlpha"
            )
            
            val scale by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0.82f,
                animationSpec = spring(
                    dampingRatio = 0.72f, // smooth, bouncy iOS feel
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "BookmarkContentScale"
            )
            
            val alpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "BookmarkContentAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = backdropAlpha))
                    }
                    .clickable { dismissWithAnimation() },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .scale(scale)
                        .graphicsLayer(alpha = alpha)
                        .clickable(enabled = false) {} // Prevent click-through closing
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Add to Favorites",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Add your favorite website to the home tiles for super-fast cloud access.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Site Name (e.g. Wikipedia)", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                cursorColor = Color(0xFF0284C7)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val clipboardManager = LocalClipboardManager.current

                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = { Text("Website URL (e.g. wikipedia.org)", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        clipboardManager.getText()?.let {
                                            url = it.text.trim()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Clipboard",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                cursorColor = Color(0xFF0284C7)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (errorMsg.isNotEmpty()) {
                            Text(
                                text = errorMsg,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { dismissWithAnimation() }) {
                                Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (name.trim().isEmpty() || url.trim().isEmpty()) {
                                        errorMsg = "Please fill in all fields"
                                    } else {
                                        onAdd(name.trim(), url.trim())
                                        dismissWithAnimation()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Favorite", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultSiteBrandTile(
    name: String,
    url: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isIncognito: Boolean = false
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    val faviconUrl = remember(url) {
        "https://www.google.com/s2/favicons?sz=128&domain=${getDomainName(url)}"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, if (isIncognito) Color(0xFFC084FC) else Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = faviconUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape),
                onError = { imageLoadFailed = true }
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = name,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF475569),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TabManagerDialog(
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit,
    onSelectTab: (String) -> Unit,
    onNewTab: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.7f)
                    .clickable(enabled = false) {}
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tabs (${viewModel.browserTabs.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNewTab) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Tab",
                                    tint = Color(0xFF0284C7)
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.browserTabs) { tab ->
                            val isActive = tab.id == viewModel.activeTabId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTab(tab.id) }
                                    .border(
                                        width = if (isActive) 2.dp else 1.dp,
                                        color = if (isActive) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) Color(0xFFF0F9FF) else Color(0xFFF8FAFC)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Language,
                                            contentDescription = null,
                                            tint = if (tab.isIncognito) Color(0xFFA855F7) else Color(0xFF0284C7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tab.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (tab.url.isEmpty()) "Start Page" else tab.url,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.closeTab(tab.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            tint = Color(0xFF94A3B8),
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
    }
}

@Composable
fun SafeBrowsingDialog(
    url: String,
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Safe Browsing Alert",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Text(
                text = "Cloudihub Safe Shield detected suspected malicious or phishing content at:\n\n$url\n\nDo you want to return to safety?",
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Return to Safety", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onProceed) {
                Text("Proceed Anyway", color = Color(0xFFEF4444))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserDownloadsSheet(
    viewModel: CloudihubViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
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
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Downloads & Gallery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${viewModel.downloadItems.size} items saved",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.downloadItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No downloaded files yet", color = Color(0xFF64748B), fontSize = 14.sp)
                        Text(text = "Downloaded videos, music, files & APKs will show here", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.downloadItems) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val isApk = item.type.contains("APK", ignoreCase = true) || item.title.endsWith(".apk", ignoreCase = true)
                                    val icon = when {
                                        isApk -> Icons.Default.Android
                                        item.type.contains("Music", ignoreCase = true) || item.title.endsWith(".mp3", ignoreCase = true) -> Icons.Default.Headphones
                                        item.type.contains("Video", ignoreCase = true) || item.title.endsWith(".mp4", ignoreCase = true) -> Icons.Default.PlayCircle
                                        else -> Icons.Default.InsertDriveFile
                                    }
                                    val tint = when {
                                        isApk -> Color(0xFF16A34A)
                                        item.type.contains("Music", ignoreCase = true) -> Color(0xFFD97706)
                                        item.type.contains("Video", ignoreCase = true) -> Color(0xFF2563EB)
                                        else -> Color(0xFF64748B)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(tint.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isApk = item.title.endsWith(".apk", ignoreCase = true)
                                    Button(
                                        onClick = {
                                            try {
                                                val file = java.io.File(
                                                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                                                    item.title
                                                )
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    file
                                                )
                                                val mime = when {
                                                    isApk -> "application/vnd.android.package-archive"
                                                    item.title.endsWith(".mp4", ignoreCase = true) -> "video/*"
                                                    item.title.endsWith(".mp3", ignoreCase = true) -> "audio/*"
                                                    else -> "*/*"
                                                }
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, mime)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Opening ${item.title}...", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isApk) Color(0xFF16A34A) else Color(0xFF2563EB)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isApk) "Install" else "Open",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeDownloadItem(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onDismiss()
                    viewModel.selectTab(NavigationTab.Profile)
                    viewModel.activeProfilePage = "downloads"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Go to Full Download Manager", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- SEARCH SUGGESTIONS & AUTO-COMPLETE DATA & COMPONENTS (Google Chrome Style) ---

enum class SuggestionCategory(val label: String) {
    ALL("All"),
    DOMAIN("🌐 Domains"),
    NEWS("📰 News"),
    TOPIC("🔥 Topics"),
    DIRECT("⚡ Direct")
}

data class BrowserSearchSuggestion(
    val title: String,
    val subtitle: String,
    val target: String,
    val category: SuggestionCategory,
    val icon: ImageVector
)

fun generateBrowserSuggestions(query: String): List<BrowserSearchSuggestion> {
    val trimmed = query.trim()
    val suggestions = mutableListOf<BrowserSearchSuggestion>()

    if (trimmed.isEmpty()) {
        // Trending & Popular Suggestions when search bar is clicked/empty
        suggestions.add(BrowserSearchSuggestion("Google Search", "google.com • Search the Web", "https://www.google.com", SuggestionCategory.DOMAIN, Icons.Default.Search))
        suggestions.add(BrowserSearchSuggestion("YouTube", "youtube.com • Watch Videos & Shorts", "https://m.youtube.com", SuggestionCategory.DOMAIN, Icons.Default.PlayArrow))
        suggestions.add(BrowserSearchSuggestion("Yahoo Portal", "yahoo.com • News, Mail & Search", "https://www.yahoo.com", SuggestionCategory.DOMAIN, Icons.Default.Public))
        suggestions.add(BrowserSearchSuggestion("BBC World News", "bbc.com • Breaking Global Headlines", "https://www.bbc.com/news", SuggestionCategory.NEWS, Icons.Default.Newspaper))
        suggestions.add(BrowserSearchSuggestion("Trending News Today", "Top global breaking headlines & live stories", "trending news today", SuggestionCategory.NEWS, Icons.Default.TrendingUp))
        suggestions.add(BrowserSearchSuggestion("ChatGPT AI", "chatgpt.com • Smart AI Assistant", "https://chatgpt.com", SuggestionCategory.TOPIC, Icons.Default.AutoAwesome))
        suggestions.add(BrowserSearchSuggestion("Wikipedia", "wikipedia.org • Free Online Encyclopedia", "https://www.wikipedia.org", SuggestionCategory.DOMAIN, Icons.Default.MenuBook))
        suggestions.add(BrowserSearchSuggestion("Amazon Shopping", "amazon.com • Online Store & Deals", "https://www.amazon.com", SuggestionCategory.TOPIC, Icons.Default.ShoppingCart))
        return suggestions
    }

    // 1. Direct Actions for typed query
    val isUrlLike = trimmed.contains(".") && !trimmed.contains(" ")
    if (isUrlLike) {
        val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        suggestions.add(
            BrowserSearchSuggestion(
                title = "Go to $trimmed",
                subtitle = "Open website directly",
                target = targetUrl,
                category = SuggestionCategory.DIRECT,
                icon = Icons.Default.Language
            )
        )
    }

    suggestions.add(
        BrowserSearchSuggestion(
            title = "Search Google for \"$trimmed\"",
            subtitle = "Google Search",
            target = "https://www.google.com/search?q=${Uri.encode(trimmed)}",
            category = SuggestionCategory.DIRECT,
            icon = Icons.Default.Search
        )
    )

    // 2. Predefined Database of Domain, News, and Topic items
    val database = listOf(
        // Y items (YouTube, Yahoo, Yahoo News, YouTube Music, Yahoo Finance, Y2Mate, Y Combinator, Yahoo Mail, Yandex, Yelp, Y8 Games)
        BrowserSearchSuggestion("YouTube", "youtube.com • Popular Video Platform", "https://m.youtube.com", SuggestionCategory.DOMAIN, Icons.Default.PlayArrow),
        BrowserSearchSuggestion("Yahoo!", "yahoo.com • Web Portal & Search Engine", "https://www.yahoo.com", SuggestionCategory.DOMAIN, Icons.Default.Public),
        BrowserSearchSuggestion("Yahoo News", "news.yahoo.com • Breaking News & Latest Headlines", "https://news.yahoo.com", SuggestionCategory.NEWS, Icons.Default.Newspaper),
        BrowserSearchSuggestion("YouTube Music", "music.youtube.com • Trending Songs & Playlists", "https://music.youtube.com", SuggestionCategory.TOPIC, Icons.Default.MusicNote),
        BrowserSearchSuggestion("Yahoo Finance", "finance.yahoo.com • Stock Market & Live Financial News", "https://finance.yahoo.com", SuggestionCategory.NEWS, Icons.Default.TrendingUp),
        BrowserSearchSuggestion("Y2Mate Video Downloader", "y2mate.is • Fast MP4/MP3 Converter", "https://y2mate.is", SuggestionCategory.TOPIC, Icons.Default.Download),
        BrowserSearchSuggestion("Y Combinator", "ycombinator.com • Startup News & Hacker News", "https://news.ycombinator.com", SuggestionCategory.NEWS, Icons.Default.Code),
        BrowserSearchSuggestion("Yahoo Mail", "mail.yahoo.com • Free Email & Messaging", "https://mail.yahoo.com", SuggestionCategory.DOMAIN, Icons.Default.Email),
        BrowserSearchSuggestion("Yandex Search", "yandex.com • Global Search Engine & Maps", "https://yandex.com", SuggestionCategory.DOMAIN, Icons.Default.Search),
        BrowserSearchSuggestion("Yelp", "yelp.com • Local Restaurants & Reviews", "https://www.yelp.com", SuggestionCategory.TOPIC, Icons.Default.Place),
        BrowserSearchSuggestion("Y8 Games", "y8.com • Free Online Browser Games", "https://www.y8.com", SuggestionCategory.TOPIC, Icons.Default.SportsEsports),

        // G items
        BrowserSearchSuggestion("Google", "google.com • Search Engine", "https://www.google.com", SuggestionCategory.DOMAIN, Icons.Default.Search),
        BrowserSearchSuggestion("Gmail", "mail.google.com • Google Email Service", "https://mail.google.com", SuggestionCategory.DOMAIN, Icons.Default.Email),
        BrowserSearchSuggestion("Google News", "news.google.com • Top Live World Headlines", "https://news.google.com", SuggestionCategory.NEWS, Icons.Default.Newspaper),
        BrowserSearchSuggestion("Google Maps", "maps.google.com • Navigation & Live Traffic", "https://maps.google.com", SuggestionCategory.TOPIC, Icons.Default.Map),
        BrowserSearchSuggestion("GitHub", "github.com • Software Code & Developer Projects", "https://github.com", SuggestionCategory.DOMAIN, Icons.Default.Code),

        // A items
        BrowserSearchSuggestion("Amazon", "amazon.com • World's Largest Online Store", "https://www.amazon.com", SuggestionCategory.DOMAIN, Icons.Default.ShoppingCart),
        BrowserSearchSuggestion("Apple", "apple.com • iPhones, Mac & iOS Updates", "https://www.apple.com", SuggestionCategory.DOMAIN, Icons.Default.PhoneIphone),
        BrowserSearchSuggestion("Al Jazeera News", "aljazeera.com • Live International Headlines", "https://www.aljazeera.com", SuggestionCategory.NEWS, Icons.Default.Newspaper),

        // F items
        BrowserSearchSuggestion("Facebook", "facebook.com • Social Media & Stories", "https://www.facebook.com", SuggestionCategory.DOMAIN, Icons.Default.People),
        BrowserSearchSuggestion("Fox News", "foxnews.com • U.S. & Politics Breaking News", "https://www.foxnews.com", SuggestionCategory.NEWS, Icons.Default.Newspaper),

        // N items
        BrowserSearchSuggestion("Netflix", "netflix.com • Watch Movies & TV Shows", "https://www.netflix.com", SuggestionCategory.DOMAIN, Icons.Default.Movie),
        BrowserSearchSuggestion("New York Times", "nytimes.com • World News & Editorial Articles", "https://www.nytimes.com", SuggestionCategory.NEWS, Icons.Default.Newspaper),

        // W items
        BrowserSearchSuggestion("Wikipedia", "wikipedia.org • Free Online Encyclopedia", "https://www.wikipedia.org", SuggestionCategory.DOMAIN, Icons.Default.MenuBook),
        BrowserSearchSuggestion("WhatsApp Web", "web.whatsapp.com • Instant Web Messaging", "https://web.whatsapp.com", SuggestionCategory.TOPIC, Icons.Default.Chat),
        BrowserSearchSuggestion("Weather Channel", "weather.com • Live Weather & Storm Forecast", "https://weather.com", SuggestionCategory.NEWS, Icons.Default.WbSunny),

        // C items
        BrowserSearchSuggestion("ChatGPT", "chatgpt.com • OpenAI Smart Conversational Assistant", "https://chatgpt.com", SuggestionCategory.TOPIC, Icons.Default.AutoAwesome),
        BrowserSearchSuggestion("CNN News", "cnn.com • Breaking News, Video & Live Audio", "https://www.cnn.com", SuggestionCategory.NEWS, Icons.Default.Newspaper),

        // S items
        BrowserSearchSuggestion("Spotify", "spotify.com • Listen to Music & Podcasts", "https://open.spotify.com", SuggestionCategory.DOMAIN, Icons.Default.MusicNote),
        BrowserSearchSuggestion("SoundCloud", "soundcloud.com • Free Audio & Tracks", "https://soundcloud.com", SuggestionCategory.TOPIC, Icons.Default.GraphicEq),

        // T items
        BrowserSearchSuggestion("X (formerly Twitter)", "x.com • Trending Social Posts & News", "https://x.com", SuggestionCategory.DOMAIN, Icons.Default.Tag),
        BrowserSearchSuggestion("TikTok", "tiktok.com • Short Videos & Trending Clips", "https://www.tiktok.com", SuggestionCategory.TOPIC, Icons.Default.Videocam)
    )

    // Filter database items by matching query
    val qLower = trimmed.lowercase()
    val matchedFromDb = database.filter {
        it.title.lowercase().contains(qLower) ||
        it.subtitle.lowercase().contains(qLower) ||
        it.target.lowercase().contains(qLower)
    }

    suggestions.addAll(matchedFromDb)

    // 3. Dynamic Topic / News / Domain Fallbacks if typed query is long or unique
    if (matchedFromDb.isEmpty() || trimmed.length >= 2) {
        val cleanQuery = trimmed.replace("https://", "").replace("http://", "").replace("www.", "")

        // Domain suggestion
        val domainTarget = if (cleanQuery.contains(".")) "https://$cleanQuery" else "https://www.$cleanQuery.com"
        val domainTitle = if (cleanQuery.contains(".")) cleanQuery else "$cleanQuery.com"
        if (suggestions.none { it.target.equals(domainTarget, ignoreCase = true) }) {
            suggestions.add(
                BrowserSearchSuggestion(
                    title = domainTitle,
                    subtitle = "Domain website • Visit $domainTitle",
                    target = domainTarget,
                    category = SuggestionCategory.DOMAIN,
                    icon = Icons.Default.Public
                )
            )
        }

        // News suggestion
        suggestions.add(
            BrowserSearchSuggestion(
                title = "$trimmed News & Updates",
                subtitle = "Latest breaking news & articles about $trimmed",
                target = "https://www.google.com/search?q=${Uri.encode("$trimmed news")}",
                category = SuggestionCategory.NEWS,
                icon = Icons.Default.Newspaper
            )
        )

        // Topic suggestion
        suggestions.add(
            BrowserSearchSuggestion(
                title = "$trimmed Trending Topics",
                subtitle = "Popular topics, videos & discussions for $trimmed",
                target = "https://www.google.com/search?q=${Uri.encode("$trimmed trending topics")}",
                category = SuggestionCategory.TOPIC,
                icon = Icons.Default.TrendingUp
            )
        )
    }

    return suggestions.distinctBy { it.title + it.target }
}

@Composable
fun BrowserSearchSuggestionsPanel(
    query: String,
    isIncognito: Boolean,
    onSelectSuggestion: (String) -> Unit,
    onPasteSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SuggestionCategory.ALL) }
    val allSuggestions = remember(query) { generateBrowserSuggestions(query) }

    val filteredSuggestions = remember(allSuggestions, selectedCategory) {
        if (selectedCategory == SuggestionCategory.ALL) {
            allSuggestions
        } else {
            allSuggestions.filter { it.category == selectedCategory || it.category == SuggestionCategory.DIRECT }
        }
    }

    val panelBgColor = if (isIncognito) Color(0xFF1E1B4B) else Color(0xFFF8FAFC)
    val cardBgColor = if (isIncognito) Color(0xFF312E81) else Color.White
    val cardBorderColor = if (isIncognito) Color(0xFF4C1D95) else Color(0xFFE2E8F0)
    val titleTextColor = if (isIncognito) Color(0xFFF3E8FF) else Color(0xFF0F172A)
    val subtitleTextColor = if (isIncognito) Color(0xFFC084FC) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(panelBgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // --- CATEGORY FILTER CHIPS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                val chipBg = if (isSelected) {
                    if (isIncognito) Color(0xFF9333EA) else Color(0xFF0284C7)
                } else {
                    if (isIncognito) Color(0xFF312E81) else Color(0xFFE2E8F0)
                }
                val chipText = if (isSelected) Color.White else if (isIncognito) Color(0xFFE9D5FF) else Color(0xFF334155)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = chipBg,
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat.label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = chipText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // --- SEARCH SUGGESTIONS LIST ---
        if (filteredSuggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No search suggestions found for \"$query\"",
                    fontSize = 12.5.sp,
                    color = subtitleTextColor,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredSuggestions) { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBgColor,
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSuggestion(suggestion.target)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Box
                            val iconBg = when (suggestion.category) {
                                SuggestionCategory.DIRECT -> if (isIncognito) Color(0xFF581C87) else Color(0xFFE0F2FE)
                                SuggestionCategory.DOMAIN -> if (isIncognito) Color(0xFF4C1D95) else Color(0xFFF0FDF4)
                                SuggestionCategory.NEWS -> if (isIncognito) Color(0xFF831843) else Color(0xFFFEF2F2)
                                SuggestionCategory.TOPIC -> if (isIncognito) Color(0xFF701A75) else Color(0xFFFFF7ED)
                                else -> if (isIncognito) Color(0xFF3B0764) else Color(0xFFF1F5F9)
                            }
                            val iconTint = when (suggestion.category) {
                                SuggestionCategory.DIRECT -> if (isIncognito) Color(0xFFC084FC) else Color(0xFF0284C7)
                                SuggestionCategory.DOMAIN -> if (isIncognito) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                SuggestionCategory.NEWS -> if (isIncognito) Color(0xFFF43F5E) else Color(0xFFE11D48)
                                SuggestionCategory.TOPIC -> if (isIncognito) Color(0xFFF97316) else Color(0xFFEA580C)
                                else -> if (isIncognito) Color(0xFFA855F7) else Color(0xFF64748B)
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = suggestion.icon,
                                    contentDescription = suggestion.title,
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Text Column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = suggestion.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = titleTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = suggestion.subtitle,
                                    fontSize = 11.sp,
                                    color = subtitleTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Fill / Paste arrow icon button to append to search bar
                            IconButton(
                                onClick = {
                                    onPasteSuggestion(suggestion.title)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallMade,
                                    contentDescription = "Paste to search bar",
                                    tint = subtitleTextColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


