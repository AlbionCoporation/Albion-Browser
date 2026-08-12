package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startDownloadPolling(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_downloads), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenRoute.Browser) },
                        modifier = Modifier.testTag("downloads_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFDFBFF))
        ) {
            if (downloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF005AC1).copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No downloaded files", color = Color(0xFF44474E), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Saved to Downloads folder on device", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF1F9),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF005AC1), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Storage location: Downloads/", fontSize = 12.sp, color = Color(0xFF001D36), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    items(downloads, key = { it.id }) { download ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDEE2F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (download.mimeType) {
                                            "text/html", "text/htm" -> Icons.Default.Code
                                            "application/pdf" -> Icons.Default.PictureAsPdf
                                            "image/jpeg", "image/png" -> Icons.Default.Image
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF005AC1),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            download.fileName,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${formatBytes(download.fileSize)} • ${download.status}",
                                            fontSize = 12.sp,
                                            color = when (download.status) {
                                                "COMPLETED" -> Color(0xFF1E88E5)
                                                "DOWNLOADING" -> Color(0xFF2E7D32)
                                                "FAILED", "CANCELLED" -> Color(0xFFC62828)
                                                else -> Color(0xFF44474E)
                                            }
                                        )
                                    }

                                    if (download.status == "COMPLETED") {
                                        TextButton(onClick = { viewModel.openDownloadedFile(context, download) }) {
                                            Text("Open", fontWeight = FontWeight.Bold)
                                        }
                                    } else if (download.status == "DOWNLOADING") {
                                        IconButton(onClick = { viewModel.cancelDownload(context, download) }) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color.Red)
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteDownload(download) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }

                                if (download.status == "DOWNLOADING") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { download.progress / 100f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = Color(0xFF005AC1),
                                        trackColor = Color(0xFFE0E0E0),
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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}
