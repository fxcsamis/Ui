package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CloudSite
import com.example.ui.CloudihubViewModel

@Composable
fun SitesScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (viewModel.showCloudHubInProfile) {
                IconButton(
                    onClick = { viewModel.showCloudHubInProfile = false },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A)
                    )
                }
            }
            Text(
                text = "Cloud Services Hub",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Instant access to secure cloud storage & repositories",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid of Curated Sites
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(viewModel.cloudSites, key = { it.url }) { site ->
                CloudSiteCard(
                    site = site,
                    onLaunchClick = { viewModel.openUrl(site.url) }
                )
            }
        }
    }
}

@Composable
fun CloudSiteCard(
    site: CloudSite,
    onLaunchClick: () -> Unit
) {
    val brandColor = Color(site.colorHex)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            .clickable { onLaunchClick() }
            .testTag("site_card_${site.name.replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon holder matching Brand Color
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = mapIconToVector(site.iconName),
                    contentDescription = site.name,
                    tint = brandColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = site.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )

            Text(
                text = site.category.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action connect button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connect Portal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = "Open",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

private fun mapIconToVector(iconName: String): ImageVector {
    return when (iconName) {
        "folder" -> Icons.Default.Folder
        "code" -> Icons.Default.Code
        "music" -> Icons.Default.MusicNote
        "cloud" -> Icons.Default.Public
        "image" -> Icons.Default.Image
        "book" -> Icons.Default.Book
        else -> Icons.Default.Public
    }
}
