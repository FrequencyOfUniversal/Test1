package com.example.musicplayer.data

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumArtUri: String? = null,
    // BPM ước lượng dùng cho tính năng auto DJ-mix (crossfade đúng nhịp).
    // Có thể để null nếu chưa phân tích, engine sẽ dùng crossfade mặc định.
    val estimatedBpm: Float? = null
)
