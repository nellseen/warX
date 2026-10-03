package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadState {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val originalUrl: String,
    val mediaUrl: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val quality: String,
    val format: String,
    val filePath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val state: DownloadState = DownloadState.PENDING,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val isHls: Boolean = false,
    val totalSegments: Int = 0,
    val downloadedSegments: Int = 0,
    val durationSeconds: Double = 0.0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val progressPercent: Float
        get() = when {
            isHls && totalSegments > 0 -> (downloadedSegments.toFloat() / totalSegments.toFloat()) * 100f
            totalBytes > 0 -> (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100f
            else -> 0f
        }
}
