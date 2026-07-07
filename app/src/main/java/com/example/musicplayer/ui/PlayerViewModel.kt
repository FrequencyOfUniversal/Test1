package com.example.musicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.data.Song
import com.example.musicplayer.dj.DjSettings
import com.example.musicplayer.dj.DjTransitionEngine
import com.example.musicplayer.dj.TransitionEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val autoMixEnabled: Boolean = true,
    val transitionEffect: TransitionEffect = TransitionEffect.CROSSFADE,
    val transitionDurationSec: Int = 8
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private var engine: DjTransitionEngine? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    fun loadLibrary() {
        viewModelScope.launch {
            val songs = repository.loadAllSongs()
            _uiState.value = _uiState.value.copy(songs = songs)
        }
    }

    fun playFrom(index: Int) {
        val state = _uiState.value
        if (state.songs.isEmpty()) return

        engine?.release()
        val settings = DjSettings(
            transitionDurationMs = state.transitionDurationSec * 1000L,
            effect = state.transitionEffect,
            autoMixEnabled = state.autoMixEnabled
        )
        val newEngine = DjTransitionEngine(getApplication(), settings)
        newEngine.onTrackChanged = { newIndex ->
            _uiState.value = _uiState.value.copy(currentIndex = newIndex)
        }
        newEngine.playQueue(state.songs.map { it.uriString }, index)
        engine = newEngine
        _uiState.value = _uiState.value.copy(currentIndex = index, isPlaying = true)
    }

    fun togglePlayPause() {
        val playing = _uiState.value.isPlaying
        if (playing) engine?.pause() else engine?.resume()
        _uiState.value = _uiState.value.copy(isPlaying = !playing)
    }

    fun skipNext() {
        engine?.skipToNext()
    }

    fun setAutoMix(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoMixEnabled = enabled)
    }

    fun setTransitionEffect(effect: TransitionEffect) {
        _uiState.value = _uiState.value.copy(transitionEffect = effect)
    }

    fun setTransitionDuration(seconds: Int) {
        _uiState.value = _uiState.value.copy(transitionDurationSec = seconds)
    }

    override fun onCleared() {
        engine?.release()
        super.onCleared()
    }
}
