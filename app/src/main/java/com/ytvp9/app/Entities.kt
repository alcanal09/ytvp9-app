package com.ytvp9.app

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ダウンロード済み動画のメタデータ。
 * 実ファイルは結合済みWebMを1つ、字幕はDB側で紐づけて管理する。
 */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailPath: String?,
    val durationSec: Long,
    val width: Int,
    val height: Int,
    val vcodec: String,
    val acodec: String,
    val isHdr: Boolean,
    val filePath: String,
    val fileSize: Long,
    val downloadedAt: Long,
    // 再生状態
    val resumePositionMs: Long = 0,
    val lastPlayedAt: Long = 0,
    val isWatched: Boolean = false,
    // 字幕
    val subtitleLang: String? = null,
    val subtitlePath: String? = null,
    val subtitleIsAuto: Boolean = false,
)

/**
 * ダウンロードキューの1件。
 */
@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val url: String,
    val title: String?,
    val status: String, // QUEUED, DOWNLOADING, PAUSED, FAILED, DONE
    val progress: Int = 0,
    val errorKind: String? = null, // 7章のエラー区分
    val errorDetail: String? = null,
    val source: String = "manual", // manual | share | muse
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * ブロック検知状態。
 */
@Entity(tableName = "block_state")
data class BlockStateEntity(
    @PrimaryKey val id: Int = 1,
    val detectedAt: Long,
    val errorKind: String,
    val playerClient: String,
    val cooldownUntil: Long,
)
