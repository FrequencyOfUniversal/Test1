package com.example.musicplayer.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.widget.MusicWidgetProvider

/**
 * Service chạy nền: giữ ExoPlayer sống khi tắt màn hình / thoát app.
 * Media3 tự động sinh ra notification điều khiển (play/pause/next/prev)
 * và tự động đồng bộ với màn hình khoá, chỉ cần MediaSession được cấu hình đúng.
 */
class PlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_TOGGLE_PLAY = "com.example.musicplayer.action.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.musicplayer.action.NEXT"
        const val ACTION_PREV = "com.example.musicplayer.action.PREV"
    }

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    var equalizerManager: EqualizerManager? = null
        private set

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .build()

        // Gắn Equalizer vào đúng audio session của ExoPlayer để chỉnh EQ thật.
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                equalizerManager?.release()
                equalizerManager = EqualizerManager(audioSessionId)
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()

        player.addListener(object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                val item = p.currentMediaItem
                val title = item?.mediaMetadata?.title?.toString() ?: "Chưa phát nhạc"
                val artist = item?.mediaMetadata?.artist?.toString() ?: ""
                MusicWidgetProvider.updateWidgets(this@PlaybackService, title, artist, p.isPlaying)
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAY -> if (player.isPlaying) player.pause() else player.play()
            ACTION_NEXT -> player.seekToNextMediaItem()
            ACTION_PREV -> player.seekToPreviousMediaItem()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        equalizerManager?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
