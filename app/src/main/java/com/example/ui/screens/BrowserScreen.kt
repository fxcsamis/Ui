package com.example.ui.screens

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.shadow
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudSkyBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(viewModel.browserUrl) }
    var isPageLoading by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableStateOf(0f) }
    var canGoBackState by remember { mutableStateOf(false) }
    var canGoForwardState by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    // Sync input box when viewModel url shifts
    LaunchedEffect(viewModel.browserUrl) {
        if (currentUrl != viewModel.browserUrl) {
            currentUrl = viewModel.browserUrl
            if (viewModel.browserUrl.isNotEmpty()) {
                webViewRef?.loadUrl(viewModel.browserUrl)
            } else {
                webViewRef?.loadUrl("about:blank")
            }
        }
    }

    val isStartPage = viewModel.browserUrl.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Force Slate 50 background for clean, glare-free Light Mode!
    ) {
        if (isStartPage) {
            CloudSkyBackground(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // --- TOP URL ADDRESS BAR & CONTROL ---
        if (!viewModel.isBrowserFullscreen) {
            val isStartPage = viewModel.browserUrl.isEmpty()
            val headerBgColor = if (isStartPage) Color.Transparent else Color.White
            val searchBoxBgColor = if (isStartPage) Color.White else Color(0xFFF1F5F9)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor) // Match start page background seamlessly!
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cloud styled URL Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(searchBoxBgColor)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))

                    // Use BasicTextField for perfect vertical centering and no clipping of text!
                    BasicTextField(
                        value = currentUrl,
                        onValueChange = { currentUrl = it },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
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
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_address_input"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp), // Prevent text clipping
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (currentUrl.isEmpty()) {
                                    Text(
                                        text = "Type URL or search...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp
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
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear address bar",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Refresh Key (Smaller and sharper size)
                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh page",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Fullscreen Toggle (Smaller and sharper size)
                IconButton(
                    onClick = { viewModel.toggleBrowserFullscreen(true) },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen mode",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(12.dp)
                    )
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
        } else {
            // Fullscreen status bar spacer to cover edges nicely
            Spacer(modifier = Modifier.statusBarsPadding())
        }

        // --- MAIN BROWSER AREA (START PAGE OR WEB VIEW) ---
        if (viewModel.browserUrl.isEmpty()) {
            // Display Custom Chrome-Style Start Page (Fully Light Theme, colorful search tiles)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Cloud Search Logo & Brand Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFE0F2FE)), // Beautiful soft sky blue background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Cloud Browser Logo",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "CloudBrowser",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "Lightweight Secured Cloud Search Engine",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Title Section with "Bookmarked Favorites"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favorite Sites",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    
                    Text(
                        text = "${viewModel.browserBookmarks.size} Sites",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(Color(0xFFE0F2FE), RoundedCornerShape(12.dp))
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
                                        modifier = Modifier.weight(1f)
                                    )
                                } else if (itemIndex == bookmarks.size) {
                                    AddBookmarkTile(
                                        onClick = { showAddDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
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
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewRef = this
                            
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                                // Force Light Theme even if system dark mode is active
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    forceDark = WebSettings.FORCE_DARK_OFF
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    isAlgorithmicDarkeningAllowed = false
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isPageLoading = true
                                    url?.let { 
                                        currentUrl = if (it == "about:blank") "" else it
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoading = false
                                    canGoBackState = view?.canGoBack() == true
                                    canGoForwardState = view?.canGoForward() == true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    pageProgress = newProgress / 100f
                                }
                            }

                            if (viewModel.browserUrl.isNotEmpty()) {
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

        // --- BOTTOM BROWSER BACK/FORWARD BAR ---
        if (!viewModel.isBrowserFullscreen && viewModel.browserUrl.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0))
                    .navigationBarsPadding() // Keep bottom bar safe from screen gesture/navigation lines
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Back key
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBackState,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (canGoBackState) Color.White else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = if (canGoBackState) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Forward key
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForwardState,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (canGoForwardState) Color.White else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Go forward",
                            tint = if (canGoForwardState) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // SECURE WEB PROTECTION TOGGLE (In the absolute center of Back & Home buttons!)
                val isProtectionActive = viewModel.isProtectionEnabled
                val protectionIconColor = if (isProtectionActive) Color(0xFF10B981) else Color(0xFF94A3B8)
                IconButton(
                    onClick = { viewModel.toggleProtection() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isProtectionActive) Color(0xFFECFDF5) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isProtectionActive) Color(0xFF34D399) else Color(0xFFE2E8F0),
                            shape = CircleShape
                        )
                        .then(
                            if (isProtectionActive) {
                                Modifier.shadow(8.dp, CircleShape, spotColor = Color(0xFF10B981), ambientColor = Color(0xFF10B981))
                            } else Modifier
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Safe Shield Web Protection",
                        tint = protectionIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Return to Browser Home Start Page
                IconButton(
                    onClick = {
                        viewModel.openUrl("")
                        webViewRef?.loadUrl("about:blank")
                    },
                    modifier = Modifier
                        .size(40.dp)
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
            }
        }
    }
    }
}

@Composable
fun BookmarkTile(
    bookmark: CloudihubViewModel.BrowserBookmark,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
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
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(8.dp)
    ) {
        // Slim elegant delete button on top right (made even smaller!)
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9).copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete bookmark",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(8.dp)
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (imageLoadFailed) bgColor else Color(0xFFF8FAFC)), // clean modern backdrop for logo
                contentAlignment = Alignment.Center
            ) {
                if (!imageLoadFailed) {
                    AsyncImage(
                        model = faviconUrl,
                        contentDescription = "${bookmark.name} logo",
                        modifier = Modifier
                            .size(28.dp) // Perfect scaling inside the circle
                            .clip(CircleShape),
                        onSuccess = {
                            // Image loaded successfully!
                        },
                        onError = {
                            imageLoadFailed = true // Fall back to beautiful letter design
                        }
                    )
                } else {
                    Text(
                        text = charLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bookmark.name,
                fontSize = 11.sp,
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant "+" circular container with a beautiful sky blue / primary color tone
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)), // Light Sky Blue
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Site",
                    tint = Color(0xFF0284C7), // Sky Blue Primary
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add Site",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569),
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
