package com.example.musicplayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.data.Song
import com.example.musicplayer.dj.TransitionEffect

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
        bottomBar = { MiniPlayerBar(state, viewModel) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            DjMixControls(state, viewModel)
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
fun MiniPlayerBar(state: PlayerUiState, viewModel: PlayerViewModel) {
    if (state.songs.isEmpty()) return
    val current = state.songs.getOrNull(state.currentIndex) ?: return

    BottomAppBar {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(current.title, maxLines = 1)
                Text(current.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
        }
    }
}

@Composable
fun DjMixControls(state: PlayerUiState, viewModel: PlayerViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tự động Mix DJ", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = state.autoMixEnabled,
                    onCheckedChange = { viewModel.setAutoMix(it) }
                )
            }
            if (state.autoMixEnabled) {
                Spacer(Modifier.height(8.dp))
                Text("Hiệu ứng chuyển bài", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransitionEffect.entries.forEach { effect ->
                        FilterChip(
                            selected = state.transitionEffect == effect,
                            onClick = { viewModel.setTransitionEffect(effect) },
                            label = { Text(effectLabel(effect)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Thời gian hoà: ${state.transitionDurationSec}s")
                Slider(
                    value = state.transitionDurationSec.toFloat(),
                    onValueChange = { viewModel.setTransitionDuration(it.toInt()) },
                    valueRange = 2f..15f,
                    steps = 12
                )
            }
        }
    }
}

fun effectLabel(effect: TransitionEffect): String = when (effect) {
    TransitionEffect.CROSSFADE -> "Crossfade"
    TransitionEffect.ECHO_OUT -> "Echo Out"
    TransitionEffect.FILTER_SWEEP -> "Filter Sweep"
    TransitionEffect.BEATMATCH_CUT -> "Beatmatch Cut"
}
