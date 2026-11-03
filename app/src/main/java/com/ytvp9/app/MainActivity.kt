package com.ytvp9.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // yt-dlp初期化
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            // 既に初期化済みの場合は無視
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        urlInput = EditText(this).apply {
            hint = "YouTube URLを入力"
        }

        val downloadBtn = Button(this).apply {
            text = "ダウンロード"
            setOnClickListener { startDownload() }
        }

        statusText = TextView(this).apply {
            text = "待機中"
        }

        layout.addView(urlInput)
        layout.addView(downloadBtn)
        layout.addView(statusText)
        setContentView(layout)
    }

    private fun startDownload() {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "URLを入力してね", Toast.LENGTH_SHORT).show()
            return
        }

        statusText.text = "ダウンロード中..."
        scope.launch(Dispatchers.IO) {
            try {
                val downloadDir = File(getExternalFilesDir(null), "downloads")
                downloadDir.mkdirs()

                val request = YoutubeDLRequest(url)
                request.addOption("-f", "best[height<=1080]")
                request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                YoutubeDL.getInstance().execute(request) { progress, _, _ ->
                    runOnUiThread {
                        statusText.text = "ダウンロード中... $progress%"
                    }
                }

                withContext(Dispatchers.Main) {
                    statusText.text = "ダウンロード完了！"
                    Toast.makeText(this@MainActivity, "完了", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "エラー: ${e.message}"
                }
            }
        }
    }
}
