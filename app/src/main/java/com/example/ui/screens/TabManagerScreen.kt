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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.ScreenRoute
import com.example.viewmodel.TabState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabManagerScreen(
    viewModel: BrowserViewModel
) {
    val tabs by viewModel.tabs.collectAsState()
    val incognitoTabs by viewModel.incognitoTabs.collectAsState()
    val isIncognitoMode by viewModel.isIncognitoMode.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val recentlyClosed by viewModel.recentlyClosedTabs.collectAsState()

    val currentTabList = if (isIncognitoMode) incognitoTabs else tabs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.tab_manager),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Mode Switcher Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEFF1F9)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                FilterChip(
                                    selected = !isIncognitoMode,
                                    onClick = { viewModel.toggleIncognitoMode(false) },
                                    label = { Text("${tabs.size} Tabs") },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                FilterChip(
                                    selected = isIncognitoMode,
                                    onClick = { viewModel.toggleIncognitoMode(true) },
                                    label = { Text("${incognitoTabs.size} Incognito") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenRoute.Browser) },
                        modifier = Modifier.testTag("tab_manager_back")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Manager")
                    }
                },
                actions = {
                    if (recentlyClosed.isNotEmpty()) {
                        IconButton(onClick = { viewModel.reopenRecentlyClosedTab() }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore Tab")
                        }
                    }
                    IconButton(onClick = { viewModel.closeAllTabs(isIncognitoMode) }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Close All")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createNewTab("albion://newtab", isIncognitoMode) },
                containerColor = Color(0xFF005AC1),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_tab_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Tab")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isIncognitoMode) Color(0xFF121214) else Color(0xFFFDFBFF))
        ) {
            if (isIncognitoMode) {
                Surface(
                    color = Color(0xFF231728),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFA8C7FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.private_mode_notice),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (currentTabList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Tab,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF44474E).copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No open tabs",
                            color = Color(0xFF44474E)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.createNewTab("albion://newtab", isIncognitoMode) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                        ) {
                            Text("Open New Tab")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentTabList, key = { it.id }) { tab ->
                        TabCardItem(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            isIncognito = isIncognitoMode,
                            onSelect = { viewModel.switchTab(tab.id, isIncognitoMode) },
                            onClose = { viewModel.closeTab(tab.id, isIncognitoMode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabCardItem(
    tab: TabState,
    isActive: Boolean,
    isIncognito: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isIncognito) Color(0xFF202026) else Color.White,
        shadowElevation = if (isActive) 6.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) Color(0xFF005AC1) else Color(0xFFDEE2F0),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isIncognito) Color(0xFF2C2C36) else Color(0xFFEFF1F9))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFF005AC1),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = tab.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isIncognito) Color.White else Color(0xFF1A1C1E),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp),
                        tint = if (isIncognito) Color.White else Color(0xFF1A1C1E)
                    )
                }
            }

            // Card Body Preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (tab.url.startsWith("albion://")) "Albion Home" else tab.url,
                        fontSize = 11.sp,
                        color = if (isIncognito) Color.White.copy(alpha = 0.6f) else Color(0xFF44474E),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD3E4FF)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
