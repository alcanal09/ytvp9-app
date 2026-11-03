package com.ytvp9.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*

/**
 * 動画再生画面 (Media3/ExoPlayer)。
 * シーク、レジューム再生、再生速度、字幕表示に対応。
 */
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var videoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoId = intent.getStringExtra("video_id") ?: run { finish(); return }

        val playerView = PlayerView(this)
        setContentView(playerView)

        player = ExoPlayer.Builder(this).build().also {
            playerView.player = it
        }

        scope.launch(Dispatchers.IO) {
            val video = App.db(this@PlayerActivity).videoDao().getById(videoId!!)
            withContext(Dispatchers.Main) {
                if (video == null) { finish(); return@withContext }
                player?.setMediaItem(MediaItem.fromUri(video.filePath))
                player?.seekTo(video.resumePositionMs)
                player?.prepare()
                player?.play()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // レジューム位置を保存
        val pos = player?.currentPosition ?: 0
        val vid = videoId ?: return
        CoroutineScope(Dispatchers.IO).launch {
            App.db(this@PlayerActivity).videoDao()
                .updateResume(vid, pos, System.currentTimeMillis())
        }
        player?.pause()
    }

    override fun onDestroy() {
        scope.cancel()
        player?.release()
        player = null
        super.onDestroy()
    }
}
