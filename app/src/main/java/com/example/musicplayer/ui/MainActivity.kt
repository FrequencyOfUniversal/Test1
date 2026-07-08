package com.example.musicplayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.example.musicplayer.data.Song

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MusicApp()
            }
        }
    }
}

@Composable
fun MusicApp(viewModel: PlayerViewModel = viewModel()) {
    val permission = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.loadLibrary() }

    LaunchedEffect(Unit) { launcher.launch(permission) }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { PlayerBar(state, viewModel) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SongList(state.songs, state.currentIndex) { index ->
                viewModel.playFrom(index)
            }
        }
    }
}

@Composable
fun SongList(songs: List<Song>, currentIndex: Int, onSongClick: (Int) -> Unit) {
    LazyColumn {
        itemsIndexed(songs) { index, song ->
            ListItem(
                headlineContent = { Text(song.title) },
                supportingContent = { Text("${song.artist} • ${song.album}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(index) },
                tonalElevation = if (index == currentIndex) 4.dp else 0.dp
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun PlayerBar(state: PlayerUiState, viewModel: PlayerViewModel) {
    if (state.songs.isEmpty()) return
    val current = state.songs.getOrNull(state.currentIndex) ?: return

    Column {
        // Thanh tua bài hát
        Slider(
            value = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..(state.durationMs.toFloat().coerceAtLeast(1f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        BottomAppBar {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(current.title, maxLines = 1)
                    Text(current.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }

                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                IconButton(onClick = { viewModel.skipPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
                IconButton(onClick = { viewModel.skipNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }
        }
    }
}
