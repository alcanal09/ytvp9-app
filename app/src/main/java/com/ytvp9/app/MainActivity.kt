package com.ytvp9.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView
    private lateinit var downloadBtn: Button
    private var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "YTVP9 ダウンローダー"
            textSize = 24f
        }

        urlInput = EditText(this).apply {
            hint = "YouTube URLを入力"
        }

        downloadBtn = Button(this).apply {
            text = "ダウンロード"
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(this@MainActivity, "URLを入力してね", Toast.LENGTH_SHORT).show()
                } else {
                    startDownload(url)
                }
            }
        }

        statusText = TextView(this).apply {
            text = "初期化中..."
        }

        layout.addView(title)
        layout.addView(urlInput)
        layout.addView(downloadBtn)
        layout.addView(statusText)
        setContentView(layout)

        // yt-dlpとFFmpegを初期化（時間がかかるので別スレッド）
        thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                initialized = true
                runOnUiThread { statusText.text = "待機中（初期化完了）" }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "初期化エラー: ${e.message}" }
            }
        }

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val uri: Uri? = intent.data
        if (uri != null && uri.scheme == "ytvp9" && uri.host == "download") {
            val youtubeUrl = uri.getQueryParameter("url")
            if (!youtubeUrl.isNullOrEmpty()) {
                urlInput.setText(youtubeUrl)
                statusText.text = "Museからの指示を受信:\n$youtubeUrl"
                Toast.makeText(this, "ダウンロード指示を受信したよ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDownload(url: String) {
        if (!initialized) {
            Toast.makeText(this, "初期化が終わるまで待ってね", Toast.LENGTH_SHORT).show()
            return
        }
        downloadBtn.isEnabled = false
        statusText.text = "ダウンロード開始...\n$url"

        thread {
            try {
                val downloadDir = File(getExternalFilesDir(null), "downloads")
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val request = YoutubeDLRequest(url)
                request.addOption("--no-playlist")
                request.addOption("-f", "bv*[vcodec^=vp09][height<=1440]+ba[acodec^=opus]/bv*[vcodec^=vp09]+ba/best")
                request.addOption("--merge-output-format", "webm")
                request.addOption("-o", "${downloadDir.absolutePath}/%(id)s.%(ext)s")

                YoutubeDL.getInstance().execute(request) { progress, eta, _ ->
                    runOnUiThread {
                        statusText.text = "ダウンロード中: ${progress.toInt()}% (残り約${eta}秒)"
                    }
                }

                runOnUiThread {
                    statusText.text = "ダウンロード完了！\n保存先: ${downloadDir.absolutePath}"
                    downloadBtn.isEnabled = true
                    Toast.makeText(this, "ダウンロード完了", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "エラー: ${e.message}"
                    downloadBtn.isEnabled = true
                    Toast.makeText(this, "ダウンロード失敗", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
