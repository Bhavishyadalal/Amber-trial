package com.example.data.model

data class VideoFormat(
    val id: String,
    val height: Int,
    val width: Int,
    val fps: Int,
    val codec: String, // "H.264", "VP9", "AV1"
    val ext: String,
    val tbr: Double = 0.0,
    val hdr: String = "",
    val hasAudio: Boolean = false,
    val size: Long = 0L,
    val approx: Boolean = true
)

data class AudioFormat(
    val id: String,
    val ext: String,
    val codec: String,
    val abr: Int = 0,
    val lang: String = "",
    val size: Long = 0L,
    val approx: Boolean = true
)

data class VideoDetails(
    val id: String,
    val title: String,
    val channel: String,
    val channelId: String = "",
    val duration: String,
    val seconds: Long,
    val thumbnail: String,
    val videos: List<VideoFormat> = emptyList(),
    val audios: List<AudioFormat> = emptyList(),
    val isLive: Boolean = false,
    val ffmpegAvailable: Boolean = true
)

data class PlaylistItem(
    val id: String,
    val title: String,
    val channel: String,
    val duration: String,
    val thumbnail: String,
    val isLive: Boolean = false
)

data class PlaylistDetails(
    val id: String,
    val title: String,
    val channel: String = "",
    val count: Int,
    val items: List<PlaylistItem>
)
