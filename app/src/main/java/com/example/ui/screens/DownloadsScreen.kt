package com.example.ui.screens

import androidx.activity.compose.BackHandler
import com.example.ui.components.NavigationTab
import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.CloudihubViewModel
import com.example.ui.DownloadStatus
import com.example.ui.DownloadTask
import com.example.ui.components.LottieDownloadIcon
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloads by viewModel.downloads.collectAsState()
    
    BackHandler {
        if (viewModel.showFullScreenDownloads) {
            viewModel.showFullScreenDownloads = false
        } else if (viewModel.activeProfilePage == "downloads") {
            viewModel.activeProfilePage = "main"
        } else if (viewModel.activeTab != NavigationTab.Home) {
            viewModel.selectTab(NavigationTab.Home)
        }
    }

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

    val tabs = listOf("Download", "Videos", "Music", "Folders")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    // Fresh Cloud White Light Theme for Media Downloads Hub
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE), // Soft Sky Blue Light
                        Color(0xFFF8FAFC), // Pure Cloud White
                        Color(0xFFF1F5F9)  // Gentle Cloud Slate
                    )
                )
            )
            .testTag("downloads_fullscreen_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- DYNAMIC CURVED TOP BAR ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.80f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 10.dp)
                ) {
                    // --- TOP HEADER ROW ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (viewModel.showFullScreenDownloads) {
                                    viewModel.showFullScreenDownloads = false
                                } else if (viewModel.activeProfilePage == "downloads") {
                                    viewModel.activeProfilePage = "main"
                                } else if (viewModel.activeTab != NavigationTab.Home) {
                                    viewModel.selectTab(NavigationTab.Home)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Downloads",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    // --- TOP TABS (Download, Videos, Music, Folders) ---
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tabs.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            
                            val textColor by animateColorAsState(
                                if (isSelected) Color.White else Color(0xFF475569),
                                label = "tab_text"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected)
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                            )
                                        else
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                            )
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.Default.CloudDownload
                                            1 -> Icons.Default.PlayCircleOutline
                                            2 -> Icons.Default.MusicNote
                                            else -> Icons.Default.FolderOpen
                                        },
                                        contentDescription = tabs[index],
                                        tint = textColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tabs[index],
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // --- HORIZONTAL SLIDE PAGER FOR SECTIONS ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> DownloadSectionPage(viewModel = viewModel, downloads = downloads)
                    1 -> VideosSectionPage(viewModel = viewModel)
                    2 -> MusicSectionPage(viewModel = viewModel)
                    3 -> FoldersSectionPage(viewModel = viewModel)
                }
            }
        }
    }
}

