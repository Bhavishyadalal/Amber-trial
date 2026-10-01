package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TrackListType {
    LIKED,
    RECENT,
    QUEUE
}

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val seconds: Long = 0L,
    val thumbnail: String,
    val channelId: String = "",
    val isLive: Boolean = false,
    val localPath: String? = null,
    val listType: TrackListType,
    val timestamp: Long = System.currentTimeMillis(),
    val queueOrder: Int = 0
)

data class Track(
    val id: String,
    val title: String,
    val artist: String = "",
    val channelId: String = "",
    val duration: String = "",
    val seconds: Long = 0L,
    val thumbnail: String = "",
    val isLive: Boolean = false,
    val localPath: String? = null
) {
    fun toEntity(type: TrackListType, order: Int = 0): TrackEntity = TrackEntity(
        id = id,
        title = title,
        artist = artist,
        channelId = channelId,
        duration = duration,
        seconds = seconds,
        thumbnail = thumbnail,
        isLive = isLive,
        localPath = localPath,
        listType = type,
        timestamp = System.currentTimeMillis(),
        queueOrder = order
    )

    companion object {
        fun fromEntity(entity: TrackEntity): Track = Track(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            channelId = entity.channelId,
            duration = entity.duration,
            seconds = entity.seconds,
            thumbnail = entity.thumbnail,
            isLive = entity.isLive,
            localPath = entity.localPath
        )
    }
}
