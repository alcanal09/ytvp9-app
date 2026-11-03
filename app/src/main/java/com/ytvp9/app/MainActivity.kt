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

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView

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

        val downloadBtn = Button(this).apply {
            text = "ダウンロード"
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(this@MainActivity, "URLを入力してね", Toast.LENGTH_SHORT).show()
                } else {
                    statusText.text = "URL受付: $url\n（ダウンロード機能は準備中）"
                }
            }
        }

        statusText = TextView(this).apply {
            text = "待機中"
        }

        layout.addView(title)
        layout.addView(urlInput)
        layout.addView(downloadBtn)
        layout.addView(statusText)
        setContentView(layout)

        // ディープリンクからの起動を処理
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
}