// --- SECTION 0: DOWNLOAD PAGE ---
@Composable
private fun DownloadSectionPage(
    viewModel: CloudihubViewModel,
    downloads: List<DownloadTask>
) {
    val context = LocalContext.current

    if (downloads.isEmpty() && viewModel.downloadItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE))
                        .border(1.dp, Color(0xFFBAE6FD), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Empty",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Active Downloads",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap on any stream video card or file link to save offline.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Active Tasks
            if (downloads.isNotEmpty()) {
                item {
                    Text(
                        text = "ACTIVE QUEUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(downloads, key = { it.videoId }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.videoTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (task.status) {
                                            DownloadStatus.QUEUED -> "Queued in cloud..."
                                            DownloadStatus.DOWNLOADING -> "Downloading... ${task.speedMbps.toInt()} Mbps"
                                            DownloadStatus.COMPLETED -> "Saved offline"
                                            DownloadStatus.FAILED -> "Failed. Tap to retry."
                                        },
                                        fontSize = 12.sp,
                                        color = when (task.status) {
                                            DownloadStatus.DOWNLOADING -> Color(0xFF0284C7)
                                            DownloadStatus.COMPLETED -> Color(0xFF10B981)
                                            else -> Color(0xFF64748B)
                                        }
                                    )
                                }

                                CircularProgressIndicator(
                                    progress = task.progress,
                                    color = Color(0xFF0284C7),
                                    trackColor = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }

            // All items
            if (viewModel.downloadItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ALL DOWNLOADED FILES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(viewModel.downloadItems, key = { it.id }) { item ->
                    DownloadCardRow(
                        item = item,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// --- SECTION 1: VIDEOS PAGE ---
@Composable
private fun VideosSectionPage(viewModel: CloudihubViewModel) {
    val videos = viewModel.downloadItems.filter { it.type == "Video" }

    if (videos.isEmpty()) {
        EmptySectionPlaceholder(
            icon = Icons.Default.PlayCircleOutline,
            title = "No Video Downloads",
            subtitle = "Downloaded stream videos and clips will appear here."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                DownloadCardRow(
                    item = video,
                    viewModel = viewModel,
                    tagLabel = "4K Ultra HD"
                )
            }
        }
    }
}

// --- SECTION 2: MUSIC PAGE ---
@Composable
private fun MusicSectionPage(viewModel: CloudihubViewModel) {
    val musicTracks = viewModel.downloadItems.filter { it.type == "Music" }

    if (musicTracks.isEmpty()) {
        EmptySectionPlaceholder(
            icon = Icons.Default.MusicNote,
            title = "No Music Downloads",
            subtitle = "Saved offline audio tracks & melodies will appear here."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(musicTracks, key = { it.id }) { track ->
                DownloadCardRow(
                    item = track,
                    viewModel = viewModel,
                    tagLabel = "320kbps MP3"
                )
            }
        }
    }
}

// --- SECTION 3: FOLDERS PAGE ---
@Composable
private fun FoldersSectionPage(viewModel: CloudihubViewModel) {
    val context = LocalContext.current
    val folderList = listOf(
        FolderData("Cloud Downloads", "/Storage/Cloudihub/Downloads", "12 Files", "2.4 GB", Icons.Default.FolderZip, Color(0xFF0284C7)),
        FolderData("Video Streams", "/Storage/Cloudihub/Videos", "${viewModel.downloadItems.filter { it.type == "Video" }.size} Videos", "1.8 GB", Icons.Default.OndemandVideo, Color(0xFF8B5CF6)),
        FolderData("Music & Melodies", "/Storage/Cloudihub/Music", "${viewModel.downloadItems.filter { it.type == "Music" }.size} Tracks", "320 MB", Icons.Default.LibraryMusic, Color(0xFF10B981)),
        FolderData("Private Cache", "/Storage/Cloudihub/Vault", "4 Files", "12 KB", Icons.Default.Security, Color(0xFFF59E0B))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(folderList) { folder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Toast.makeText(context, "Opened ${folder.name} directory", Toast.LENGTH_SHORT).show()
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(folder.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = folder.icon,
                            contentDescription = folder.name,
                            tint = folder.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = folder.path,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = folder.size,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                        Text(
                            text = folder.count,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadCardRow(
    item: CloudihubViewModel.DownloadItem,
    viewModel: CloudihubViewModel,
    tagLabel: String? = null
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.type) {
                            "Video" -> Color(0xFF0284C7).copy(alpha = 0.12f)
                            "Music" -> Color(0xFF10B981).copy(alpha = 0.12f)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.type) {
                        "Video" -> Icons.Default.PlayArrow
                        "Music" -> Icons.Default.MusicNote
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = item.title,
                    tint = when (item.type) {
                        "Video" -> Color(0xFF0284C7)
                        "Music" -> Color(0xFF10B981)
                        else -> Color(0xFFD97706)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.size,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    if (tagLabel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE0F2FE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tagLabel,
                                fontSize = 10.sp,
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    if (item.type == "Video") {
                        viewModel.playVideo(
                            com.example.ui.CloudVideo(
                                id = item.id,
                                title = item.title,
                                duration = "05:20",
                                creator = "Offline Saved",
                                imageUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop",
                                views = "Offline",
                                fileUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                sizeMb = 450.0
                            )
                        )
                        Toast.makeText(context, "Playing offline video", Toast.LENGTH_SHORT).show()
                    } else if (item.type == "Music") {
                        viewModel.playTrack(
                            com.example.ui.CloudMusicTrack(
                                id = item.id,
                                title = item.title,
                                artist = "Offline Audio",
                                duration = "03:45",
                                durationSec = 225,
                                imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop",
                                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                            )
                        )
                        Toast.makeText(context, "Playing offline audio", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Opened file: ${item.title}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete file", color = Color(0xFFEF4444)) },
                        onClick = {
                            menuExpanded = false
                            viewModel.removeDownloadItem(item.id)
                            Toast.makeText(context, "File removed!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySectionPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE))
                    .border(1.dp, Color(0xFFBAE6FD), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class FolderData(
    val name: String,
    val path: String,
    val count: String,
    val size: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
