package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.ScreenRoute
import com.example.viewmodel.TabState

@Composable
fun BrowserTopBar(
    viewModel: BrowserViewModel,
    activeTab: TabState,
    tabCount: Int,
    isIncognito: Boolean,
    onMenuAction: (ScreenRoute) -> Unit
) {
    val context = LocalContext.current
    val urlInputText by viewModel.urlInputText.collectAsState()
    val isFindInPageVisible by viewModel.isFindInPageVisible.collectAsState()
    val findQuery by viewModel.findInPageQuery.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val topBarHtmlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openLocalHtmlUri(context, uri)
        }
    }

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        color = if (isIncognito) Color(0xFF1B1B1F) else Color(0xFFFDFBFF)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Back Button
                IconButton(
                    onClick = { viewModel.goBack() },
                    enabled = activeTab.canGoBack,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (activeTab.canGoBack) {
                            if (isIncognito) Color.White else Color(0xFF1A1C1E)
                        } else Color.Gray.copy(alpha = 0.3f)
                    )
                }

                // Forward Button
                IconButton(
                    onClick = { viewModel.goForward() },
                    enabled = activeTab.canGoForward,
                    modifier = Modifier.testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (activeTab.canGoForward) {
                            if (isIncognito) Color.White else Color(0xFF1A1C1E)
                        } else Color.Gray.copy(alpha = 0.3f)
                    )
                }

                // Sleek Smart Address Bar
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isIncognito) Color(0xFF2C2C32) else Color(0xFFEFF1F9),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isIncognito) Color(0xFF3F3F46) else Color(0xFFDEE2F0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isIncognito) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Incognito",
                                tint = Color(0xFFA8C7FF),
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (activeTab.url.startsWith("https://")) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure SSL",
                                tint = Color(0xFF44474E),
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "Web",
                                tint = Color(0xFF005AC1),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // URL Input
                        TextField(
                            value = urlInputText,
                            onValueChange = { viewModel.setUrlInputText(it) },
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "Search or type web address",
                                    fontSize = 14.sp,
                                    color = Color(0xFF44474E).copy(alpha = 0.6f)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = if (isIncognito) Color.White else Color(0xFF1A1C1E),
                                unfocusedTextColor = if (isIncognito) Color.White else Color(0xFF1A1C1E)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.processInputAndLoad(urlInputText)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                onGo = {
                                    viewModel.processInputAndLoad(urlInputText)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                onDone = {
                                    viewModel.processInputAndLoad(urlInputText)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("address_bar_input")
                        )

                        if (urlInputText.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setUrlInputText("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF44474E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Tab Manager Pill (Sleek Blue Container Style)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isIncognito) Color(0xFF3B3B42)
                            else Color(0xFFD3E4FF)
                        )
                        .clickable { viewModel.navigateTo(ScreenRoute.TabManager) }
                        .testTag("open_tab_manager_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$tabCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isIncognito) Color.White else Color(0xFF001D36)
                    )
                }

                // Menu Button
                Box {
                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.testTag("top_bar_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = if (isIncognito) Color.White else Color(0xFF1A1C1E)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Home Page") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF005AC1)) },
                            onClick = {
                                showMenu = false
                                viewModel.openHomepage()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Tab") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF005AC1)) },
                            onClick = {
                                showMenu = false
                                viewModel.createNewTab("albion://newtab", false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open Local HTML File") },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF005AC1)) },
                            onClick = {
                                showMenu = false
                                topBarHtmlPicker.launch(arrayOf("text/html", "text/htm", "application/xhtml+xml", "*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Incognito Tab") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.createNewTab("albion://newtab", true)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMenuAction(ScreenRoute.BookmarksHistory)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("History") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMenuAction(ScreenRoute.BookmarksHistory)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Downloads") },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMenuAction(ScreenRoute.Downloads)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Privacy Center") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF005AC1)) },
                            onClick = {
                                showMenu = false
                                onMenuAction(ScreenRoute.PrivacyCenter)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Desktop Site", modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = activeTab.desktopMode,
                                        onCheckedChange = {
                                            viewModel.toggleDesktopModeForActiveTab()
                                            showMenu = false
                                        }
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                            onClick = {
                                viewModel.toggleDesktopModeForActiveTab()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Find in Page") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.toggleFindInPage(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View Page Source") },
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                val currentUrl = activeTab.url
                                if (!currentUrl.startsWith("albion://") && !currentUrl.startsWith("view-source:")) {
                                    viewModel.processInputAndLoad("view-source:$currentUrl")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMenuAction(ScreenRoute.Settings)
                            }
                        )
                    }
                }
            }

            // Find in Page Bar
            AnimatedVisibility(visible = isFindInPageVisible) {
                Surface(
                    color = Color(0xFFEFF1F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = { viewModel.setFindInPageQuery(it) },
                            placeholder = { Text("Find in page...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )
                        IconButton(onClick = { viewModel.toggleFindInPage(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Find")
                        }
                    }
                }
            }

            // Page Loading Progress Line
            if (activeTab.isLoading) {
                LinearProgressIndicator(
                    progress = { activeTab.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color(0xFF005AC1),
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}
