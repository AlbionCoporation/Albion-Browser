package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.ScreenRoute

@Composable
fun BrowserBottomBar(
    viewModel: BrowserViewModel,
    tabCount: Int,
    isIncognito: Boolean,
    onNavigate: (ScreenRoute) -> Unit
) {
    Surface(
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
        color = if (isIncognito) Color(0xFF1B1B1F) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isIncognito) Color(0xFF2C2C32) else Color(0xFFDEE2F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Action
            IconButton(
                onClick = { viewModel.goBack() },
                modifier = Modifier.testTag("bottom_nav_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isIncognito) Color.White.copy(alpha = 0.6f) else Color(0xFF1A1C1E).copy(alpha = 0.6f)
                )
            }

            // Forward Action
            IconButton(
                onClick = { viewModel.goForward() },
                modifier = Modifier.testTag("bottom_nav_forward")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (isIncognito) Color.White.copy(alpha = 0.6f) else Color(0xFF1A1C1E).copy(alpha = 0.6f)
                )
            }

            // Home Button (Pill in #D3E4FF matching design)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isIncognito) Color(0xFF005AC1) else Color(0xFFD3E4FF))
                    .clickable { viewModel.openHomepage() }
                    .testTag("bottom_nav_home"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (isIncognito) Color.White else Color(0xFF001D36),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tabs Manager Button with Badge Count
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigate(ScreenRoute.TabManager) }
                    .testTag("bottom_nav_tabs"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = if (isIncognito) Color.White else Color(0xFF1A1C1E),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$tabCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncognito) Color.White else Color(0xFF1A1C1E)
                    )
                }
            }

            // Settings Button
            IconButton(
                onClick = { onNavigate(ScreenRoute.Settings) },
                modifier = Modifier.testTag("bottom_nav_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isIncognito) Color.White else Color(0xFF1A1C1E)
                )
            }
        }
    }
}
