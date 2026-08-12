package com.example.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.repository.BrowserRepository
import com.example.data.repository.BrowserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.UUID

data class TabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "albion://newtab",
    val faviconUrl: String? = null,
    val isIncognito: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val desktopMode: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val adsBlockedOnPage: Int = 0,
    val trackersBlockedOnPage: Int = 0,
    val pageBlockedLogs: List<BlockedRequestLog> = emptyList()
)

sealed class ScreenRoute {
    object Browser : ScreenRoute()
    object TabManager : ScreenRoute()
    object BookmarksHistory : ScreenRoute()
    object Downloads : ScreenRoute()
    object PrivacyCenter : ScreenRoute()
    object Settings : ScreenRoute()
}

class BrowserViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    val settings = repository.settings
    val adsBlockedCount = repository.adsBlockedCount
    val trackersBlockedCount = repository.trackersBlockedCount
    val blockedLogs = repository.blockedLogs
    val recentlyClosedTabs = repository.recentlyClosedTabs

    // Active screen route
    private val _currentRoute = MutableStateFlow<ScreenRoute>(ScreenRoute.Browser)
    val currentRoute: StateFlow<ScreenRoute> = _currentRoute.asStateFlow()

    // Normal Tabs & Incognito Tabs
    private val _tabs = MutableStateFlow<List<TabState>>(listOf(TabState()))
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _incognitoTabs = MutableStateFlow<List<TabState>>(emptyList())
    val incognitoTabs: StateFlow<List<TabState>> = _incognitoTabs.asStateFlow()

    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    // URL bar state
    private val _urlInputText = MutableStateFlow("albion://newtab")
    val urlInputText: StateFlow<String> = _urlInputText.asStateFlow()

    // Find in page state
    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    private val _isFindInPageVisible = MutableStateFlow(false)
    val isFindInPageVisible: StateFlow<Boolean> = _isFindInPageVisible.asStateFlow()

    // Database reactive feeds
    val bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloads = repository.downloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val whitelistedDomains = repository.whitelistedDomains.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe tabs to auto sync or restore
    }

    // Navigation Action Flow for WebViews
    enum class WebViewNavAction { GO_BACK, GO_FORWARD, RELOAD }
    private val _webNavAction = MutableSharedFlow<WebViewNavAction>()
    val webNavAction: SharedFlow<WebViewNavAction> = _webNavAction.asSharedFlow()

    fun goBack() {
        viewModelScope.launch { _webNavAction.emit(WebViewNavAction.GO_BACK) }
    }

    fun goForward() {
        viewModelScope.launch { _webNavAction.emit(WebViewNavAction.GO_FORWARD) }
    }

    fun reload() {
        viewModelScope.launch { _webNavAction.emit(WebViewNavAction.RELOAD) }
    }

    fun navigateTo(route: ScreenRoute) {
        _currentRoute.value = route
    }

    fun setUrlInputText(text: String) {
        _urlInputText.value = text
    }

    fun getActiveTab(): TabState {
        val list = if (_isIncognitoMode.value) _incognitoTabs.value else _tabs.value
        return list.find { it.id == _activeTabId.value }
            ?: list.firstOrNull()
            ?: TabState().also { createNewTab("albion://newtab", _isIncognitoMode.value) }
    }

    fun createNewTab(url: String = "albion://newtab", isIncognito: Boolean = _isIncognitoMode.value) {
        val newTab = TabState(
            url = url,
            isIncognito = isIncognito,
            desktopMode = settings.value.defaultDesktopSite
        )
        if (isIncognito) {
            _incognitoTabs.value = _incognitoTabs.value + newTab
            _isIncognitoMode.value = true
        } else {
            _tabs.value = _tabs.value + newTab
            _isIncognitoMode.value = false
        }
        _activeTabId.value = newTab.id
        _urlInputText.value = url
        _currentRoute.value = ScreenRoute.Browser
    }

    fun switchTab(tabId: String, isIncognito: Boolean) {
        _isIncognitoMode.value = isIncognito
        _activeTabId.value = tabId
        val tab = getActiveTab()
        _urlInputText.value = tab.url
        _currentRoute.value = ScreenRoute.Browser
    }

    fun closeTab(tabId: String, isIncognito: Boolean) {
        if (isIncognito) {
            val list = _incognitoTabs.value.toMutableList()
            val target = list.find { it.id == tabId }
            if (target != null) {
                repository.pushRecentlyClosedTab(target.title, target.url)
                list.remove(target)
                _incognitoTabs.value = list
                if (list.isEmpty()) {
                    _isIncognitoMode.value = false
                    if (_tabs.value.isEmpty()) createNewTab("albion://newtab", false)
                    else _activeTabId.value = _tabs.value.first().id
                } else if (_activeTabId.value == tabId) {
                    _activeTabId.value = list.last().id
                }
            }
        } else {
            val list = _tabs.value.toMutableList()
            val target = list.find { it.id == tabId }
            if (target != null) {
                repository.pushRecentlyClosedTab(target.title, target.url)
                list.remove(target)
                _tabs.value = list
                if (list.isEmpty()) {
                    val newTab = TabState()
                    _tabs.value = listOf(newTab)
                    _activeTabId.value = newTab.id
                } else if (_activeTabId.value == tabId) {
                    _activeTabId.value = list.last().id
                }
            }
        }
    }

    fun closeAllTabs(isIncognito: Boolean) {
        if (isIncognito) {
            _incognitoTabs.value = emptyList()
            _isIncognitoMode.value = false
            if (_tabs.value.isEmpty()) createNewTab("albion://newtab", false)
            else _activeTabId.value = _tabs.value.first().id
        } else {
            _tabs.value = emptyList()
            val newTab = TabState()
            _tabs.value = listOf(newTab)
            _activeTabId.value = newTab.id
        }
    }

    fun reopenRecentlyClosedTab() {
        val item = repository.popRecentlyClosedTab() ?: return
        createNewTab(item.second, _isIncognitoMode.value)
    }

    fun toggleIncognitoMode(enable: Boolean) {
        _isIncognitoMode.value = enable
        if (enable && _incognitoTabs.value.isEmpty()) {
            createNewTab("albion://newtab", true)
        } else if (!enable && _tabs.value.isEmpty()) {
            createNewTab("albion://newtab", false)
        } else {
            val list = if (enable) _incognitoTabs.value else _tabs.value
            _activeTabId.value = list.first().id
            _urlInputText.value = list.first().url
        }
    }

    fun updateActiveTabState(
        title: String? = null,
        url: String? = null,
        faviconUrl: String? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null,
        isLoading: Boolean? = null,
        progress: Int? = null,
        desktopMode: Boolean? = null,
        adsBlockedOnPage: Int? = null,
        trackersBlockedOnPage: Int? = null,
        pageBlockedLogs: List<BlockedRequestLog>? = null
    ) {
        val isIncognito = _isIncognitoMode.value
        val list = (if (isIncognito) _incognitoTabs.value else _tabs.value).toMutableList()
        val index = list.indexOfFirst { it.id == _activeTabId.value }
        if (index != -1) {
            val old = list[index]
            val updated = old.copy(
                title = title ?: old.title,
                url = url ?: old.url,
                faviconUrl = faviconUrl ?: old.faviconUrl,
                canGoBack = canGoBack ?: old.canGoBack,
                canGoForward = canGoForward ?: old.canGoForward,
                isLoading = isLoading ?: old.isLoading,
                progress = progress ?: old.progress,
                desktopMode = desktopMode ?: old.desktopMode,
                adsBlockedOnPage = adsBlockedOnPage ?: old.adsBlockedOnPage,
                trackersBlockedOnPage = trackersBlockedOnPage ?: old.trackersBlockedOnPage,
                pageBlockedLogs = pageBlockedLogs ?: old.pageBlockedLogs
            )
            list[index] = updated
            if (isIncognito) _incognitoTabs.value = list else _tabs.value = list

            if (url != null && url != old.url) {
                _urlInputText.value = url
            }

            // Save history if not incognito
            if (!isIncognito && url != null && !url.startsWith("albion://") && !url.startsWith("about:")) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.addHistory(title ?: updated.title, url, faviconUrl)
                }
            }
        }
    }

    fun toggleDesktopModeForActiveTab() {
        val active = getActiveTab()
        updateActiveTabState(desktopMode = !active.desktopMode)
    }

    /**
     * Smart URL parser recognizing URLs vs search terms vs local files.
     */
    fun processInputAndLoad(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val formattedUrl = when {
            trimmed.startsWith("albion://") -> trimmed
            trimmed.startsWith("file://") || trimmed.startsWith("content://") || trimmed.startsWith("about:") || trimmed.startsWith("view-source:") -> trimmed
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            isDomainOrUrl(trimmed) -> "https://$trimmed"
            else -> buildSearchUrl(trimmed)
        }

        updateActiveTabState(url = formattedUrl, title = formattedUrl)
        _urlInputText.value = formattedUrl
        _currentRoute.value = ScreenRoute.Browser
    }

    fun openHomepage() {
        val url = when (settings.value.homepageType) {
            "Blank Page" -> "about:blank"
            "Custom" -> settings.value.homepageUrl.ifEmpty { "albion://newtab" }
            else -> "albion://newtab"
        }
        processInputAndLoad(url)
    }

    fun openLocalHtmlUri(context: Context, uri: Uri) {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }

            val htmlDir = File(context.filesDir, "local_html").apply { mkdirs() }
            val fileName = getFileNameFromUri(context, uri) ?: "local_file.html"
            val destFile = File(htmlDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val fileUrl = "file://${destFile.absolutePath}"
            createNewTab(fileUrl, false)
            Toast.makeText(context, "Loaded local HTML: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            createNewTab(uri.toString(), false)
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return File(uri.path ?: return null).name
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) result = it.getString(nameIndex)
                }
            }
        }
        return result
    }

    private fun isDomainOrUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ")) return false

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://") || trimmed.startsWith("about:")) {
            return true
        }

        if (android.util.Patterns.WEB_URL.matcher(trimmed).matches()) {
            return true
        }

        if (trimmed.contains(".")) {
            val hostPart = trimmed.split("/")[0].split("?")[0].split("#")[0].split(":")[0]
            if (hostPart.contains(".")) {
                val tld = hostPart.split(".").last().lowercase()
                if (tld.length >= 2 && tld.all { it.isLetter() }) {
                    return true
                }
            }
        }
        return false
    }

    private fun buildSearchUrl(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"
        val encoded = try {
            java.net.URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: Exception) {
            Uri.encode(trimmed)
        }
        return when (settings.value.searchEngine) {
            "Google" -> "https://www.google.com/search?q=$encoded"
            "Bing" -> "https://www.bing.com/search?q=$encoded"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encoded"
            "Brave" -> "https://search.brave.com/search?q=$encoded"
            "Ecosia" -> "https://www.ecosia.org/search?q=$encoded"
            "Custom" -> {
                val pattern = settings.value.customSearchEngineUrl
                if (pattern.contains("%s")) pattern.replace("%s", encoded)
                else "$pattern$encoded"
            }
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    fun toggleBookmarkForCurrentPage() {
        val current = getActiveTab()
        if (current.url.startsWith("albion://")) return

        viewModelScope.launch(Dispatchers.IO) {
            val isBookmarkedNow = repository.isDomainWhitelisted(current.url) // check bookmark
            repository.addBookmark(current.title, current.url, current.faviconUrl)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmark(bookmark)
        }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryItem(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllHistory()
        }
    }

    fun onAdBlocked(url: String, domain: String) {
        repository.incrementBlockedAds(url, domain)
        val active = getActiveTab()
        val newLog = BlockedRequestLog(url = url, domain = domain, blockType = "Ad")
        updateActiveTabState(
            adsBlockedOnPage = active.adsBlockedOnPage + 1,
            pageBlockedLogs = (listOf(newLog) + active.pageBlockedLogs).take(50)
        )
    }

    fun onTrackerBlocked(url: String, domain: String) {
        repository.incrementBlockedTrackers(url, domain)
        val active = getActiveTab()
        val newLog = BlockedRequestLog(url = url, domain = domain, blockType = "Tracker")
        updateActiveTabState(
            trackersBlockedOnPage = active.trackersBlockedOnPage + 1,
            pageBlockedLogs = (listOf(newLog) + active.pageBlockedLogs).take(50)
        )
    }

    fun clearBlockingStats() {
        repository.clearBlockingStats()
        val active = getActiveTab()
        updateActiveTabState(
            adsBlockedOnPage = 0,
            trackersBlockedOnPage = 0,
            pageBlockedLogs = emptyList()
        )
    }

    fun toggleWhitelist(domain: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.isDomainWhitelisted(domain)) {
                repository.removeWhitelistedDomain(domain)
            } else {
                repository.addWhitelistedDomain(domain)
            }
        }
    }

    fun clearBrowsingData(
        context: Context,
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearAdBlockStats: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            if (clearHistory) {
                clearAllHistory()
            }
            if (clearCookies) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            if (clearCache) {
                WebView(context).clearCache(true)
                WebStorage.getInstance().deleteAllData()
            }
            if (clearAdBlockStats) {
                clearBlockingStats()
            }
        }
    }

    fun updateSettings(newSettings: BrowserSettings) {
        repository.updateSettings(newSettings)
    }

    fun toggleFindInPage(show: Boolean) {
        _isFindInPageVisible.value = show
        if (!show) _findInPageQuery.value = ""
    }

    fun setFindInPageQuery(query: String) {
        _findInPageQuery.value = query
    }

    private var downloadPollingJob: Job? = null

    fun startDownload(
        context: Context,
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0
    ) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType ?: "application/octet-stream")
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription("Downloading $fileName...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType(mimeType ?: "*/*")
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrEmpty()) {
                    addRequestHeader("Cookie", cookie)
                }
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                Toast.makeText(context, "Download service unavailable", Toast.LENGTH_SHORT).show()
                return
            }
            val downloadId = dm.enqueue(request)

            val downloadItem = DownloadItem(
                systemDownloadId = downloadId,
                fileName = fileName,
                url = url,
                filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + fileName,
                fileSize = contentLength,
                progress = 0,
                status = "DOWNLOADING",
                mimeType = mimeType ?: "*/*"
            )
            viewModelScope.launch(Dispatchers.IO) {
                repository.addDownload(downloadItem)
            }
            startDownloadPolling(context)
            Toast.makeText(context, "Started download: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun startDownloadPolling(context: Context) {
        if (downloadPollingJob?.isActive == true) return
        downloadPollingJob = viewModelScope.launch(Dispatchers.IO) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return@launch
            while (isActive) {
                val currentDownloads = repository.downloads.first()
                val activeDownloads = currentDownloads.filter { it.status == "DOWNLOADING" && it.systemDownloadId != -1L }
                if (activeDownloads.isEmpty()) break

                for (item in activeDownloads) {
                    val query = DownloadManager.Query().setFilterById(item.systemDownloadId)
                    val cursor = dm.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                        val bytesDownloaded = if (bytesDownloadedIndex != -1) cursor.getLong(bytesDownloadedIndex) else 0L
                        val bytesTotal = if (bytesTotalIndex != -1) cursor.getLong(bytesTotalIndex) else 0L
                        val statusInt = if (statusIndex != -1) cursor.getInt(statusIndex) else -1

                        val progress = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else item.progress
                        val statusStr = when (statusInt) {
                            DownloadManager.STATUS_SUCCESSFUL -> "COMPLETED"
                            DownloadManager.STATUS_FAILED -> "FAILED"
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> "DOWNLOADING"
                            DownloadManager.STATUS_PAUSED -> "PAUSED"
                            else -> "DOWNLOADING"
                        }
                        val updated = item.copy(
                            progress = if (statusStr == "COMPLETED") 100 else progress,
                            fileSize = if (bytesTotal > 0) bytesTotal else item.fileSize,
                            status = statusStr
                        )
                        repository.updateDownload(updated)

                        if (statusStr == "COMPLETED" && settings.value.autoOpenDownloads) {
                            withContext(Dispatchers.Main) {
                                openDownloadedFile(context, updated)
                            }
                        }
                    } else {
                        repository.updateDownload(item.copy(status = "FAILED"))
                    }
                    cursor?.close()
                }
                delay(1000)
            }
        }
    }

    fun cancelDownload(context: Context, download: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (download.systemDownloadId != -1L) {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                dm?.remove(download.systemDownloadId)
            }
            repository.updateDownload(download.copy(status = "CANCELLED"))
        }
    }

    fun openDownloadedFile(context: Context, download: DownloadItem) {
        try {
            val file = File(download.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist: ${download.fileName}", Toast.LENGTH_SHORT).show()
                return
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, download.mimeType.ifEmpty { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun deleteDownload(download: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(download.filePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // Ignore file delete errors
            }
            repository.deleteDownload(download)
        }
    }
}

class BrowserViewModelFactory(
    private val repository: BrowserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
