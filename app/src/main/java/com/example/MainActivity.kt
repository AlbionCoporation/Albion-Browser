package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.repository.BrowserRepository
import com.example.ui.components.BrowserBottomBar
import com.example.ui.components.BrowserTopBar
import com.example.ui.components.WebViewContainer
import com.example.ui.screens.*
import com.example.ui.theme.AlbionBrowserTheme
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.BrowserViewModelFactory
import com.example.viewmodel.ScreenRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val repository = remember {
                val db = AppDatabase.getDatabase(context.applicationContext)
                BrowserRepository(db, context.applicationContext)
            }
            val viewModelFactory = remember { BrowserViewModelFactory(repository) }
            val viewModel: BrowserViewModel = viewModel(factory = viewModelFactory)

            val currentRoute by viewModel.currentRoute.collectAsState()
            val activeTab = viewModel.getActiveTab()
            val tabs by viewModel.tabs.collectAsState()
            val incognitoTabs by viewModel.incognitoTabs.collectAsState()
            val isIncognitoMode by viewModel.isIncognitoMode.collectAsState()
            val settings by viewModel.settings.collectAsState()

            val tabCount = if (isIncognitoMode) incognitoTabs.size else tabs.size

            AlbionBrowserTheme(themeMode = settings.themeMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentRoute == ScreenRoute.Browser) {
                            BrowserTopBar(
                                viewModel = viewModel,
                                activeTab = activeTab,
                                tabCount = tabCount,
                                isIncognito = isIncognitoMode,
                                onMenuAction = { route -> viewModel.navigateTo(route) }
                            )
                        }
                    },
                    bottomBar = {
                        if (currentRoute == ScreenRoute.Browser) {
                            BrowserBottomBar(
                                viewModel = viewModel,
                                tabCount = tabCount,
                                isIncognito = isIncognitoMode,
                                onNavigate = { route -> viewModel.navigateTo(route) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Crossfade(targetState = currentRoute, label = "screen_transition") { route ->
                            when (route) {
                                ScreenRoute.Browser -> {
                                    WebViewContainer(
                                        viewModel = viewModel,
                                        activeTab = activeTab,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                ScreenRoute.TabManager -> {
                                    TabManagerScreen(viewModel = viewModel)
                                }
                                ScreenRoute.BookmarksHistory -> {
                                    BookmarksHistoryScreen(viewModel = viewModel)
                                }
                                ScreenRoute.Downloads -> {
                                    DownloadsScreen(viewModel = viewModel)
                                }
                                ScreenRoute.PrivacyCenter -> {
                                    PrivacyCenterScreen(viewModel = viewModel)
                                }
                                ScreenRoute.Settings -> {
                                    SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
