package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.AmberLineHighlight
import com.example.ui.theme.LocalAmberTheme

enum class AmberTab(val title: String) {
    HOME("Home"),
    PLAYER("Player"),
    BROWSE("Browse"),
    LIBRARY("Library"),
    DOWNLOAD("Download"),
    SETUP("Setup")
}

@Composable
fun AmberBottomNav(
    currentTab: AmberTab,
    onTabSelected: (AmberTab) -> Unit,
    activeDownloadCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val theme = LocalAmberTheme.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = AmberGlassRaised,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberLineHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmberTab.entries.forEach { tab ->
                val isSelected = tab == currentTab

                val filledIcon: ImageVector = when (tab) {
                    AmberTab.HOME -> Icons.Default.Home
                    AmberTab.PLAYER -> Icons.Default.PlayCircle
                    AmberTab.BROWSE -> Icons.Default.Search
                    AmberTab.LIBRARY -> Icons.Default.LibraryMusic
                    AmberTab.DOWNLOAD -> Icons.Default.Download
                    AmberTab.SETUP -> Icons.Default.Settings
                }

                val outlinedIcon: ImageVector = when (tab) {
                    AmberTab.HOME -> Icons.Outlined.Home
                    AmberTab.PLAYER -> Icons.Outlined.PlayCircle
                    AmberTab.BROWSE -> Icons.Outlined.Search
                    AmberTab.LIBRARY -> Icons.Outlined.LibraryMusic
                    AmberTab.DOWNLOAD -> Icons.Outlined.Download
                    AmberTab.SETUP -> Icons.Outlined.Settings
                }

                val icon = if (isSelected) filledIcon else outlinedIcon
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) theme.accentLight else AmberDim,
                    label = "tabColor"
                )

                val pillBg = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            theme.accent.copy(alpha = 0.28f),
                            theme.accent.copy(alpha = 0.14f)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(pillBg)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 7.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (tab == AmberTab.DOWNLOAD && activeDownloadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = theme.accent,
                                        contentColor = Color(0xFF1D110B)
                                    ) {
                                        Text(
                                            text = if (activeDownloadCount > 9) "9+" else activeDownloadCount.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = tab.title,
                            color = contentColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
