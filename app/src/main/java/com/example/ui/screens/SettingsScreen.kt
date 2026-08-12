package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel
) {
    val settings by viewModel.settings.collectAsState()

    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showTextZoomDialog by remember { mutableStateOf(false) }

    var customHomepageInput by remember(settings.homepageUrl) { mutableStateOf(settings.homepageUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenRoute.Browser) },
                        modifier = Modifier.testTag("settings_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFDFBFF))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Search & Navigation Preferences
            item {
                Text("Search & Navigation", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEFF1F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Search Engine Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSearchEngineDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Default Search Engine", fontWeight = FontWeight.Bold)
                                Text(settings.searchEngine, fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            TextButton(onClick = { showSearchEngineDialog = true }) {
                                Text("Change", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Homepage Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHomepageDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Homepage / Start Page", fontWeight = FontWeight.Bold)
                                Text(settings.homepageUrl, fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            TextButton(onClick = { showHomepageDialog = true }) {
                                Text("Change", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Appearance & Customization
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Appearance & Display", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEFF1F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Theme Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App Theme", fontWeight = FontWeight.Bold)
                                Text(settings.themeMode, fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            TextButton(onClick = { showThemeDialog = true }) {
                                Text("Select", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // New Tab Wallpaper Style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWallpaperDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("New Tab Page Style", fontWeight = FontWeight.Bold)
                                Text(settings.newTabWallpaper, fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            TextButton(onClick = { showWallpaperDialog = true }) {
                                Text("Customize", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Text Zoom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTextZoomDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Page Text Scaling", fontWeight = FontWeight.Bold)
                                Text("${settings.textZoom}%", fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            TextButton(onClick = { showTextZoomDialog = true }) {
                                Text("Adjust", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Web Page & Browsing Controls
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Browsing & Web Controls", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEFF1F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Always Desktop Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Default Desktop Mode", fontWeight = FontWeight.Bold)
                                Text("Request desktop version for all websites", fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            Switch(
                                checked = settings.defaultDesktopSite,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(defaultDesktopSite = it)) },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF005AC1))
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // JavaScript Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("JavaScript", fontWeight = FontWeight.Bold)
                                Text("Enable interactive web features and scripts", fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            Switch(
                                checked = settings.javascriptEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(javascriptEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF005AC1))
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Popups Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Block Pop-up Windows", fontWeight = FontWeight.Bold)
                                Text("Prevent sites from opening unprompted tabs", fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            Switch(
                                checked = settings.popupsBlocked,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(popupsBlocked = it)) },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF005AC1))
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Auto-Open Downloads Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Open Completed Downloads", fontWeight = FontWeight.Bold)
                                Text("Automatically launch files when download finishes", fontSize = 12.sp, color = Color(0xFF44474E))
                            }
                            Switch(
                                checked = settings.autoOpenDownloads,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(autoOpenDownloads = it)) },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF005AC1))
                            )
                        }
                    }
                }
            }

            // Privacy Center Shortcut
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Privacy & Security", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFD3E4FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF005AC1))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Privacy Shield & Ad Blocker", fontWeight = FontWeight.Bold, color = Color(0xFF001D36))
                                Text("View real blocked statistics and filter rules", fontSize = 12.sp, color = Color(0xFF001D36).copy(alpha = 0.7f))
                            }
                            Button(
                                onClick = { viewModel.navigateTo(ScreenRoute.PrivacyCenter) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                            ) {
                                Text("Manage")
                            }
                        }
                    }
                }
            }

            // About Application Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEFF1F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Albion Browser 1.0", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF005AC1))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("High Performance Mobile Browser • Real AdBlock & Local HTML", fontSize = 12.sp, color = Color(0xFF44474E))
                    }
                }
            }
        }
    }

    // Search Engine Dialog
    if (showSearchEngineDialog) {
        val engines = listOf("Google", "DuckDuckGo", "Bing", "Brave", "Ecosia")
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text("Select Search Engine") },
            text = {
                Column {
                    engines.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(searchEngine = engine))
                                    showSearchEngineDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.searchEngine == engine,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(searchEngine = engine))
                                    showSearchEngineDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(engine, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Homepage Dialog
    if (showHomepageDialog) {
        AlertDialog(
            onDismissRequest = { showHomepageDialog = false },
            title = { Text("Set Homepage") },
            text = {
                Column {
                    OutlinedTextField(
                        value = customHomepageInput,
                        onValueChange = { customHomepageInput = it },
                        label = { Text("Homepage URL or albion://newtab") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { customHomepageInput = "albion://newtab" }) {
                        Text("Use Default New Tab Page")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUrl = if (customHomepageInput.isBlank()) "albion://newtab" else customHomepageInput
                        viewModel.updateSettings(settings.copy(homepageUrl = finalUrl))
                        showHomepageDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomepageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        val themes = listOf("System Default", "Light Theme", "Dark Theme")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Theme") },
            text = {
                Column {
                    themes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(themeMode = theme))
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.themeMode == theme,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(themeMode = theme))
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(theme, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // New Tab Wallpaper Dialog
    if (showWallpaperDialog) {
        val wallpapers = listOf("Gradient Teal", "Sunset Warmth", "Dark Cosmos", "Minimalist Slate")
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("New Tab Style") },
            text = {
                Column {
                    wallpapers.forEach { wp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(newTabWallpaper = wp))
                                    showWallpaperDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.newTabWallpaper == wp,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(newTabWallpaper = wp))
                                    showWallpaperDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(wp, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Text Zoom Dialog
    if (showTextZoomDialog) {
        val zoomLevels = listOf(75, 100, 125, 150, 200)
        AlertDialog(
            onDismissRequest = { showTextZoomDialog = false },
            title = { Text("Page Text Scaling") },
            text = {
                Column {
                    zoomLevels.forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(textZoom = level))
                                    showTextZoomDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.textZoom == level,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(textZoom = level))
                                    showTextZoomDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$level%", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTextZoomDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
