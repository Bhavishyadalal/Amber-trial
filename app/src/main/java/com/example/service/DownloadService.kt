package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AmberApplication
import com.example.MainActivity
import com.example.R
import com.example.data.local.AmberDatabase
import com.example.data.model.DownloadJob
import com.example.data.model.JobStatus
import com.example.engine.AmberEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val mutex = Mutex()
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Amber:DownloadWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val jobId = intent?.getStringExtra(EXTRA_JOB_ID)

        when (action) {
            ACTION_START -> {
                pumpQueue()
            }
            ACTION_PAUSE -> {
                if (jobId != null) pauseJob(jobId)
            }
            ACTION_CANCEL -> {
                if (jobId != null) cancelJob(jobId)
            }
            ACTION_RETRY -> {
                if (jobId != null) retryJob(jobId)
            }
        }

        return START_NOT_STICKY
    }

    private fun pumpQueue() {
        serviceScope.launch {
            mutex.withLock {
                val db = AmberDatabase.getInstance(applicationContext)
                val jobDao = db.downloadJobDao()

                // Check active count
                if (activeJobs.size >= MAX_CONCURRENT_DOWNLOADS) return@withLock

                val allJobs = db.openHelper.readableDatabase.query(
                    "SELECT id FROM download_jobs WHERE status = 'QUEUED' ORDER BY timestamp ASC"
                )
                val queuedIds = mutableListOf<String>()
                while (allJobs.moveToNext()) {
                    queuedIds.add(allJobs.getString(0))
                }
                allJobs.close()

                for (id in queuedIds) {
                    if (activeJobs.size >= MAX_CONCURRENT_DOWNLOADS) break
                    if (!activeJobs.containsKey(id)) {
                        startJobExecution(id)
                    }
                }

                if (activeJobs.isNotEmpty()) {
                    acquireWakeLock()
                    updateForegroundNotification()
                } else {
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun startJobExecution(jobId: String) {
        val job = serviceScope.launch {
            val db = AmberDatabase.getInstance(applicationContext)
            val jobDao = db.downloadJobDao()
            val jobData = jobDao.getJobById(jobId) ?: return@launch

            jobDao.updateProgress(
                id = jobId,
                status = JobStatus.DOWNLOADING,
                stage = "Preparing",
                progress = 0f,
                speed = 0L,
                eta = 0L,
                bytesDone = 0L,
                bytesTotal = jobData.bytesTotal
            )

            try {
                val (filename, filePath) = AmberEngine.executeDownload(
                    context = applicationContext,
                    job = jobData
                ) { progress, speed, eta, stage, bytesDone, bytesTotal ->
                    serviceScope.launch {
                        jobDao.updateProgress(
                            id = jobId,
                            status = JobStatus.DOWNLOADING,
                            stage = stage,
                            progress = progress,
                            speed = speed,
                            eta = eta,
                            bytesDone = bytesDone,
                            bytesTotal = bytesTotal
                        )
                        updateForegroundNotification()
                    }
                }

                jobDao.markDone(jobId, JobStatus.DONE, filename, filePath)
            } catch (e: Exception) {
                Log.e(TAG, "Download error for job $jobId: ${e.message}")
                jobDao.markError(jobId, JobStatus.ERROR, e.message?.take(110) ?: "Download failed")
            } finally {
                activeJobs.remove(jobId)
                pumpQueue()
            }
        }

        activeJobs[jobId] = job
    }

    private fun pauseJob(jobId: String) {
        serviceScope.launch {
            activeJobs[jobId]?.cancel()
            activeJobs.remove(jobId)
            AmberEngine.cancelJob(jobId)
            val db = AmberDatabase.getInstance(applicationContext)
            db.downloadJobDao().updateProgress(
                id = jobId,
                status = JobStatus.PAUSED,
                stage = "Paused",
                progress = 0f,
                speed = 0L,
                eta = 0L,
                bytesDone = 0L,
                bytesTotal = 0L
            )
            pumpQueue()
        }
    }

    private fun cancelJob(jobId: String) {
        serviceScope.launch {
            activeJobs[jobId]?.cancel()
            activeJobs.remove(jobId)
            AmberEngine.cancelJob(jobId)
            val db = AmberDatabase.getInstance(applicationContext)
            db.downloadJobDao().updateProgress(
                id = jobId,
                status = JobStatus.CANCELLED,
                stage = "Cancelled",
                progress = 0f,
                speed = 0L,
                eta = 0L,
                bytesDone = 0L,
                bytesTotal = 0L
            )
            pumpQueue()
        }
    }

    private fun retryJob(jobId: String) {
        serviceScope.launch {
            val db = AmberDatabase.getInstance(applicationContext)
            db.downloadJobDao().updateProgress(
                id = jobId,
                status = JobStatus.QUEUED,
                stage = "Queued",
                progress = 0f,
                speed = 0L,
                eta = 0L,
                bytesDone = 0L,
                bytesTotal = 0L
            )
            pumpQueue()
        }
    }

    private fun updateForegroundNotification() {
        val count = activeJobs.size
        if (count == 0) return

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_tab", "download")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, AmberApplication.CHANNEL_DOWNLOADS)
            .setContentTitle("Amber Downloading ($count active)")
            .setContentText("Downloading video/audio in progress…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours max
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "DownloadService"
        const val MAX_CONCURRENT_DOWNLOADS = 2

        const val ACTION_START = "com.example.amber.action.START"
        const val ACTION_PAUSE = "com.example.amber.action.PAUSE"
        const val ACTION_CANCEL = "com.example.amber.action.CANCEL"
        const val ACTION_RETRY = "com.example.amber.action.RETRY"

        const val EXTRA_JOB_ID = "extra_job_id"

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(intent)
        }

        fun cancel(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(intent)
        }

        fun retry(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RETRY
                putExtra(EXTRA_JOB_ID, jobId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
