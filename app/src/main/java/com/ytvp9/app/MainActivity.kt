package com.ytvp9.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

/**
 * ホーム／ライブラリ画面。
 * URL入力欄、ダウンロード済み動画の一覧、検索、合計容量表示。
 */
class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val urlInput = EditText(this).apply { hint = "YouTube URLを貼り付け" }
        val list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@MainActivity) }
        val status = TextView(this)
        root.addView(urlInput)
        root.addView(list)
        root.addView(status)
        setContentView(root)

        // 共有メニューからのURL受け取り
        if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { shared ->
                val id = extractVideoId(shared)
                if (id != null) enqueueDownload("https://www.youtube.com/watch?v=$id", "share")
            }
        }

        refreshLibrary(list, status)
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("[?&]v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("/shorts/([a-zA-Z0-9_-]{11})"),
        )
        for (p in patterns) { p.find(url)?.let { return it.groupValues[1] } }
        return null
    }

    private fun enqueueDownload(url: String, source: String) {
        scope.launch(Dispatchers.IO) {
            val db = App.db(this@MainActivity)
            val id = extractVideoId(url) ?: return@launch
            if (db.videoDao().getById(id) == null && db.queueDao().countActiveForVideo(id) == 0) {
                db.queueDao().insert(QueueEntity(videoId = id, url = url, title = null, status = "QUEUED", source = source))
                withContext(Dispatchers.Main) { DownloadService.enqueueWork(this@MainActivity) }
            }
        }
    }

    private fun refreshLibrary(list: RecyclerView, status: TextView) {
        scope.launch(Dispatchers.IO) {
            val db = App.db(this@MainActivity)
            val videos = db.videoDao().getAll()
            val total = db.videoDao().totalSize() ?: 0
            withContext(Dispatchers.Main) {
                status.text = "${videos.size}本 / ${total / 1024 / 1024}MB"
                // TODO: RecyclerViewアダプタを設定
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
