package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.engine.AmberEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AmberApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        applicationScope.launch {
            try {
                AmberEngine.initialize(this@AmberApplication)
            } catch (e: Exception) {
                Log.e("AmberApp", "Failed to initialize AmberEngine", e)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }

            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress and status"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(playbackChannel)
            notificationManager.createNotificationChannel(downloadChannel)
        }
    }

    companion object {
        const val CHANNEL_PLAYBACK = "amber_playback"
        const val CHANNEL_DOWNLOADS = "amber_downloads"
        lateinit var instance: AmberApplication
            private set
    }
}
