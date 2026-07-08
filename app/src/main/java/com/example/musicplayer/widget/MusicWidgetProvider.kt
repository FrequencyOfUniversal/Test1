package com.example.musicplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.musicplayer.R
import com.example.musicplayer.player.PlaybackService

/**
 * Widget màn hình chính hiển thị bài đang phát và nút prev/play/next.
 * Widget gửi broadcast action tới chính nó; MusicWidgetProvider nhận action đó
 * và chuyển thành lệnh điều khiển cho service thông qua controller dùng chung.
 */
class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.musicplayer.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.musicplayer.widget.NEXT"
        const val ACTION_PREV = "com.example.musicplayer.widget.PREV"

        /** Gọi hàm này từ PlaybackService/ViewModel mỗi khi trạng thái bài hát đổi. */
        fun updateWidgets(context: Context, title: String, artist: String, isPlaying: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
            for (id in ids) {
                val views = buildViews(context, title, artist, isPlaying)
                manager.updateAppWidget(id, views)
            }
        }

        private fun buildViews(context: Context, title: String, artist: String, isPlaying: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_song_artist, artist)
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, actionPendingIntent(context, ACTION_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_btn_next, actionPendingIntent(context, ACTION_NEXT))
            views.setOnClickPendingIntent(R.id.widget_btn_prev, actionPendingIntent(context, ACTION_PREV))
            return views
        }

        private fun actionPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context, action.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val serviceIntent = Intent(context, PlaybackService::class.java)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> serviceIntent.action = PlaybackService.ACTION_TOGGLE_PLAY
            ACTION_NEXT -> serviceIntent.action = PlaybackService.ACTION_NEXT
            ACTION_PREV -> serviceIntent.action = PlaybackService.ACTION_PREV
            else -> return
        }
        context.startService(serviceIntent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, "Chưa phát nhạc", "", false))
        }
    }
}
