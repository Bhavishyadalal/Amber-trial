package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class AmberPreferences(
    val audioFormat: String = "m4a", // "m4a", "mp3", "opus"
    val embedMeta: Boolean = true,
    val subtitles: Boolean = false,
    val chapters: Boolean = true,
    val autoplay: Boolean = true,
    val ambientGlow: Boolean = true,
    val lowPower: Boolean = false,
    val performance: Boolean = false,
    val downloadMode: String = "video", // "video" or "audio"
    val videoPreset: Int = 0, // 0 = Best, 2160, 1440, 1080, 720, 480, 360
    val container: String = "auto", // "auto", "mp4", "mkv"
    val autoUpdateYtDlp: Boolean = true,
    val youtubeApiKey: String = "",
    val browseMode: String = "ytm", // "ytm" or "yt"
    val playbackSpeed: Float = 1.0f
)

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("amber_prefs", Context.MODE_PRIVATE)

    fun getPreferences(): Flow<AmberPreferences> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(loadCurrentPreferences())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(loadCurrentPreferences())
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.conflate()

    fun loadCurrentPreferences(): AmberPreferences {
        return AmberPreferences(
            audioFormat = prefs.getString("audio_format", "m4a") ?: "m4a",
            embedMeta = prefs.getBoolean("embed_meta", true),
            subtitles = prefs.getBoolean("subtitles", false),
            chapters = prefs.getBoolean("chapters", true),
            autoplay = prefs.getBoolean("autoplay", true),
            ambientGlow = prefs.getBoolean("ambient_glow", true),
            lowPower = prefs.getBoolean("low_power", false),
            performance = prefs.getBoolean("performance", false),
            downloadMode = prefs.getString("download_mode", "video") ?: "video",
            videoPreset = prefs.getInt("video_preset", 0),
            container = prefs.getString("container", "auto") ?: "auto",
            autoUpdateYtDlp = prefs.getBoolean("auto_update_ytdlp", true),
            youtubeApiKey = prefs.getString("youtube_api_key", "") ?: "",
            browseMode = prefs.getString("browse_mode", "ytm") ?: "ytm",
            playbackSpeed = prefs.getFloat("playback_speed", 1.0f)
        )
    }

    fun setAudioFormat(format: String) {
        prefs.edit().putString("audio_format", format).apply()
    }

    fun setEmbedMeta(enabled: Boolean) {
        prefs.edit().putBoolean("embed_meta", enabled).apply()
    }

    fun setSubtitles(enabled: Boolean) {
        prefs.edit().putBoolean("subtitles", enabled).apply()
    }

    fun setChapters(enabled: Boolean) {
        prefs.edit().putBoolean("chapters", enabled).apply()
    }

    fun setAutoplay(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay", enabled).apply()
    }

    fun setAmbientGlow(enabled: Boolean) {
        prefs.edit().putBoolean("ambient_glow", enabled).apply()
    }

    fun setLowPower(enabled: Boolean) {
        prefs.edit().putBoolean("low_power", enabled).apply()
    }

    fun setPerformance(enabled: Boolean) {
        prefs.edit().putBoolean("performance", enabled).apply()
    }

    fun setDownloadMode(mode: String) {
        prefs.edit().putString("download_mode", mode).apply()
    }

    fun setVideoPreset(preset: Int) {
        prefs.edit().putInt("video_preset", preset).apply()
    }

    fun setContainer(container: String) {
        prefs.edit().putString("container", container).apply()
    }

    fun setYoutubeApiKey(key: String) {
        prefs.edit().putString("youtube_api_key", key).apply()
    }

    fun setBrowseMode(mode: String) {
        prefs.edit().putString("browse_mode", mode).apply()
    }

    fun setPlaybackSpeed(speed: Float) {
        prefs.edit().putFloat("playback_speed", speed).apply()
    }
}
