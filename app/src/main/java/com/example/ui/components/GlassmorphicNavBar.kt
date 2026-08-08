package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

sealed class NavigationTab(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Profile : NavigationTab("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    object Home : NavigationTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Music : NavigationTab("music", "Music", Icons.Filled.Headphones, Icons.Outlined.Headphones)
    object Hub : NavigationTab("hub", "Hub", Icons.Filled.DeviceHub, Icons.Outlined.DeviceHub)
    object Browser : NavigationTab("browser", "Browser", Icons.Filled.Language, Icons.Outlined.Language)
}

@Composable
fun GlassmorphicNavBar(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavigationTab.Profile,
        NavigationTab.Music,
        NavigationTab.Home,
        NavigationTab.Hub,
        NavigationTab.Browser
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White.copy(alpha = 0.95f)) // Translucent white pill background
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(30.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                
                // Micro-interactions: Bouncy scale and translation float
                val tabScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.22f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.55f, // Fun bouncy spring
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "TabScale_${tab.route}"
                )

                val translationY by animateFloatAsState(
                    targetValue = if (isSelected) -4f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "TabTranslation_${tab.route}"
                )

                // Dot opacity and scale
                val indicatorScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.0f,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
                    label = "Indicator_${tab.route}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_tab_${tab.route}")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // Custom and clean ripple-free taps
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = translationY.dp)
                            .scale(tabScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color(0xFF0284C7) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Slim glowing active dot indicator
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .scale(indicatorScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                )
                            )
                    )
                }
            }
        }
    }
}
}
