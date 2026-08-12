package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrowserSettings(
    val searchEngine: String = "Google", // Google, DuckDuckGo, Bing, Brave, Ecosia, Custom
    val customSearchEngineUrl: String = "https://search.brave.com/search?q=%s",
    val homepageType: String = "New Tab", // New Tab, Blank Page, Custom
    val homepageUrl: String = "albion://newtab",
    val adBlockerEnabled: Boolean = true,
    val trackerBlockerEnabled: Boolean = true,
    val adBlockerMode: String = "Standard", // Standard, Strict
    val popupsBlocked: Boolean = true,
    val thirdPartyCookiesBlocked: Boolean = true,
    val doNotTrack: Boolean = true,
    val defaultDesktopSite: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val textZoom: Int = 100, // 75, 100, 125, 150, 200
    val themeMode: String = "System", // Light, Dark, System
    val accentColorHex: String = "#3A86FF",
    val newTabWallpaper: String = "Gradient Teal", // Gradient Teal, Sunset Warmth, Dark Cosmos, Minimalist Slate
    val openLinksInBackground: Boolean = false,
    val autoOpenDownloads: Boolean = true,
    val closeTabOnBack: Boolean = true,
    val appLanguage: String = "System"
)

class BrowserRepository(private val db: AppDatabase, context: Context) {

