package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.BlockedRequestLog
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.ScreenRoute
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val adsBlocked by viewModel.adsBlockedCount.collectAsState()
    val trackersBlocked by viewModel.trackersBlockedCount.collectAsState()
    val blockedLogs by viewModel.blockedLogs.collectAsState()
    val whitelistedDomains by viewModel.whitelistedDomains.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeTab = viewModel.getActiveTab()

    var showClearDataDialog by remember { mutableStateOf(false) }
    var clearHistoryChecked by remember { mutableStateOf(true) }
    var clearCookiesChecked by remember { mutableStateOf(true) }
    var clearCacheChecked by remember { mutableStateOf(true) }
    var clearAdStatsChecked by remember { mutableStateOf(true) }

    var newWhitelistInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_privacy_center), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenRoute.Browser) },
                        modifier = Modifier.testTag("privacy_center_back")
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
            // Live Stats Header Card (Sleek Style)
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFD3E4FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFADC6EF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = Color(0xFF005AC1),
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Albion Real Protection Shield",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36)
                        )

                        Text(
                            text = "Active Page: ${activeTab.adsBlockedOnPage} ads, ${activeTab.trackersBlockedOnPage} trackers blocked",
                            fontSize = 12.sp,
                            color = Color(0xFF001D36).copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$adsBlocked",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF005AC1)
                                )
                                Text(
                                    text = "Total Ads Blocked",
                                    fontSize = 13.sp,
                                    color = Color(0xFF001D36)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp),
                                color = Color(0xFFADC6EF)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$trackersBlocked",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF005AC1)
                                )
                                Text(
                                    text = "Total Trackers Blocked",
                                    fontSize = 13.sp,
                                    color = Color(0xFF001D36)
                                )
                            }
                        }
                    }
                }
            }

            // Blocking Controls Section
            item {
                Text(
                    text = "Protection Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEFF1F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Ad Blocker Toggle
                        PrivacyToggleRow(
                            title = stringResource(R.string.ad_blocker),
                            subtitle = "Blocks intrusive ads & banner scripts",
                            checked = settings.adBlockerEnabled,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(adBlockerEnabled = it)) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Tracker Blocker Toggle
                        PrivacyToggleRow(
                            title = stringResource(R.string.tracker_blocker),
                            subtitle = "Prevents third-party analytics & telemetry scripts",
                            checked = settings.trackerBlockerEnabled,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(trackerBlockerEnabled = it)) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Popups Toggle
                        PrivacyToggleRow(
                            title = stringResource(R.string.block_popups),
                            subtitle = "Blocks unwanted popup window requests",
                            checked = settings.popupsBlocked,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(popupsBlocked = it)) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Third-Party Cookies Toggle
                        PrivacyToggleRow(
                            title = stringResource(R.string.block_third_party_cookies),
                            subtitle = "Prevents cross-site cookie tracking",
                            checked = settings.thirdPartyCookiesBlocked,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(thirdPartyCookiesBlocked = it)) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2F0))

                        // Do Not Track
                        PrivacyToggleRow(
                            title = "Do Not Track Header",
                            subtitle = "Sends DNT signal to all requested servers",
                            checked = settings.doNotTrack,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(doNotTrack = it)) }
                        )
                    }
                }
            }

            // Real Verifiable Blocked Requests Log Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blocked Requests Log (${blockedLogs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    if (blockedLogs.isNotEmpty() || adsBlocked > 0 || trackersBlocked > 0) {
                        TextButton(onClick = { viewModel.clearBlockingStats() }) {
                            Text("Reset Stats", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (blockedLogs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEFF1F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ad or tracker requests blocked yet in this session.",
                                fontSize = 13.sp,
                                color = Color(0xFF44474E)
                            )
                        }
                    }
                }
            } else {
                items(blockedLogs.take(25)) { logItem ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF1F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (logItem.blockType == "Ad") Color(0xFFBA1A1A) else Color(0xFF005AC1),
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                Text(
                                    text = logItem.blockType.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = logItem.domain,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1A1C1E)
                                )
                                Text(
                                    text = logItem.url,
                                    fontSize = 11.sp,
                                    color = Color(0xFF44474E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = dateFormat.format(Date(logItem.timestamp)),
                                fontSize = 10.sp,
                                color = Color(0xFF74777F)
                            )
                        }
                    }
                }
            }

            // Whitelisted Domains Section
            item {
                Text(
                    text = "Whitelisted Websites",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newWhitelistInput,
                        onValueChange = { newWhitelistInput = it },
                        placeholder = { Text("e.g. example.com") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newWhitelistInput.isNotBlank()) {
                                viewModel.toggleWhitelist(newWhitelistInput.trim())
                                newWhitelistInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                    ) {
                        Text("Add")
                    }
                }
            }

            items(whitelistedDomains) { domainItem ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF1F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF005AC1),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = domainItem.domain,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { viewModel.toggleWhitelist(domainItem.domain) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }

            // Clear Data Button
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showClearDataDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("clear_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.clear_browsing_data), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Clear Browsing Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.clear_browsing_data)) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearHistoryChecked,
                            onCheckedChange = { clearHistoryChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF005AC1))
                        )
                        Text("Clear History")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearCookiesChecked,
                            onCheckedChange = { clearCookiesChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF005AC1))
                        )
                        Text("Clear Cookies")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearCacheChecked,
                            onCheckedChange = { clearCacheChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF005AC1))
                        )
                        Text("Clear Cache")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearAdStatsChecked,
                            onCheckedChange = { clearAdStatsChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF005AC1))
                        )
                        Text("Clear Ad-Blocking Statistics & Logs")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearBrowsingData(
                            context = context,
                            clearHistory = clearHistoryChecked,
                            clearCookies = clearCookiesChecked,
                            clearCache = clearCacheChecked,
                            clearAdBlockStats = clearAdStatsChecked
                        )
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A1C1E))
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF44474E))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF005AC1))
        )
    }
}
