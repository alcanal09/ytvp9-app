package com.ytvp9.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val resolution: String,
    val videoCodec: String,
    val audioCodec: String,
    val isHdr: Boolean,
    val filePath: String,
    val fileSize: Long,
    val downloadedAt: Long,
    val resumePosition: Long = 0,
    val lastPlayedAt: Long = 0,
    val isWatched: Boolean = false
)
