package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.ScreenRoute

data class QuickDialShortcut(
    val name: String,
    val url: String,
    val initial: String,
    val color: Color
)

@Composable
fun NewTabPage(
    viewModel: BrowserViewModel,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val adsBlocked by viewModel.adsBlockedCount.collectAsState()
    val trackersBlocked by viewModel.trackersBlockedCount.collectAsState()
    val history by viewModel.history.collectAsState()

    var queryText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val htmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openLocalHtmlUri(context, uri)
        }
    }

    val quickDials = listOf(
        QuickDialShortcut("Google", "https://www.google.com", "G", Color(0xFF4285F4)),
        QuickDialShortcut("Wiki", "https://www.wikipedia.org", "W", Color(0xFF005AC1)),
        QuickDialShortcut("YouTube", "https://www.youtube.com", "Y", Color(0xFFFF0000)),
        QuickDialShortcut("Reddit", "https://www.reddit.com", "R", Color(0xFFFF4500)),
        QuickDialShortcut("GitHub", "https://github.com", "G", Color(0xFF24292E)),
        QuickDialShortcut("BBC News", "https://www.bbc.com/news", "B", Color(0xFFBB1919)),
        QuickDialShortcut("DuckDuckGo", "https://duckduckgo.com", "D", Color(0xFFDE5833)),
        QuickDialShortcut("Amazon", "https://www.amazon.com", "A", Color(0xFFFF9900))
    )

    val backgroundBrush = when (settings.newTabWallpaper) {
        "Sunset Warmth" -> Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFDFBFF)))
        "Dark Cosmos" -> Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF1E1E24)))
        "Minimalist Slate" -> Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFFFDFBFF)))
        else -> Brush.verticalGradient(listOf(Color(0xFFE8F0FE), Color(0xFFFDFBFF)))
    }

    val isDarkBg = settings.newTabWallpaper == "Dark Cosmos"
    val primaryTextColor = if (isDarkBg) Color.White else Color(0xFF1A1C1E)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo & Branding
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF005AC1),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "A",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Albion Browser ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor
                        )
                        Text(
                            text = "1.0",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF005AC1)
                        )
                    }
                }
            }

            // Quick Actions Bar (e.g. Open Local HTML File)
            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            htmlPickerLauncher.launch(arrayOf("text/html", "text/htm", "application/xhtml+xml", "*/*"))
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF005AC1)
                        )
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Local HTML File", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // Central Search Bar
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = if (isDarkBg) Color(0xFF2C2C2E) else Color(0xFFEFF1F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkBg) Color(0xFF3A3A3C) else Color(0xFFDEE2F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF005AC1)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_or_type_url) + " (${settings.searchEngine})",
                                    color = if (isDarkBg) Color.LightGray else Color(0xFF44474E).copy(alpha = 0.7f),
                                    fontSize = 15.sp
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (queryText.isNotBlank()) {
                                        onSearchSubmit(queryText)
                                        keyboardController?.hide()
                                    }
                                },
                                onGo = {
                                    if (queryText.isNotBlank()) {
                                        onSearchSubmit(queryText)
                                        keyboardController?.hide()
                                    }
                                },
                                onDone = {
                                    if (queryText.isNotBlank()) {
                                        onSearchSubmit(queryText)
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_tab_search_input")
                        )
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = {
                                onSearchSubmit(queryText)
                                keyboardController?.hide()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Go",
                                    tint = Color(0xFF005AC1)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Dial Grid (Matching 4-Column Card Grid in Theme)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        quickDials.chunked(4).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEach { item ->
                                    SleekShortcutTile(item = item) {
                                        onOpenUrl(item.url)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Privacy Shield Active Card (Sleek Blue Container Style)
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFFD3E4FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFADC6EF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewModel.navigateTo(ScreenRoute.PrivacyCenter) }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Privacy Shield Active",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF001D36)
                                )
                                Text(
                                    text = "Browsing is currently protected",
                                    fontSize = 12.sp,
                                    color = Color(0xFF001D36).copy(alpha = 0.7f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF005AC1)
                            ) {
                                Text(
                                    text = "SECURE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "$adsBlocked",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF005AC1)
                                    )
                                    Text(
                                        text = "ADS BLOCKED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1C1E).copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "$trackersBlocked",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF005AC1)
                                    )
                                    Text(
                                        text = "TRACKERS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1C1E).copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recently Visited List
            if (history.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recently_visited),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E).copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        history.take(4).forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDEE2F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onOpenUrl(item.url) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF005AC1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1A1C1E),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = item.url,
                                            fontSize = 12.sp,
                                            color = Color(0xFF44474E),
                                            maxLines = 1
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
private fun SleekShortcutTile(
    item: QuickDialShortcut,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDEE2F0)),
            shadowElevation = 2.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = item.initial,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.color
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1C1E).copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}
