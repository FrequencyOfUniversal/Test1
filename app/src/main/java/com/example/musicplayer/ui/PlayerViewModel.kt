package com.example.musicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.data.Song
import com.example.musicplayer.player.MusicController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private var controller: MusicController? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    init {
        controller = MusicController(application) { onControllerReady() }
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                controller?.let { c ->
                    _uiState.value = _uiState.value.copy(
                        positionMs = c.currentPosition(),
                        durationMs = c.duration()
                    )
                }
            }
        }
    }

    private fun onControllerReady() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                controller?.let { c ->
                    _uiState.value = _uiState.value.copy(currentIndex = c.currentIndex())
                }
            }
        })
    }

    fun loadLibrary() {
        viewModelScope.launch {
            val songs = repository.loadAllSongs()
            _uiState.value = _uiState.value.copy(songs = songs)
        }
    }

    fun playFrom(index: Int) {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        controller?.setPlaylist(songs, index)
        _uiState.value = _uiState.value.copy(currentIndex = index, isPlaying = true)
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) controller?.pause() else controller?.play()
    }

    fun skipNext() = controller?.skipNext()
    fun skipPrevious() = controller?.skipPrevious()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
    }

    fun toggleShuffle() {
        val newValue = !_uiState.value.shuffleEnabled
        controller?.setShuffle(newValue)
        _uiState.value = _uiState.value.copy(shuffleEnabled = newValue)
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.setRepeatMode(next)
        _uiState.value = _uiState.value.copy(repeatMode = next)
    }

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
