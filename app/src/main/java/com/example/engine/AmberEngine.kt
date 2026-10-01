package com.example.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.model.AudioFormat
import com.example.data.model.DownloadJob
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistDetails
import com.example.data.model.Track
import com.example.data.model.VideoDetails
import com.example.data.model.VideoFormat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object AmberEngine {

    private const val TAG = "AmberEngine"
    private var isInitialized = false
    private var ffmpegAvailable = false
    private var ytDlpVersion: String = "unknown"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Stream cache: videoId -> (url, expiryTimestamp)
    private val streamCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private const val STREAM_CACHE_TTL = 50 * 60 * 1000L // 50 mins

    val VIDEO_ID_REGEX = Pattern.compile("^[A-Za-z0-9_-]{11}$")
    val YOUTUBE_URL_REGEX = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|shorts/|embed/|live/|v/))([A-Za-z0-9_-]{11})")

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        try {
            YoutubeDL.getInstance().init(context)
            try {
                FFmpeg.getInstance().init(context)
                ffmpegAvailable = true
            } catch (fe: Exception) {
                Log.w(TAG, "FFmpeg init failed", fe)
                ffmpegAvailable = false
            }
            ytDlpVersion = YoutubeDL.getInstance().version(context) ?: "bundled"
            isInitialized = true
            Log.d(TAG, "AmberEngine initialized successfully. yt-dlp: $ytDlpVersion, ffmpeg: $ffmpegAvailable")
        } catch (e: Exception) {
            Log.e(TAG, "AmberEngine init failed", e)
        }
    }

    fun isEngineReady(): Boolean = isInitialized
    fun isFfmpegAvailable(): Boolean = ffmpegAvailable
    fun getYtDlpVersion(): String = ytDlpVersion

    suspend fun updateYtDlp(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            val newVer = YoutubeDL.getInstance().version(context) ?: "unknown"
            ytDlpVersion = newVer
            if (status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) {
                Pair(true, "Already up to date ($newVer)")
            } else {
                Pair(true, "Updated to $newVer")
            }
        } catch (e: Exception) {
            Log.e(TAG, "yt-dlp update failed", e)
            Pair(false, e.message?.take(110) ?: "Update failed")
        }
    }

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (VIDEO_ID_REGEX.matcher(trimmed).matches()) {
            return trimmed
        }
        val matcher = YOUTUBE_URL_REGEX.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    fun isPlaylistUrl(input: String): Boolean {
        val trimmed = input.trim()
        return (trimmed.contains("list=") && !trimmed.contains("list=RDAMVM")) ||
                trimmed.contains("/playlist") ||
                trimmed.contains("/channel/") ||
                trimmed.contains("/@") ||
                trimmed.contains("/c/")
    }

    suspend fun getStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = streamCache[videoId]
        if (cached != null && cached.second > now) {
            return@withContext cached.first
        }

        val clients = listOf(
            "android_vr,ios,web_safari",
            "android,ios,mweb,web",
            "android",
            "ios"
        )

        var lastException: Exception? = null
        for (client in clients) {
            try {
                val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId").apply {
                    addOption("-f", "140/251/ba[ext=m4a]/ba/b/bestaudio/best")
                    addOption("--skip-download")
                    addOption("--dump-single-json")
                    addOption("--no-warnings")
                    addOption("--extractor-args", "youtube:player_client=$client")
                }
                val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(request)
                val json = JSONObject(response.out)
                var directUrl = json.optString("url")
                if (directUrl.isNullOrEmpty() && json.has("formats")) {
                    val formats = json.getJSONArray("formats")
                    for (i in 0 until formats.length()) {
                        val fmt = formats.getJSONObject(i)
                        val u = fmt.optString("url")
                        val acodec = fmt.optString("acodec", "none")
                        val vcodec = fmt.optString("vcodec", "none")
                        if (u.isNotEmpty() && acodec != "none") {
                            directUrl = u
                            if (vcodec == "none") break
                        }
                    }
                }

                if (!directUrl.isNullOrEmpty()) {
                    streamCache[videoId] = Pair(directUrl, now + STREAM_CACHE_TTL)
                    return@withContext directUrl
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Stream extraction with client $client failed: ${e.message}")
            }
        }

        throw lastException ?: RuntimeException("Could not extract playable stream URL for video $videoId")
    }

    suspend fun getVideoDetails(urlOrId: String): VideoDetails = withContext(Dispatchers.IO) {
        val vid = extractVideoId(urlOrId) ?: urlOrId
        val targetUrl = if (urlOrId.startsWith("http")) urlOrId else "https://www.youtube.com/watch?v=$vid"

        val request = YoutubeDLRequest(targetUrl).apply {
            addOption("--dump-single-json")
            addOption("--skip-download")
            addOption("--no-warnings")
            addOption("--extractor-args", "youtube:player_client=android_vr,ios,web_safari")
        }

        val response = YoutubeDL.getInstance().execute(request)
        val json = JSONObject(response.out)

        val id = json.optString("id", vid)
        val title = json.optString("title", id)
        val channel = json.optString("uploader", json.optString("channel", ""))
        val channelId = json.optString("channel_id", "")
        val durationSec = json.optLong("duration", 0L)
        val durationStr = formatDuration(durationSec)
        val thumbnail = json.optString("thumbnail", "https://i.ytimg.com/vi/$id/hqdefault.jpg")
        val isLive = json.optBoolean("is_live", false)

        val videosList = mutableListOf<VideoFormat>()
        val audiosList = mutableListOf<AudioFormat>()

        if (json.has("formats")) {
            val formats = json.getJSONArray("formats")
            for (i in 0 until formats.length()) {
                val f = formats.getJSONObject(i)
                val fid = f.optString("format_id")
                val vcodec = f.optString("vcodec", "none")
                val acodec = f.optString("acodec", "none")
                val ext = f.optString("ext", "mp4")
                val h = f.optInt("height", 0)
                val w = f.optInt("width", 0)
                val fps = f.optInt("fps", 0)
                val tbr = f.optDouble("tbr", 0.0)
                val abr = f.optInt("abr", 0)
                val lang = f.optString("language", "")
                val dr = f.optString("dynamic_range", "")
                val filesize = f.optLong("filesize", f.optLong("filesize_approx", 0L))
                val isApprox = !f.has("filesize") || f.optLong("filesize", 0L) == 0L

                if (vcodec == "none" && acodec != "none") {
                    audiosList.add(
                        AudioFormat(
                            id = fid,
                            ext = ext,
                            codec = parseAudioCodec(acodec),
                            abr = abr,
                            lang = lang,
                            size = if (filesize > 0) filesize else (abr * 125L * durationSec),
                            approx = isApprox
                        )
                    )
                } else if (h > 0) {
                    val codecFamily = parseVideoCodec(vcodec)
                    videosList.add(
                        VideoFormat(
                            id = fid,
                            height = h,
                            width = w,
                            fps = fps,
                            codec = codecFamily,
                            ext = ext,
                            tbr = tbr,
                            hdr = if (dr == "SDR" || dr.isEmpty()) "" else dr,
                            hasAudio = acodec != "none",
                            size = if (filesize > 0) filesize else ((tbr * 125.0 * durationSec).toLong()),
                            approx = isApprox
                        )
                    )
                }
            }
        }

        // Deduplicate and prioritize real audio and video options
        val filteredVideos = if (ffmpegAvailable) videosList else videosList.filter { it.hasAudio }
        val sortedVideos = filteredVideos.sortedWith(
            compareByDescending<VideoFormat> { it.height }
                .thenByDescending { it.fps }
                .thenBy { codecRank(it.codec) }
        )
        val sortedAudios = audiosList.sortedByDescending { it.abr }

        VideoDetails(
            id = id,
            title = title,
            channel = channel,
            channelId = channelId,
            duration = durationStr,
            seconds = durationSec,
            thumbnail = thumbnail,
            videos = sortedVideos,
            audios = sortedAudios,
            isLive = isLive,
            ffmpegAvailable = ffmpegAvailable
        )
    }

    suspend fun getPlaylistDetails(url: String): PlaylistDetails = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--flat-playlist")
            addOption("-J")
            addOption("--playlist-end", "200")
            addOption("--no-warnings")
        }
        val response = YoutubeDL.getInstance().execute(request)
        val json = JSONObject(response.out)

        val id = json.optString("id", "")
        val title = json.optString("title", "Playlist")
        val channel = json.optString("uploader", json.optString("channel", ""))

        val items = mutableListOf<PlaylistItem>()
        if (json.has("entries")) {
            val entries = json.getJSONArray("entries")
            for (i in 0 until entries.length()) {
                val e = entries.getJSONObject(i)
                val vid = e.optString("id")
                val itemTitle = e.optString("title", vid)
                if (vid.isNotEmpty() && itemTitle != "[Private video]" && itemTitle != "[Deleted video]") {
                    val durSec = e.optLong("duration", 0L)
                    val thumb = e.optString("thumbnail", "https://i.ytimg.com/vi/$vid/mqdefault.jpg")
                    items.add(
                        PlaylistItem(
                            id = vid,
                            title = itemTitle,
                            channel = e.optString("uploader", e.optString("channel", channel)),
                            duration = formatDuration(durSec),
                            thumbnail = thumb,
                            isLive = e.optString("live_status") == "is_live"
                        )
                    )
                }
            }
        }

        PlaylistDetails(
            id = id,
            title = title,
            channel = channel,
            count = items.size,
            items = items
        )
    }

    suspend fun searchVideos(query: String, filter: String = ""): List<Track> = withContext(Dispatchers.IO) {
        val searchQuery = if (filter.isNotEmpty()) "$query $filter" else query
        val request = YoutubeDLRequest("ytsearch20:$searchQuery").apply {
            addOption("--flat-playlist")
            addOption("-J")
            addOption("--no-warnings")
        }
        try {
            val response = YoutubeDL.getInstance().execute(request)
            val json = JSONObject(response.out)
            val results = mutableListOf<Track>()
            if (json.has("entries")) {
                val entries = json.getJSONArray("entries")
                for (i in 0 until entries.length()) {
                    val e = entries.getJSONObject(i)
                    val vid = e.optString("id")
                    if (vid.isNotEmpty()) {
                        val dur = e.optLong("duration", 0L)
                        results.add(
                            Track(
                                id = vid,
                                title = e.optString("title", vid),
                                artist = e.optString("uploader", e.optString("channel", "")),
                                channelId = e.optString("channel_id", ""),
                                duration = formatDuration(dur),
                                seconds = dur,
                                thumbnail = e.optString("thumbnail", "https://i.ytimg.com/vi/$vid/mqdefault.jpg"),
                                isLive = e.optString("live_status") == "is_live"
                            )
                        )
                    }
                }
            }
            return@withContext results
        } catch (e: Exception) {
            Log.e(TAG, "searchVideos failed: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun searchYtMusic(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20241001.00.00")
                    })
                })
                put("query", query)
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext searchVideos(query, "music")
            }

            val responseStr = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(responseStr)
            val results = mutableListOf<Track>()
            val seen = mutableSetOf<String>()

            fun extractFromObject(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        if (obj.has("musicResponsiveListItemRenderer")) {
                            val item = obj.getJSONObject("musicResponsiveListItemRenderer")
                            var vid = item.optJSONObject("playlistItemData")?.optString("videoId") ?: ""
                            if (vid.isEmpty() && item.has("flexColumns")) {
                                val cols = item.getJSONArray("flexColumns")
                                for (c in 0 until cols.length()) {
                                    val col = cols.getJSONObject(c)
                                    val runs = col.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")?.optJSONArray("runs")
                                    if (runs != null) {
                                        for (r in 0 until runs.length()) {
                                            val ep = runs.getJSONObject(r).optJSONObject("navigationEndpoint")
                                                ?.optJSONObject("watchEndpoint")
                                            val v = ep?.optString("videoId")
                                            if (!v.isNullOrEmpty()) {
                                                vid = v
                                                break
                                            }
                                        }
                                    }
                                    if (vid.isNotEmpty()) break
                                }
                            }

                            if (vid.isNotEmpty() && !seen.contains(vid) && VIDEO_ID_REGEX.matcher(vid).matches()) {
                                seen.add(vid)
                                var title = ""
                                var artist = ""
                                var duration = ""

                                val flexCols = item.optJSONArray("flexColumns")
                                if (flexCols != null && flexCols.length() > 0) {
                                    val titleRuns = flexCols.getJSONObject(0)
                                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")?.optJSONArray("runs")
                                    if (titleRuns != null && titleRuns.length() > 0) {
                                        title = titleRuns.getJSONObject(0).optString("text")
                                    }

                                    if (flexCols.length() > 1) {
                                        val artistRuns = flexCols.getJSONObject(1)
                                            .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                            ?.optJSONObject("text")?.optJSONArray("runs")
                                        if (artistRuns != null) {
                                            val parts = mutableListOf<String>()
                                            for (k in 0 until artistRuns.length()) {
                                                val txt = artistRuns.getJSONObject(k).optString("text")
                                                if (txt.isNotEmpty() && txt != " • ") parts.add(txt)
                                            }
                                            artist = parts.joinToString(" • ")
                                        }
                                    }
                                }

                                val thumbs = item.optJSONObject("thumbnail")
                                    ?.optJSONObject("musicThumbnailRenderer")
                                    ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                                    thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                                } else {
                                    "https://i.ytimg.com/vi/$vid/mqdefault.jpg"
                                }

                                if (title.isNotEmpty()) {
                                    results.add(
                                        Track(
                                            id = vid,
                                            title = title,
                                            artist = artist,
                                            duration = duration,
                                            thumbnail = thumbUrl
                                        )
                                    )
                                }
                            }
                        } else {
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                extractFromObject(obj.get(keys.next()))
                            }
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            extractFromObject(obj.get(i))
                        }
                    }
                }
            }

            extractFromObject(root)
            if (results.isEmpty()) {
                return@withContext searchVideos(query, "music")
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "searchYtMusic error", e)
            searchVideos(query, "music")
        }
    }

    suspend fun getRelatedTracks(seedVideoId: String, limit: Int = 12): List<Track> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20241001.00.00")
                    })
                })
                put("playlistId", "RDAMVM$seedVideoId")
                put("videoId", seedVideoId)
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/next")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val responseStr = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(responseStr)
            val results = mutableListOf<Track>()
            val seen = mutableSetOf(seedVideoId)

            fun walk(obj: Any?) {
                when (obj) {
                    is JSONObject -> {
                        if (obj.has("playlistPanelVideoRenderer")) {
                            val p = obj.getJSONObject("playlistPanelVideoRenderer")
                            val vid = p.optString("videoId")
                            if (vid.isNotEmpty() && !seen.contains(vid) && VIDEO_ID_REGEX.matcher(vid).matches()) {
                                seen.add(vid)
                                val titleRuns = p.optJSONObject("title")?.optJSONArray("runs")
                                val title = titleRuns?.optJSONObject(0)?.optString("text") ?: ""
                                val bylineRuns = p.optJSONObject("longBylineText")?.optJSONArray("runs")
                                    ?: p.optJSONObject("shortBylineText")?.optJSONArray("runs")
                                val artist = if (bylineRuns != null) {
                                    val parts = mutableListOf<String>()
                                    for (k in 0 until bylineRuns.length()) {
                                        val t = bylineRuns.getJSONObject(k).optString("text")
                                        if (t.isNotEmpty() && t != " • ") parts.add(t)
                                    }
                                    parts.joinToString(" • ")
                                } else ""

                                val durRuns = p.optJSONObject("lengthText")?.optJSONArray("runs")
                                val dur = durRuns?.optJSONObject(0)?.optString("text")
                                    ?: p.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""

                                val thumbs = p.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val thumb = if (thumbs != null && thumbs.length() > 0) {
                                    thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                                } else "https://i.ytimg.com/vi/$vid/mqdefault.jpg"

                                if (title.isNotEmpty()) {
                                    results.add(
                                        Track(
                                            id = vid,
                                            title = title,
                                            artist = artist,
                                            duration = dur,
                                            thumbnail = thumb
                                        )
                                    )
                                }
                            }
                        } else {
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                walk(obj.get(keys.next()))
                            }
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            walk(obj.get(i))
                        }
                    }
                }
            }

            walk(root)
            results.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getRelatedTracks error", e)
            emptyList()
        }
    }

    suspend fun executeDownload(
        context: Context,
        job: DownloadJob,
        onProgress: (progress: Float, speed: Long, eta: Long, stage: String, bytesDone: Long, bytesTotal: Long) -> Unit
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val isAudio = job.kind == "audio"
        val outDir = getDownloadOutputDir(context, isAudio)
        outDir.mkdirs()

        val tempDir = File(context.cacheDir, "dl_${job.id}").apply { mkdirs() }
        val sanitizedTitle = sanitizeFilename(job.title)
        val nameTemplate = if (isAudio) {
            "$sanitizedTitle [${job.videoId}].%(ext)s"
        } else {
            val heightLabel = if (job.height > 0) "${job.height}p" else "%(height)sp"
            "$sanitizedTitle [${job.videoId}] $heightLabel.%(ext)s"
        }

        val selector = buildFormatSelector(job)
        val request = YoutubeDLRequest("https://www.youtube.com/watch?v=${job.videoId}").apply {
            addOption("-f", selector)
            addOption("-o", File(outDir, nameTemplate).absolutePath)
            addOption("--paths", "temp:${tempDir.absolutePath}")
            addOption("--no-warnings")
            addOption("--retries", "5")
            addOption("--fragment-retries", "5")
            addOption("--socket-timeout", "30")
            addOption("--concurrent-fragments", "4")
            addOption("--extractor-args", "youtube:player_client=android_vr,ios,web_safari,mweb,android,web")

            if (isAudio) {
                if (ffmpegAvailable) {
                    when (job.audioFormat.lowercase()) {
                        "mp3" -> {
                            addOption("-x")
                            addOption("--audio-format", "mp3")
                            addOption("--audio-quality", "192K")
                        }
                        "opus" -> {
                            addOption("-x")
                            addOption("--audio-format", "opus")
                        }
                    }
                    addOption("--embed-metadata")
                    addOption("--embed-thumbnail")
                    addOption("--convert-thumbnails", "jpg")
                }
            } else {
                when (job.container.lowercase()) {
                    "mp4" -> addOption("--merge-output-format", "mp4")
                    "mkv" -> addOption("--merge-output-format", "mkv")
                    else -> addOption("--merge-output-format", "mp4/mkv")
                }
                if (ffmpegAvailable) {
                    addOption("--embed-chapters")
                }
            }
        }

        var lastProgress = 0f
        var currentStage = "Downloading"

        try {
            YoutubeDL.getInstance().execute(request, job.id) { progress, etaInSeconds, line ->
                val speedBytes = parseSpeed(line)
                val stage = when {
                    line.contains("[Merger]") -> "Merging"
                    line.contains("[ExtractAudio]") -> "Converting"
                    line.contains("[EmbedThumbnail]") || line.contains("[Metadata]") -> "Finishing"
                    progress > 98f -> "Finishing"
                    else -> "Downloading"
                }
                currentStage = stage
                lastProgress = progress
                onProgress(progress, speedBytes, etaInSeconds, stage, 0L, job.bytesTotal)
            }

            // Find created file
            val targetFiles = outDir.listFiles { _, name ->
                name.contains("[${job.videoId}]") && !name.endsWith(".part") && !name.endsWith(".ytdl") && !name.startsWith(".")
            }?.sortedByDescending { it.lastModified() }

            val finishedFile = targetFiles?.firstOrNull()
                ?: throw RuntimeException("Download completed but destination file was not found")

            tempDir.deleteRecursively()
            Pair(finishedFile.name, finishedFile.absolutePath)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            throw e
        }
    }

    fun cancelJob(jobId: String) {
        try {
            YoutubeDL.getInstance().destroyProcessById(jobId)
        } catch (e: Exception) {
            Log.w(TAG, "Cancel job $jobId exception: ${e.message}")
        }
    }

    fun getDownloadOutputDir(context: Context, isAudio: Boolean): File {
        return if (isAudio) {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            File(musicDir, "Amber").takeIf { it.exists() || it.mkdirs() }
                ?: File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Amber")
        } else {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            File(moviesDir, "Amber").takeIf { it.exists() || it.mkdirs() }
                ?: File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Amber")
        }
    }

    private fun buildFormatSelector(job: DownloadJob): String {
        if (job.kind == "audio") {
            return when (job.audioFormat.lowercase()) {
                "m4a" -> "140/bestaudio[ext=m4a]/bestaudio/best"
                "opus" -> "251/bestaudio[acodec=opus]/bestaudio/best"
                else -> "bestaudio/best"
            }
        }
        if (job.videoFormatId.isNotEmpty()) {
            return if (job.audioFormatId.isNotEmpty()) {
                "${job.videoFormatId}+${job.audioFormatId}"
            } else {
                "${job.videoFormatId}+(ba[ext=m4a]/ba)"
            }
        }
        val h = job.height
        return if (h > 0) {
            "(bv*[height<=$h]+ba)/b[height<=$h]/(bv*+ba)/b"
        } else {
            "(bv*+ba)/b"
        }
    }

    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.')
            .take(120)
            .ifEmpty { "Amber_Media" }
    }

    private fun parseVideoCodec(codec: String): String {
        val c = codec.lowercase()
        return when {
            c.startsWith("avc1") || c.startsWith("h264") -> "H.264"
            c.startsWith("vp09") || c.startsWith("vp9") -> "VP9"
            c.startsWith("av01") || c.startsWith("av1") -> "AV1"
            c.startsWith("hev") || c.startsWith("hvc") || c.startsWith("h265") -> "H.265"
            else -> "H.264"
        }
    }

    private fun parseAudioCodec(codec: String): String {
        val c = codec.lowercase()
        return when {
            c.startsWith("mp4a") || c.contains("aac") -> "AAC"
            c.contains("opus") -> "Opus"
            c.contains("mp3") -> "MP3"
            else -> codec.uppercase()
        }
    }

    private fun codecRank(codec: String): Int = when (codec) {
        "H.264" -> 0
        "VP9" -> 1
        "AV1" -> 2
        else -> 3
    }

    private fun parseSpeed(line: String): Long {
        try {
            val match = Regex("at\\s+([0-9.]+)\\s*([kKMmGg]?[iI]?[bB]/s)").find(line) ?: return 0L
            val num = match.groupValues[1].toDoubleOrNull() ?: return 0L
            val unit = match.groupValues[2].uppercase()
            return when {
                unit.startsWith("G") -> (num * 1024 * 1024 * 1024).toLong()
                unit.startsWith("M") -> (num * 1024 * 1024).toLong()
                unit.startsWith("K") -> (num * 1024).toLong()
                else -> num.toLong()
            }
        } catch (_: Exception) {
            return 0L
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0L) return "0:00"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%d:%02d", m, s)
        }
    }
}