    val bookmarks: Flow<List<Bookmark>> = db.bookmarkDao().getAllBookmarks()
    val history: Flow<List<HistoryItem>> = db.historyDao().getAllHistory()
    val downloads: Flow<List<DownloadItem>> = db.downloadDao().getAllDownloads()
    val whitelistedDomains: Flow<List<WhitelistedDomain>> = db.whitelistDao().getAllWhitelisted()
    val tabs: Flow<List<TabEntity>> = db.tabDao().getAllTabs()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("albion_browser_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettingsFromPrefs())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    private val _adsBlockedCount = MutableStateFlow(prefs.getLong("ads_blocked_count", 0L))
    val adsBlockedCount: StateFlow<Long> = _adsBlockedCount.asStateFlow()

    private val _trackersBlockedCount = MutableStateFlow(prefs.getLong("trackers_blocked_count", 0L))
    val trackersBlockedCount: StateFlow<Long> = _trackersBlockedCount.asStateFlow()

    private val _blockedLogs = MutableStateFlow<List<BlockedRequestLog>>(emptyList())
    val blockedLogs: StateFlow<List<BlockedRequestLog>> = _blockedLogs.asStateFlow()

    // Closed tabs restoration stack
    private val _recentlyClosedTabs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val recentlyClosedTabs: StateFlow<List<Pair<String, String>>> = _recentlyClosedTabs.asStateFlow()

    fun isBookmarked(url: String): Flow<Boolean> = db.bookmarkDao().isBookmarked(url)

    suspend fun addBookmark(title: String, url: String, faviconUrl: String? = null) {
        db.bookmarkDao().insertBookmark(Bookmark(title = title, url = url, faviconUrl = faviconUrl))
    }

    suspend fun deleteBookmark(bookmark: Bookmark) {
        db.bookmarkDao().deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkByUrl(url: String) {
        db.bookmarkDao().deleteBookmarkByUrl(url)
    }

    suspend fun addHistory(title: String, url: String, faviconUrl: String? = null) {
        if (url.startsWith("albion://") || url.startsWith("about:")) return
        db.historyDao().insertHistory(HistoryItem(title = title, url = url, faviconUrl = faviconUrl))
    }

    suspend fun deleteHistoryItem(item: HistoryItem) {
        db.historyDao().deleteHistoryItem(item)
    }

    suspend fun clearAllHistory() {
        db.historyDao().clearAllHistory()
    }

    suspend fun addDownload(download: DownloadItem): Long {
        return db.downloadDao().insertDownload(download)
    }

    suspend fun updateDownload(download: DownloadItem) {
        db.downloadDao().updateDownload(download)
    }

    suspend fun deleteDownload(download: DownloadItem) {
        db.downloadDao().deleteDownload(download)
    }

    suspend fun clearAllDownloads() {
        db.downloadDao().clearAllDownloads()
    }

    suspend fun addWhitelistedDomain(domain: String) {
        db.whitelistDao().insertWhitelist(WhitelistedDomain(domain = domain.lowercase()))
    }

    suspend fun removeWhitelistedDomain(domain: String) {
        db.whitelistDao().deleteWhitelist(WhitelistedDomain(domain = domain.lowercase()))
    }

    suspend fun isDomainWhitelisted(domain: String): Boolean {
        return db.whitelistDao().isWhitelisted(domain.lowercase())
    }

    suspend fun saveTabs(tabEntities: List<TabEntity>) {
        db.tabDao().clearAllTabs()
        db.tabDao().saveTabs(tabEntities)
    }

    fun incrementBlockedAds(url: String, domain: String) {
        val newCount = _adsBlockedCount.value + 1
        _adsBlockedCount.value = newCount
        prefs.edit().putLong("ads_blocked_count", newCount).apply()
        addBlockedLog(BlockedRequestLog(url = url, domain = domain, blockType = "Ad"))
    }

    fun incrementBlockedTrackers(url: String, domain: String) {
        val newCount = _trackersBlockedCount.value + 1
        _trackersBlockedCount.value = newCount
        prefs.edit().putLong("trackers_blocked_count", newCount).apply()
        addBlockedLog(BlockedRequestLog(url = url, domain = domain, blockType = "Tracker"))
    }

    private fun addBlockedLog(log: BlockedRequestLog) {
        val current = _blockedLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 100) {
            current.removeAt(current.lastIndex)
        }
        _blockedLogs.value = current
    }

    fun clearBlockingStats() {
        _adsBlockedCount.value = 0L
        _trackersBlockedCount.value = 0L
        _blockedLogs.value = emptyList()
        prefs.edit()
            .putLong("ads_blocked_count", 0L)
            .putLong("trackers_blocked_count", 0L)
            .apply()
    }

    fun pushRecentlyClosedTab(title: String, url: String) {
        if (url.startsWith("albion://")) return
        val current = _recentlyClosedTabs.value.toMutableList()
        current.add(0, Pair(title, url))
        if (current.size > 10) current.removeAt(current.lastIndex)
        _recentlyClosedTabs.value = current
    }

    fun popRecentlyClosedTab(): Pair<String, String>? {
        val current = _recentlyClosedTabs.value.toMutableList()
        if (current.isEmpty()) return null
        val item = current.removeAt(0)
        _recentlyClosedTabs.value = current
        return item
    }

    fun updateSettings(newSettings: BrowserSettings) {
        _settings.value = newSettings
        saveSettingsToPrefs(newSettings)
    }

    private fun loadSettingsFromPrefs(): BrowserSettings {
        return BrowserSettings(
            searchEngine = prefs.getString("search_engine", "Google") ?: "Google",
            customSearchEngineUrl = prefs.getString("custom_search_engine_url", "https://search.brave.com/search?q=%s") ?: "https://search.brave.com/search?q=%s",
            homepageType = prefs.getString("homepage_type", "New Tab") ?: "New Tab",
            homepageUrl = prefs.getString("homepage_url", "albion://newtab") ?: "albion://newtab",
            adBlockerEnabled = prefs.getBoolean("ad_blocker_enabled", true),
            trackerBlockerEnabled = prefs.getBoolean("tracker_blocker_enabled", true),
            adBlockerMode = prefs.getString("ad_blocker_mode", "Standard") ?: "Standard",
            popupsBlocked = prefs.getBoolean("popups_blocked", true),
            thirdPartyCookiesBlocked = prefs.getBoolean("third_party_cookies_blocked", true),
            doNotTrack = prefs.getBoolean("do_not_track", true),
            defaultDesktopSite = prefs.getBoolean("default_desktop_site", false),
            javascriptEnabled = prefs.getBoolean("javascript_enabled", true),
            textZoom = prefs.getInt("text_zoom", 100),
            themeMode = prefs.getString("theme_mode", "System") ?: "System",
            accentColorHex = prefs.getString("accent_color_hex", "#3A86FF") ?: "#3A86FF",
            newTabWallpaper = prefs.getString("new_tab_wallpaper", "Gradient Teal") ?: "Gradient Teal",
            openLinksInBackground = prefs.getBoolean("open_links_in_background", false),
            autoOpenDownloads = prefs.getBoolean("auto_open_downloads", true),
            closeTabOnBack = prefs.getBoolean("close_tab_on_back", true),
            appLanguage = prefs.getString("app_language", "System") ?: "System"
        )
    }

    private fun saveSettingsToPrefs(s: BrowserSettings) {
        prefs.edit()
            .putString("search_engine", s.searchEngine)
            .putString("custom_search_engine_url", s.customSearchEngineUrl)
            .putString("homepage_type", s.homepageType)
            .putString("homepage_url", s.homepageUrl)
            .putBoolean("ad_blocker_enabled", s.adBlockerEnabled)
            .putBoolean("tracker_blocker_enabled", s.trackerBlockerEnabled)
            .putString("ad_blocker_mode", s.adBlockerMode)
            .putBoolean("popups_blocked", s.popupsBlocked)
            .putBoolean("third_party_cookies_blocked", s.thirdPartyCookiesBlocked)
            .putBoolean("do_not_track", s.doNotTrack)
            .putBoolean("default_desktop_site", s.defaultDesktopSite)
            .putBoolean("javascript_enabled", s.javascriptEnabled)
            .putInt("text_zoom", s.textZoom)
            .putString("theme_mode", s.themeMode)
            .putString("accent_color_hex", s.accentColorHex)
            .putString("new_tab_wallpaper", s.newTabWallpaper)
            .putBoolean("open_links_in_background", s.openLinksInBackground)
            .putBoolean("auto_open_downloads", s.autoOpenDownloads)
            .putBoolean("close_tab_on_back", s.closeTabOnBack)
            .putString("app_language", s.appLanguage)
            .apply()
    }
}
