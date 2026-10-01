package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.local.AmberPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DownloadJob
import com.example.data.model.JobStatus
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistDetails
import com.example.data.model.Track
import com.example.data.model.VideoDetails
import com.example.data.repository.AmberRepository
import com.example.data.repository.SavedVideoFile
import com.example.engine.AmberEngine
import com.example.service.DownloadService
import com.example.service.PlaybackService
import com.example.ui.components.AmberTab
import com.example.ui.theme.AmberAccentDefault
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class HomeShelf(
    val id: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
    val isLoading: Boolean = false
)

class AmberViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AmberRepository.getInstance(application)
    private val prefsRepo = UserPreferencesRepository(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Navigation Tab
    private val _currentTab = MutableStateFlow(AmberTab.HOME)
    val currentTab: StateFlow<AmberTab> = _currentTab.asStateFlow()

    // Preferences
    val preferences: StateFlow<AmberPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = prefsRepo.loadCurrentPreferences()
    )

    // Player State
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: off, 1: all, 2: one
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _paletteAccentColor = MutableStateFlow(AmberAccentDefault)
    val paletteAccentColor: StateFlow<Color> = _paletteAccentColor.asStateFlow()

    // Home Shelves
    private val _shelves = MutableStateFlow<List<HomeShelf>>(emptyList())
    val shelves: StateFlow<List<HomeShelf>> = _shelves.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    // Browse State
    private val _browseQuery = MutableStateFlow("")
    val browseQuery: StateFlow<String> = _browseQuery.asStateFlow()

    private val _browseFilter = MutableStateFlow("")
    val browseFilter: StateFlow<String> = _browseFilter.asStateFlow()

    private val _browseResults = MutableStateFlow<List<Track>>(emptyList())
    val browseResults: StateFlow<List<Track>> = _browseResults.asStateFlow()

    private val _isBrowseLoading = MutableStateFlow(false)
    val isBrowseLoading: StateFlow<Boolean> = _isBrowseLoading.asStateFlow()

    // Library State
    val likedTracks: StateFlow<List<Track>> = repository.likedTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val recentTracks: StateFlow<List<Track>> = repository.recentTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    private val _downloadedTracks = MutableStateFlow<List<Track>>(emptyList())
    val downloadedTracks: StateFlow<List<Track>> = _downloadedTracks.asStateFlow()

    private val _libraryFilter = MutableStateFlow("")
    val libraryFilter: StateFlow<String> = _libraryFilter.asStateFlow()

    // Download Tab State
    private val _downloadUrlInput = MutableStateFlow("")
    val downloadUrlInput: StateFlow<String> = _downloadUrlInput.asStateFlow()

    private val _fetchedVideo = MutableStateFlow<VideoDetails?>(null)
    val fetchedVideo: StateFlow<VideoDetails?> = _fetchedVideo.asStateFlow()

    private val _fetchedPlaylist = MutableStateFlow<PlaylistDetails?>(null)
    val fetchedPlaylist: StateFlow<PlaylistDetails?> = _fetchedPlaylist.asStateFlow()

    private val _isFetchingDetails = MutableStateFlow(false)
    val isFetchingDetails: StateFlow<Boolean> = _isFetchingDetails.asStateFlow()

    private val _fetchError = MutableStateFlow<String?>(null)
    val fetchError: StateFlow<String?> = _fetchError.asStateFlow()

    private val _downloadTabMode = MutableStateFlow("video") // "video" or "audio"
    val downloadTabMode: StateFlow<String> = _downloadTabMode.asStateFlow()

    private val _selectedVideoFormatIndex = MutableStateFlow(0)
    val selectedVideoFormatIndex: StateFlow<Int> = _selectedVideoFormatIndex.asStateFlow()

    private val _selectedAudioFormatId = MutableStateFlow("auto")
    val selectedAudioFormatId: StateFlow<String> = _selectedAudioFormatId.asStateFlow()

    private val _selectedPlaylistItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedPlaylistItems: StateFlow<Set<String>> = _selectedPlaylistItems.asStateFlow()

    val downloadJobs: StateFlow<List<DownloadJob>> = repository.allDownloadJobs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val activeJobCount: StateFlow<Int> = repository.activeJobCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )

    private val _savedVideos = MutableStateFlow<List<SavedVideoFile>>(emptyList())
    val savedVideos: StateFlow<List<SavedVideoFile>> = _savedVideos.asStateFlow()

    // Setup State
    private val _ytDlpStatus = MutableStateFlow(AmberEngine.getYtDlpVersion())
    val ytDlpStatus: StateFlow<String> = _ytDlpStatus.asStateFlow()

    private val _isUpdatingYtDlp = MutableStateFlow(false)
    val isUpdatingYtDlp: StateFlow<Boolean> = _isUpdatingYtDlp.asStateFlow()

    // Message Toast / Snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var progressJob: Job? = null

    init {
        initMediaController()
        loadHomeShelves()
        refreshDownloads()
        refreshSavedVideos()
        startProgressTracker()

        // Restore saved queue from database
        viewModelScope.launch {
            repository.queueTracks.collect { savedQueue ->
                if (_queue.value.isEmpty() && savedQueue.isNotEmpty()) {
                    _queue.value = savedQueue
                    if (_currentIndex.value < 0) {
                        _currentIndex.value = 0
                        _currentTrack.value = savedQueue.first()
                        extractPalette(savedQueue.first().thumbnail)
                    }
                }
            }
        }
    }

    fun selectTab(tab: AmberTab) {
        _currentTab.value = tab
        if (tab == AmberTab.LIBRARY) {
            refreshDownloads()
        } else if (tab == AmberTab.DOWNLOAD) {
            refreshSavedVideos()
        }
    }

    fun showToast(message: String) {
        _userMessage.value = message
    }

    fun clearToast() {
        _userMessage.value = null
    }

    // MediaController Initialization
    private fun initMediaController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
            } catch (e: Exception) {
                Log.e("AmberViewModel", "Failed to connect to MediaSession", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        val player = mediaController ?: return
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_ENDED) {
                    onTrackEnded()
                }
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateTrackMetadata(mediaItem)
            }
        })

        _isPlaying.value = player.isPlaying
    }

    private fun updateTrackMetadata(mediaItem: MediaItem?) {
        val id = mediaItem?.mediaId ?: return
        val current = _queue.value.find { it.id == id } ?: _currentTrack.value
        if (current != null) {
            _currentTrack.value = current
            extractPalette(current.thumbnail)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    _currentPositionMs.value = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration
                    if (dur > 0L) {
                        _durationMs.value = dur
                    }
                }
                delay(400)
            }
        }
    }

    // Playback Actions
    fun playTrack(track: Track, newQueue: Boolean = true) {
        viewModelScope.launch {
            if (newQueue) {
                _queue.value = listOf(track)
                _currentIndex.value = 0
                repository.updateQueue(listOf(track))
            } else {
                val existingIndex = _queue.value.indexOfFirst { it.id == track.id }
                if (existingIndex >= 0) {
                    _currentIndex.value = existingIndex
                } else {
                    val at = (_currentIndex.value + 1).coerceIn(0, _queue.value.size)
                    val updated = _queue.value.toMutableList().apply { add(at, track) }
                    _queue.value = updated
                    _currentIndex.value = at
                    repository.updateQueue(updated)
                }
            }

            _currentTrack.value = track
            repository.addToRecent(track)
            extractPalette(track.thumbnail)
            loadStreamAndPlay(track)

            // Warm up next track in queue
            val nextTrack = _queue.value.getOrNull(_currentIndex.value + 1)
            if (nextTrack != null && nextTrack.localPath == null) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        AmberEngine.getStreamUrl(nextTrack.id)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun loadStreamAndPlay(track: Track) {
        viewModelScope.launch {
            _isBuffering.value = true
            try {
                val streamUrl = if (track.localPath != null && java.io.File(track.localPath).exists()) {
                    Uri.fromFile(java.io.File(track.localPath)).toString()
                } else {
                    AmberEngine.getStreamUrl(track.id)
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(track.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setArtworkUri(Uri.parse(track.thumbnail))
                            .build()
                    )
                    .build()

                mediaController?.let { player ->
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            } catch (e: Exception) {
                Log.e("AmberViewModel", "Stream playback error", e)
                showToast("Could not play stream: ${e.message?.take(80)}")
                _isBuffering.value = false
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaController
        if (player == null) {
            val track = _currentTrack.value ?: _queue.value.firstOrNull()
            if (track != null) playTrack(track, newQueue = false)
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val nextIdx = if (_isShuffle.value && q.size > 1) {
            var randomIdx: Int
            do {
                randomIdx = (q.indices).random()
            } while (randomIdx == _currentIndex.value)
            randomIdx
        } else {
            val target = _currentIndex.value + 1
            if (target >= q.size) {
                if (_repeatMode.value == 1) 0 else {
                    if (preferences.value.autoplay) {
                        triggerAutoQueue()
                    }
                    return
                }
            } else target
        }
        _currentIndex.value = nextIdx
        val track = q[nextIdx]
        _currentTrack.value = track
        loadStreamAndPlay(track)
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        if (_currentPositionMs.value > 4000L) {
            seekTo(0)
            return
        }
        val prevIdx = if (_currentIndex.value > 0) _currentIndex.value - 1 else q.size - 1
        _currentIndex.value = prevIdx
        val track = q[prevIdx]
        _currentTrack.value = track
        loadStreamAndPlay(track)
    }

    private fun onTrackEnded() {
        if (_repeatMode.value == 2) { // Repeat one
            seekTo(0)
            mediaController?.play()
            return
        }
        skipNext()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        showToast(if (_isShuffle.value) "Shuffle on" else "Shuffle off")
    }

    fun cycleRepeatMode() {
        val nextMode = (_repeatMode.value + 1) % 3
        _repeatMode.value = nextMode
        val labels = listOf("Repeat off", "Repeat all", "Repeat one")
        showToast(labels[nextMode])
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val cur = _playbackSpeed.value
        val next = speeds.firstOrNull { it > cur } ?: speeds.first()
        _playbackSpeed.value = next
        mediaController?.setPlaybackSpeed(next)
        prefsRepo.setPlaybackSpeed(next)
        showToast("${next}× speed")
    }

    fun toggleLikeCurrentTrack() {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            repository.toggleLiked(track)
        }
    }

    fun toggleLikeTrack(track: Track) {
        viewModelScope.launch {
            repository.toggleLiked(track)
        }
    }

    fun addToQueue(track: Track) {
        val updated = _queue.value.toMutableList().apply { add(track) }
        _queue.value = updated
        viewModelScope.launch {
            repository.updateQueue(updated)
        }
        showToast("Added to queue")
    }

    fun playNext(track: Track) {
        val at = (_currentIndex.value + 1).coerceIn(0, _queue.value.size)
        val updated = _queue.value.toMutableList().apply { add(at, track) }
        _queue.value = updated
        viewModelScope.launch {
            repository.updateQueue(updated)
        }
        showToast("Playing next")
    }

    fun removeFromQueue(index: Int) {
        if (index !in _queue.value.indices) return
        val updated = _queue.value.toMutableList().apply { removeAt(index) }
        _queue.value = updated
        if (index < _currentIndex.value) {
            _currentIndex.value--
        } else if (index == _currentIndex.value) {
            if (updated.isNotEmpty()) {
                val nextIdx = index.coerceAtMost(updated.size - 1)
                _currentIndex.value = nextIdx
                _currentTrack.value = updated[nextIdx]
                loadStreamAndPlay(updated[nextIdx])
            } else {
                _currentIndex.value = -1
                _currentTrack.value = null
                mediaController?.stop()
            }
        }
        viewModelScope.launch {
            repository.updateQueue(updated)
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        _currentTrack.value = null
        mediaController?.stop()
        viewModelScope.launch {
            repository.clearQueue()
        }
    }

    fun clearRecent() {
        viewModelScope.launch {
            repository.clearRecent()
        }
    }

    fun triggerAutoQueue() {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            showToast("Fetching similar songs…")
            val related = AmberEngine.getRelatedTracks(track.id, limit = 8)
            if (related.isNotEmpty()) {
                val existingIds = _queue.value.map { it.id }.toSet()
                val newItems = related.filter { !existingIds.contains(it.id) }
                if (newItems.isNotEmpty()) {
                    val updated = _queue.value + newItems
                    _queue.value = updated
                    repository.updateQueue(updated)
                    showToast("Added ${newItems.size} similar tracks")
                } else {
                    showToast("No new similar songs found")
                }
            } else {
                showToast("Could not find similar songs")
            }
        }
    }

    // Dynamic Palette Hue extraction
    private fun extractPalette(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) {
            _paletteAccentColor.value = AmberAccentDefault
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(getApplication())
                val request = ImageRequest.Builder(getApplication())
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val vibrant = palette.getVibrantColor(palette.getDominantColor(0))
                    if (vibrant != 0) {
                        _paletteAccentColor.value = Color(vibrant)
                    }
                }
            } catch (e: Exception) {
                Log.w("AmberViewModel", "Palette extraction failed", e)
            }
        }
    }

    // Home Shelves Loading
    fun loadHomeShelves() {
        viewModelScope.launch {
            _isHomeLoading.value = true
            val seedTrack = _currentTrack.value?.id ?: "4NRXx6U8ABQ"
            val shelvesList = mutableListOf(
                HomeShelf("picks", "Quick Picks", "Recommended based on your taste", emptyList(), isLoading = true),
                HomeShelf("hits", "Top Global Hits", "Popular music charts", emptyList(), isLoading = true),
                HomeShelf("lofi", "Lo-Fi & Chill", "Relaxing beats & study focus", emptyList(), isLoading = true),
                HomeShelf("ambient", "Ambient & Focus", "Atmospheric instrumental music", emptyList(), isLoading = true)
            )
            _shelves.value = shelvesList

            withContext(Dispatchers.IO) {
                val seeds = listOf(
                    seedTrack to 0,
                    "hT_nvWreIhg" to 1,
                    "jfKfPfyJRdk" to 2,
                    "DWCJZRYemXo" to 3
                )
                for ((seed, idx) in seeds) {
                    val tracks = AmberEngine.getRelatedTracks(seed, limit = 8)
                    val updated = _shelves.value.toMutableList()
                    if (idx < updated.size) {
                        updated[idx] = updated[idx].copy(tracks = tracks, isLoading = false)
                        _shelves.value = updated
                    }
                }
            }
            _isHomeLoading.value = false
        }
    }

    // Browse Actions
    fun setBrowseQuery(q: String) {
        _browseQuery.value = q
    }

    fun setBrowseFilter(f: String) {
        _browseFilter.value = f
        if (_browseQuery.value.isNotEmpty()) {
            executeSearch()
        }
    }

    fun executeSearch() {
        val query = _browseQuery.value.trim()
        if (query.isEmpty()) return

        if (AmberEngine.extractVideoId(query) != null || query.contains("youtube.com") || query.contains("youtu.be")) {
            // Direct link pasted in Browse
            fetchAndHandleUrl(query)
            return
        }

        viewModelScope.launch {
            _isBrowseLoading.value = true
            val results = withContext(Dispatchers.IO) {
                if (preferences.value.browseMode == "ytm") {
                    AmberEngine.searchYtMusic(query)
                } else {
                    AmberEngine.searchVideos(query, _browseFilter.value)
                }
            }
            _browseResults.value = results
            _isBrowseLoading.value = false
        }
    }

    private fun fetchAndHandleUrl(url: String) {
        viewModelScope.launch {
            _isBrowseLoading.value = true
            try {
                if (AmberEngine.isPlaylistUrl(url)) {
                    val pl = AmberEngine.getPlaylistDetails(url)
                    val tracks = pl.items.map {
                        Track(id = it.id, title = it.title, artist = it.channel, duration = it.duration, thumbnail = it.thumbnail, isLive = it.isLive)
                    }
                    if (tracks.isNotEmpty()) {
                        _queue.value = tracks
                        _currentIndex.value = 0
                        repository.updateQueue(tracks)
                        playTrack(tracks.first(), newQueue = false)
                        selectTab(AmberTab.PLAYER)
                        showToast("Loaded ${tracks.size} tracks from playlist")
                    }
                } else {
                    val details = AmberEngine.getVideoDetails(url)
                    val track = Track(
                        id = details.id,
                        title = details.title,
                        artist = details.channel,
                        duration = details.duration,
                        seconds = details.seconds,
                        thumbnail = details.thumbnail,
                        isLive = details.isLive
                    )
                    playTrack(track)
                    selectTab(AmberTab.PLAYER)
                }
            } catch (e: Exception) {
                showToast("Could not load link: ${e.message?.take(80)}")
            } finally {
                _isBrowseLoading.value = false
            }
        }
    }

    // Library Actions
    fun setLibraryFilter(f: String) {
        _libraryFilter.value = f
    }

    fun refreshDownloads() {
        viewModelScope.launch {
            _downloadedTracks.value = repository.scanDownloadedAudio()
        }
    }

    fun deleteDownloadedTrack(track: Track) {
        val path = track.localPath ?: return
        viewModelScope.launch {
            repository.deleteLocalFile(path)
            refreshDownloads()
            showToast("Deleted ${track.title}")
        }
    }

    // Download Tab Actions
    fun setDownloadUrlInput(url: String) {
        _downloadUrlInput.value = url
    }

    fun fetchDownloadTarget() {
        val url = _downloadUrlInput.value.trim()
        if (url.isEmpty()) {
            showToast("Please paste a YouTube link first")
            return
        }
        viewModelScope.launch {
            _isFetchingDetails.value = true
            _fetchError.value = null
            _fetchedVideo.value = null
            _fetchedPlaylist.value = null

            try {
                if (AmberEngine.isPlaylistUrl(url)) {
                    val pl = AmberEngine.getPlaylistDetails(url)
                    _fetchedPlaylist.value = pl
                    _selectedPlaylistItems.value = pl.items.filter { !it.isLive }.map { it.id }.toSet()
                } else {
                    val details = AmberEngine.getVideoDetails(url)
                    _fetchedVideo.value = details
                    _selectedVideoFormatIndex.value = 0
                }
            } catch (e: Exception) {
                _fetchError.value = e.message?.take(110) ?: "Failed to fetch link details"
            } finally {
                _isFetchingDetails.value = false
            }
        }
    }

    fun setDownloadTabMode(mode: String) {
        _downloadTabMode.value = mode
        prefsRepo.setDownloadMode(mode)
    }

    fun setSelectedVideoFormat(index: Int) {
        _selectedVideoFormatIndex.value = index
    }

    fun setSelectedAudioFormat(formatId: String) {
        _selectedAudioFormatId.value = formatId
    }

    fun togglePlaylistItemSelection(id: String) {
        val current = _selectedPlaylistItems.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedPlaylistItems.value = current
    }

    fun selectAllPlaylistItems(selectAll: Boolean) {
        val pl = _fetchedPlaylist.value ?: return
        if (selectAll) {
            _selectedPlaylistItems.value = pl.items.filter { !it.isLive }.map { it.id }.toSet()
        } else {
            _selectedPlaylistItems.value = emptySet()
        }
    }

    fun startSingleVideoDownload() {
        val video = _fetchedVideo.value ?: return
        val isAudio = _downloadTabMode.value == "audio"
        val vFormat = video.videos.getOrNull(_selectedVideoFormatIndex.value)

        val job = DownloadJob(
            id = UUID.randomUUID().toString(),
            videoId = video.id,
            title = video.title,
            thumbnail = video.thumbnail,
            label = if (isAudio) "${preferences.value.audioFormat.uppercase()} audio" else "${vFormat?.height ?: 720}p ${vFormat?.codec ?: "video"}",
            kind = if (isAudio) "audio" else "video",
            audioFormat = preferences.value.audioFormat,
            videoFormatId = vFormat?.id ?: "",
            audioFormatId = if (_selectedAudioFormatId.value == "auto") "" else _selectedAudioFormatId.value,
            height = vFormat?.height ?: 0,
            container = preferences.value.container,
            status = JobStatus.QUEUED,
            bytesTotal = if (isAudio) (video.seconds * 125 * 192) else (vFormat?.size ?: 0L)
        )

        viewModelScope.launch {
            repository.addDownloadJob(job)
            DownloadService.start(getApplication())
            showToast("Added to downloads")
        }
    }

    fun startPlaylistDownload() {
        val pl = _fetchedPlaylist.value ?: return
        val selectedIds = _selectedPlaylistItems.value
        val itemsToDownload = pl.items.filter { selectedIds.contains(it.id) }
        if (itemsToDownload.isEmpty()) {
            showToast("Select at least one video to download")
            return
        }

        val isAudio = _downloadTabMode.value == "audio"
        viewModelScope.launch {
            for (item in itemsToDownload) {
                val job = DownloadJob(
                    id = UUID.randomUUID().toString(),
                    videoId = item.id,
                    title = item.title,
                    thumbnail = item.thumbnail,
                    label = if (isAudio) "${preferences.value.audioFormat.uppercase()} audio" else "Best video",
                    kind = if (isAudio) "audio" else "video",
                    audioFormat = preferences.value.audioFormat,
                    container = preferences.value.container,
                    status = JobStatus.QUEUED
                )
                repository.addDownloadJob(job)
            }
            DownloadService.start(getApplication())
            showToast("Added ${itemsToDownload.size} items to downloads")
        }
    }

    fun pauseDownloadJob(jobId: String) {
        DownloadService.pause(getApplication(), jobId)
    }

    fun cancelDownloadJob(jobId: String) {
        DownloadService.cancel(getApplication(), jobId)
    }

    fun retryDownloadJob(jobId: String) {
        DownloadService.retry(getApplication(), jobId)
    }

    fun dismissDownloadJob(jobId: String) {
        viewModelScope.launch {
            repository.deleteDownloadJob(jobId)
        }
    }

    fun clearFinishedJobs() {
        viewModelScope.launch {
            repository.clearFinishedJobs()
        }
    }

    fun refreshSavedVideos() {
        viewModelScope.launch {
            _savedVideos.value = repository.scanDownloadedVideos()
        }
    }

    fun deleteSavedVideo(filePath: String) {
        viewModelScope.launch {
            repository.deleteLocalFile(filePath)
            refreshSavedVideos()
            showToast("Deleted video file")
        }
    }

    // Setup Actions
    fun updateYtDlp() {
        viewModelScope.launch {
            _isUpdatingYtDlp.value = true
            val (ok, msg) = AmberEngine.updateYtDlp(getApplication())
            _isUpdatingYtDlp.value = false
            _ytDlpStatus.value = AmberEngine.getYtDlpVersion()
            showToast(msg)
        }
    }

    fun setAudioFormat(format: String) = prefsRepo.setAudioFormat(format)
    fun setEmbedMeta(enabled: Boolean) = prefsRepo.setEmbedMeta(enabled)
    fun setSubtitles(enabled: Boolean) = prefsRepo.setSubtitles(enabled)
    fun setChapters(enabled: Boolean) = prefsRepo.setChapters(enabled)
    fun setAutoplay(enabled: Boolean) = prefsRepo.setAutoplay(enabled)
    fun setAmbientGlow(enabled: Boolean) = prefsRepo.setAmbientGlow(enabled)
    fun setLowPower(enabled: Boolean) = prefsRepo.setLowPower(enabled)
    fun setPerformance(enabled: Boolean) = prefsRepo.setPerformance(enabled)
    fun setContainer(container: String) = prefsRepo.setContainer(container)
    fun setVideoPreset(preset: Int) = prefsRepo.setVideoPreset(preset)
    fun setYoutubeApiKey(key: String) = prefsRepo.setYoutubeApiKey(key)
    fun setBrowseMode(mode: String) = prefsRepo.setBrowseMode(mode)

    override fun onCleared() {
        progressJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
