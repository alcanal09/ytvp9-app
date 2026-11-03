package com.ytvp9.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "YTVP9 - ビルドテスト成功！"
            textSize = 24f
        }
        setContentView(textView)
    }
}
