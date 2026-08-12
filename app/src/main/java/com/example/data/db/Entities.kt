package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null
)

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemDownloadId: Long = -1L,
    val fileName: String,
    val url: String,
    val filePath: String,
    val fileSize: Long = 0,
    val progress: Int = 0, // 0 to 100
    val speed: String = "0 KB/s",
    val status: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    val downloadedAt: Long = System.currentTimeMillis(),
    val mimeType: String = "*/*"
)

@Entity(tableName = "whitelist")
data class WhitelistedDomain(
    @PrimaryKey val domain: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean = false,
    val position: Int = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

data class BlockedRequestLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val domain: String,
    val blockType: String, // "Ad" or "Tracker"
    val timestamp: Long = System.currentTimeMillis()
)

