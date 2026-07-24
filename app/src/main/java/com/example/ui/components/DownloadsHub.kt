package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DownloadStatus
import com.example.ui.DownloadTask
import com.example.ui.CloudihubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsHub(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.downloads.collectAsState()
    
    if (viewModel.showDownloadHub) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showDownloadHub = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFFF8FAFC),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(42.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                )
            },
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Downloads Hub",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Download Manager",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.showDownloadHub = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Hub",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of downloads
                if (downloads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CloudShape())
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "No downloads",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No active downloads",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Tap on any video card to stream or save offline.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(downloads, key = { it.videoId }) { task ->
                            DownloadItemRow(task)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemRow(task: DownloadTask) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = Color(0xFF1E293B),
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
                        },
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status icons
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    when (task.status) {
                        DownloadStatus.DOWNLOADING -> {
                            CircularProgressIndicator(
                                progress = task.progress,
                                strokeWidth = 3.dp,
                                color = Color(0xFF0284C7),
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DownloadStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DownloadStatus.QUEUED -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DownloadStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.QUEUED) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress slider
                LinearProgressIndicator(
                    progress = task.progress,
                    color = Color(0xFF0284C7),
                    trackColor = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format("%.1f", task.downloadedMb)} MB of ${String.format("%.1f", task.sizeMb)} MB",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${(task.progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
