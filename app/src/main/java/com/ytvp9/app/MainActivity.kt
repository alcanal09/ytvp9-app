package com.ytvp9.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView
    private lateinit var fileListText: TextView
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

        val listTitle = TextView(this).apply {
            text = "── ダウンロード済み ──"
            textSize = 18f
            setPadding(0, 32, 0, 0)
        }

        fileListText = TextView(this).apply {
            text = "（なし）"
        }

        layout.addView(title)
        layout.addView(urlInput)
        layout.addView(downloadBtn)
        layout.addView(statusText)
        layout.addView(listTitle)
        layout.addView(fileListText)
        setContentView(layout)

        refreshFileList()

        thread {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                initialized = true
                runOnUiThread { statusText.text = "待機中（初期化完了）" }
            } catch (e: Exception) {
                Log.e("YTVP9", "YoutubeDL初期化失敗", e)
                val detail = e.cause?.message ?: e.message ?: e.toString()
                runOnUiThread {
                    statusText.text = "初期化エラー: ${e.javaClass.simpleName}\n詳細: $detail"
                }
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

    private fun
