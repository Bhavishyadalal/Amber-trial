package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.example.data.local.AmberDatabase
import com.example.data.local.AmberPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DownloadJob
import com.example.data.model.JobStatus
import com.example.data.model.Track
import com.example.data.model.TrackEntity
import com.example.data.model.TrackListType
import com.example.engine.AmberEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

data class SavedVideoFile(
    val filename: String,
    val title: String,
    val videoId: String,
    val quality: String,
    val ext: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val filePath: String,
    val thumbnail: String
)

class AmberRepository(
    private val context: Context,
    private val database: AmberDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val trackDao = database.trackDao()
    private val jobDao = database.downloadJobDao()

    val preferences: Flow<AmberPreferences> = preferencesRepository.getPreferences()

    val likedTracks: Flow<List<Track>> = trackDao.getTracksByType(TrackListType.LIKED)
        .map { entities -> entities.map { Track.fromEntity(it) } }

    val recentTracks: Flow<List<Track>> = trackDao.getTracksByType(TrackListType.RECENT)
        .map { entities -> entities.map { Track.fromEntity(it) } }

    val queueTracks: Flow<List<Track>> = trackDao.getQueueTracks()
        .map { entities -> entities.map { Track.fromEntity(it) } }

    val allDownloadJobs: Flow<List<DownloadJob>> = jobDao.getAllJobs()
    val activeJobCount: Flow<Int> = jobDao.getActiveJobCount()

    fun isLiked(trackId: String): Flow<Boolean> = trackDao.isLiked(trackId)

    suspend fun toggleLiked(track: Track) = withContext(Dispatchers.IO) {
        val existing = database.openHelper.readableDatabase.query(
            "SELECT 1 FROM tracks WHERE id = ? AND listType = 'LIKED'",
            arrayOf(track.id)
        )
        val hasLiked = existing.moveToFirst()
        existing.close()

        if (hasLiked) {
            trackDao.deleteTrack(track.id, TrackListType.LIKED)
        } else {
            trackDao.insertTrack(track.toEntity(TrackListType.LIKED))
        }
    }

    suspend fun addToRecent(track: Track) = withContext(Dispatchers.IO) {
        trackDao.insertTrack(track.toEntity(TrackListType.RECENT))
    }

    suspend fun clearRecent() = withContext(Dispatchers.IO) {
        trackDao.clearTracksByType(TrackListType.RECENT)
    }

    suspend fun updateQueue(tracks: List<Track>) = withContext(Dispatchers.IO) {
        val entities = tracks.mapIndexed { index, track ->
            track.toEntity(TrackListType.QUEUE, order = index)
        }
        trackDao.replaceQueue(entities)
    }

    suspend fun clearQueue() = withContext(Dispatchers.IO) {
        trackDao.clearTracksByType(TrackListType.QUEUE)
    }

    suspend fun addDownloadJob(job: DownloadJob) = withContext(Dispatchers.IO) {
        jobDao.insertJob(job)
    }

    suspend fun updateDownloadJob(job: DownloadJob) = withContext(Dispatchers.IO) {
        jobDao.updateJob(job)
    }

    suspend fun deleteDownloadJob(id: String) = withContext(Dispatchers.IO) {
        jobDao.deleteJob(id)
    }

    suspend fun clearFinishedJobs() = withContext(Dispatchers.IO) {
        jobDao.clearFinishedJobs()
    }

    suspend fun scanDownloadedAudio(): List<Track> = withContext(Dispatchers.IO) {
        val musicDir = AmberEngine.getDownloadOutputDir(context, isAudio = true)
        val files = musicDir.listFiles { _, name ->
            name.contains("[") && name.contains("]") && (name.endsWith(".m4a") || name.endsWith(".mp3") || name.endsWith(".opus") || name.endsWith(".webm"))
        } ?: return@withContext emptyList()

        val idPattern = Pattern.compile("\\[([A-Za-z0-9_-]{11})\\]")
        val tracks = mutableListOf<Track>()

        for (f in files.sortedByDescending { it.lastModified() }) {
            val matcher = idPattern.matcher(f.name)
            val vid = if (matcher.find()) matcher.group(1) else f.nameWithoutExtension
            val title = f.name.substringBefore("[").trim().ifEmpty { f.nameWithoutExtension }
            val thumb = "https://i.ytimg.com/vi/$vid/mqdefault.jpg"

            var durationStr = ""
            var durationSec = 0L
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(f.absolutePath)
                val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                durationSec = durMs / 1000L
                durationStr = AmberEngine.formatDuration(durationSec)
                mmr.release()
            } catch (_: Exception) {}

            tracks.add(
                Track(
                    id = vid,
                    title = title,
                    artist = "Offline (${formatFileSize(f.length())})",
                    duration = durationStr,
                    seconds = durationSec,
                    thumbnail = thumb,
                    localPath = f.absolutePath
                )
            )
        }
        tracks
    }

    suspend fun scanDownloadedVideos(): List<SavedVideoFile> = withContext(Dispatchers.IO) {
        val moviesDir = AmberEngine.getDownloadOutputDir(context, isAudio = false)
        val files = moviesDir.listFiles { _, name ->
            name.contains("[") && name.contains("]") && (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm"))
        } ?: return@withContext emptyList()

        val idPattern = Pattern.compile("\\[([A-Za-z0-9_-]{11})\\]")
        val qualityPattern = Pattern.compile("(\\d+p)")
        val result = mutableListOf<SavedVideoFile>()

        for (f in files.sortedByDescending { it.lastModified() }) {
            val matcher = idPattern.matcher(f.name)
            val vid = if (matcher.find()) matcher.group(1) else ""
            val title = f.name.substringBefore("[").trim().ifEmpty { f.nameWithoutExtension }
            val qMatcher = qualityPattern.matcher(f.name)
            val quality = if (qMatcher.find()) qMatcher.group(1) else ""
            val ext = f.extension.uppercase()

            result.add(
                SavedVideoFile(
                    filename = f.name,
                    title = title,
                    videoId = vid,
                    quality = quality,
                    ext = ext,
                    sizeBytes = f.length(),
                    lastModified = f.lastModified(),
                    filePath = f.absolutePath,
                    thumbnail = if (vid.isNotEmpty()) "https://i.ytimg.com/vi/$vid/mqdefault.jpg" else ""
                )
            )
        }
        result
    }

    suspend fun deleteLocalFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var b = bytes.toDouble()
        var i = 0
        while (b >= 1024.0 && i < units.size - 1) {
            b /= 1024.0
            i++
        }
        return String.format("%.1f %s", b, units[i])
    }

    companion object {
        @Volatile
        private var INSTANCE: AmberRepository? = null

        fun getInstance(context: Context): AmberRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AmberDatabase.getInstance(context)
                val prefs = UserPreferencesRepository(context)
                val instance = AmberRepository(context.applicationContext, db, prefs)
                INSTANCE = instance
                instance
            }
        }
    }
}
