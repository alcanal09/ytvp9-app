package com.ytvp9.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Museからのダウンロード指示をFCM経由で受け取る。
 *
 * 期待するデータペイロード:
 *   action = "download" (必須)
 *   url    = YouTube動画URL (必須)
 *   title  = 任意のメモ
 *
 * 受信したらダウンロードキューに追加し、DownloadServiceに処理を委ねる。
 * ブロック冷却期間中はキューに入れず、受信を破棄してログに残す。
 */
class MusePushService : FirebaseMessagingService() {

    override fun onMessageReceived(msg: RemoteMessage) {
        val data = msg.data
        Log.i(TAG, "FCM受信: $data")
        if (data["action"] != "download") {
            Log.w(TAG, "未知のアクション: ${data["action"]}")
            return
        }
        val url = data["url"] ?: run {
            Log.w(TAG, "URLなしのため破棄")
            return
        }
        val videoId = extractVideoId(url) ?: run {
            Log.w(TAG, "動画IDを抽出できず破棄: $url")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = App.db(this@MusePushService)
            // 冷却期間中は受け付けない
            val block = db.blockStateDao().get()
            if (block != null && System.currentTimeMillis() < block.cooldownUntil) {
                Log.i(TAG, "冷却期間中のためキュー追加をスキップ: $videoId")
                return@launch
            }
            // 重複チェック
            if (db.videoDao().getById(videoId) != null) {
                Log.i(TAG, "ダウンロード済みのためスキップ: $videoId")
                return@launch
            }
            if (db.queueDao().countActiveForVideo(videoId) > 0) {
                Log.i(TAG, "キュー済みのためスキップ: $videoId")
                return@launch
            }
            db.queueDao().insert(
                QueueEntity(
                    videoId = videoId,
                    url = url,
                    title = data["title"],
                    status = "QUEUED",
                    source = "muse",
                )
            )
            Log.i(TAG, "Museからの指示をキューに追加: $videoId")
            DownloadService.enqueueWork(this@MusePushService)
        }
    }

    override fun onNewToken(token: String) {
        // トークンはユーザーに提示してMuse側に登録してもらう想定。
        // v1では設定画面に表示する。
        Log.i(TAG, "FCMトークン更新")
        getSharedPreferences("fcm", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("[?&]v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("/shorts/([a-zA-Z0-9_-]{11})"),
        )
        for (p in patterns) {
            p.find(url)?.let { return it.groupValues[1] }
        }
        return null
    }

    companion object {
        private const val TAG = "MusePushService"
    }
}
