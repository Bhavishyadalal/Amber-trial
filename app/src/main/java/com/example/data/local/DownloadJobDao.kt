package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadJob
import com.example.data.model.JobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadJobDao {

    @Query("SELECT * FROM download_jobs ORDER BY timestamp DESC")
    fun getAllJobs(): Flow<List<DownloadJob>>

    @Query("SELECT * FROM download_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: String): DownloadJob?

    @Query("SELECT * FROM download_jobs WHERE videoId = :videoId LIMIT 1")
    suspend fun getJobByVideoId(videoId: String): DownloadJob?

    @Query("SELECT COUNT(*) FROM download_jobs WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED')")
    fun getActiveJobCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: DownloadJob)

    @Update
    suspend fun updateJob(job: DownloadJob)

    @Query("UPDATE download_jobs SET status = :status, stage = :stage, progress = :progress, speed = :speed, eta = :eta, bytesDone = :bytesDone, bytesTotal = :bytesTotal WHERE id = :id")
    suspend fun updateProgress(
        id: String,
        status: JobStatus,
        stage: String,
        progress: Float,
        speed: Long,
        eta: Long,
        bytesDone: Long,
        bytesTotal: Long
    )

    @Query("UPDATE download_jobs SET status = :status, stage = 'Done', progress = 100, filename = :filename, filePath = :filePath WHERE id = :id")
    suspend fun markDone(id: String, status: JobStatus = JobStatus.DONE, filename: String, filePath: String)

    @Query("UPDATE download_jobs SET status = :status, error = :error, stage = 'Error' WHERE id = :id")
    suspend fun markError(id: String, status: JobStatus = JobStatus.ERROR, error: String)

    @Query("DELETE FROM download_jobs WHERE id = :id")
    suspend fun deleteJob(id: String)

    @Query("DELETE FROM download_jobs WHERE status IN ('DONE', 'ERROR', 'CANCELLED')")
    suspend fun clearFinishedJobs()
}
