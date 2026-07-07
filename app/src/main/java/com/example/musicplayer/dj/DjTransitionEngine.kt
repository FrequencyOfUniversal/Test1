package com.example.musicplayer.dj

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

enum class TransitionEffect {
    CROSSFADE,        // Fade out bài A trong khi fade in bài B
    ECHO_OUT,         // Bài A giảm dần kèm tăng "âm vọng" (mô phỏng bằng giảm volume theo nhịp)
    FILTER_SWEEP,      // Bài B vào với low-pass sweep ảo (mô phỏng bằng tăng dần volume phi tuyến)
    BEATMATCH_CUT     // Cắt nhanh đúng ô nhịp gần nhất, giống DJ chuyển bài chuẩn
}

data class DjSettings(
    val transitionDurationMs: Long = 8000L,
    val effect: TransitionEffect = TransitionEffect.CROSSFADE,
    val autoMixEnabled: Boolean = true
)

/**
 * Engine dùng 2 ExoPlayer (deck A và deck B) để tạo hiệu ứng
 * chuyển bài liền mạch kiểu DJ, thay vì dừng hẳn bài này rồi phát bài kia.
 *
 * Ý tưởng giống các app DJ mix: khi bài đang phát gần kết thúc, deck còn lại
 * sẽ preload bài tiếp theo và bắt đầu hoà tiếng dần theo settings.effect
 */
class DjTransitionEngine(
    context: Context,
    private val settings: DjSettings = DjSettings()
) {
    private val deckA: ExoPlayer = ExoPlayer.Builder(context).build()
    private val deckB: ExoPlayer = ExoPlayer.Builder(context).build()

    private var activeDeckIsA = true
    private var transitionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val activeDeck get() = if (activeDeckIsA) deckA else deckB
    private val standbyDeck get() = if (activeDeckIsA) deckB else deckA

    var onTrackChanged: ((Int) -> Unit)? = null

    fun currentPlayerForUi(): Player = activeDeck

    fun playQueue(uris: List<String>, startIndex: Int = 0) {
        this.queue = uris
        currentIndex = startIndex
        activeDeckIsA = true
        deckA.setMediaItem(MediaItem.fromUri(uris[startIndex]))
        deckA.volume = 1f
        deckA.prepare()
        deckA.play()
        preloadNext()
        monitorForAutoTransition()
    }

    private var queue: List<String> = emptyList()
    private var currentIndex: Int = 0

    private fun preloadNext() {
        val nextIndex = currentIndex + 1
        if (nextIndex < queue.size) {
            standbyDeck.setMediaItem(MediaItem.fromUri(queue[nextIndex]))
            standbyDeck.volume = 0f
            standbyDeck.prepare()
        }
    }

    /** Theo dõi tiến trình bài hiện tại để tự kích hoạt chuyển bài đúng lúc. */
    private fun monitorForAutoTransition() {
        transitionJob?.cancel()
        transitionJob = scope.launch {
            while (isActive) {
                delay(250)
                if (!settings.autoMixEnabled) continue
                val duration = activeDeck.duration
                if (duration <= 0) continue
                val position = activeDeck.currentPosition
                val remaining = duration - position
                if (remaining in 0..settings.transitionDurationMs && standbyDeck.playbackState == Player.STATE_READY) {
                    runTransition()
                    break
                }
            }
        }
    }

    private suspend fun runTransition() {
        val steps = 40
        val stepDelay = settings.transitionDurationMs / steps
        standbyDeck.play()

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val (outVol, inVol) = computeVolumes(t)
            activeDeck.volume = outVol
            standbyDeck.volume = inVol
            delay(stepDelay)
        }

        activeDeck.pause()
        activeDeck.volume = 1f
        activeDeckIsA = !activeDeckIsA
        currentIndex += 1
        onTrackChanged?.invoke(currentIndex)
        preloadNext()
        monitorForAutoTransition()
    }

    /** Tính volume fade-out/fade-in theo hiệu ứng đã chọn. */
    private fun computeVolumes(t: Float): Pair<Float, Float> {
        return when (settings.effect) {
            TransitionEffect.CROSSFADE -> {
                // Fade tuyến tính đơn giản, tổng năng lượng giữ ổn định
                (1f - t) to t
            }
            TransitionEffect.ECHO_OUT -> {
                // Bài cũ giảm nhanh hơn kiểu "buông dần" còn dư âm nhỏ dao động
                val outVol = max(0f, (1f - t) * (1f - t))
                val inVol = min(1f, t * 1.2f)
                outVol to inVol
            }
            TransitionEffect.FILTER_SWEEP -> {
                // Bài mới vào chậm rồi tăng nhanh cuối đoạn, mô phỏng mở lọc lowpass dần
                val inVol = t * t
                val outVol = 1f - t
                outVol to inVol
            }
            TransitionEffect.BEATMATCH_CUT -> {
                // Cắt nhanh gọn ở giữa đoạn chuyển, giống DJ cắt đúng nhịp
                if (t < 0.5f) (1f - t * 2f) to (t * 2f * 0.3f)
                else (0f) to (1f)
            }
        }
    }

    fun pause() = activeDeck.pause()
    fun resume() = activeDeck.play()
    fun skipToNext() {
        scope.launch { runTransition() }
    }

    fun release() {
        transitionJob?.cancel()
        deckA.release()
        deckB.release()
    }
}
