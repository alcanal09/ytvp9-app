package com.ytvp9.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

/**
 * ダウンロード実行サービス（フォアグラウンド）。
 *
 * ブロック回避の最上位原則に従う:
 * - 同時ダウンロードは1本のみ
 * - 動画間にクールダウン間隔（設定可能、既定3分）
 * - 403/bot確認/PO Token要求を検知したら即時停止し、冷却期間に入る
 * - 冷却期間中は新規取得を開始しない（手動再開のみ）
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("待機中"))
        if (!running) {
            running = true
            scope.launch { processQueue() }
        }
        return START_STICKY
    }

    private suspend fun processQueue() {
        val db = App.db(this)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        while (true) {
            // 冷却期間チェック
            val block = db.blockStateDao().get()
            if (block != null && System.currentTimeMillis() < block.cooldownUntil) {
                Log.i(TAG, "冷却期間中のため停止")
                updateNotification("冷却期間中")
                break
            }

            val queue = db.queueDao().getActive()
                .filter { it.status == "QUEUED" }
            if (queue.isEmpty()) break

            val item = queue.first()
            db.queueDao().updateProgress(item.id, "DOWNLOADING", 0)
            updateNotification("ダウンロード中: ${item.title ?: item.videoId}")

            val result = downloadOne(item)

            when (result) {
                is DlResult.Ok -> {
                    db.queueDao().updateProgress(item.id, "DONE", 100)
                    // TODO: 結合してライブラリに登録
                }
                is DlResult.Blocked -> {
                    // ブロック検知: 即時停止＋冷却期間
                    val cooldownHours = prefs.getInt("cooldown_hours", 6)
                    val until = System.currentTimeMillis() + cooldownHours * 3600_000L
                    db.blockStateDao().set(
                        BlockStateEntity(
                            detectedAt = System.currentTimeMillis(),
                            errorKind = result.kind,
                            playerClient = prefs.getString("player_client", "android") ?: "android",
                            cooldownUntil = until,
                        )
                    )
                    db.queueDao().markFailed(item.id, "FAILED", result.kind, result.detail)
                    Log.w(TAG, "ブロック検知 (${result.kind})、冷却期間に入る")
                    // 通知で利用者に知らせる
                    break
                }
                is DlResult.Error -> {
                    db.queueDao().markFailed(item.id, "FAILED", result.kind, result.detail)
                }
            }

            // クールダウン間隔
            val cooldownMin = prefs.getInt("cooldown_minutes", 3)
            delay(cooldownMin * 60_000L)
        }

        running = false
        stopSelf()
    }

    /**
     * 1本をダウンロードする。yt-dlp呼び出しはここに集約。
     * TODO: youtubedl-android AARを組み込んで実装する。
     */
    private suspend fun downloadOne(item: QueueEntity): DlResult {
        // プレースホルダー: 実装時にyt-dlpを呼び出す
        // -f "bv*[vcodec^=vp09]+ba" --merge-output-format webm
        // 403/bot確認/PO Token要求を検知したら DlResult.Blocked を返す
        return DlResult.Error("not_implemented", "yt-dlp組み込み待ち")
    }

    private fun buildNotification(text: String): Notification {
        val chId = "download"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(chId, "ダウンロード", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("YTVP9")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    sealed class DlResult {
        data object Ok : DlResult()
        data class Blocked(val kind: String, val detail: String) : DlResult()
        data class Error(val kind: String, val detail: String) : DlResult()
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val NOTIF_ID = 1

        fun enqueueWork(context: Context) {
            context.startForegroundService(Intent(context, DownloadService::class.java))
        }
    }
}
