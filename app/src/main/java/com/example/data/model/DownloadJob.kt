package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class JobStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    DONE,
    ERROR,
    CANCELLED
}

@Entity(tableName = "download_jobs")
data class DownloadJob(
    @PrimaryKey val id: String,
    val videoId: String,
    val title: String,
    val thumbnail: String = "",
    val label: String = "",
    val kind: String = "video", // "video" or "audio"
    val audioFormat: String = "m4a", // "m4a", "mp3", "opus"
    val videoFormatId: String = "",
    val audioFormatId: String = "",
    val height: Int = 0,
    val container: String = "auto", // "auto", "mp4", "mkv"
    val status: JobStatus = JobStatus.QUEUED,
    val stage: String = "Queued",
    val progress: Float = 0f,
    val speed: Long = 0L,
    val eta: Long = 0L,
    val filename: String = "",
    val filePath: String = "",
    val error: String? = null,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
