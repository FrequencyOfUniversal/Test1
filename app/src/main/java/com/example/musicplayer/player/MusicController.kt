package com.example.musicplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.Song
import com.google.common.util.concurrent.MoreExecutors

/**
 * Lớp trung gian giữa UI (Compose) và MediaSessionService.
 * UI không cần biết chi tiết ExoPlayer/MediaSession, chỉ gọi các hàm ở đây.
 */
class MusicController(context: Context, private val onReady: (MediaController) -> Unit) {

    private var controller: MediaController? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.let(onReady)
        }, MoreExecutors.directExecutor())
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int) {
        val items = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uriString)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        controller?.setMediaItems(items, startIndex, 0L)
        controller?.prepare()
        controller?.play()
    }

    fun play() = controller?.play()
    fun pause() = controller?.pause()
    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrevious() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun currentPosition(): Long = controller?.currentPosition ?: 0L
    fun duration(): Long = controller?.duration?.takeIf { it > 0 } ?: 0L
    fun isPlaying(): Boolean = controller?.isPlaying == true
    fun currentIndex(): Int = controller?.currentMediaItemIndex ?: 0

    fun addListener(listener: Player.Listener) {
        controller?.addListener(listener)
    }

    fun release() {
        controller?.release()
        controller = null
    }
}
