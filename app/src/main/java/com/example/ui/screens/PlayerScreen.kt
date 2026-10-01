package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.engine.AmberEngine
import com.example.ui.components.AmberBottomNav
import com.example.ui.components.AmberTab
import com.example.ui.components.AmberThumb
import com.example.ui.components.TrackRow
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberEmber
import com.example.ui.theme.AmberFaint
import com.example.ui.theme.AmberGlassCardColor
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.AmberLine
import com.example.ui.theme.AmberLineHighlight
import com.example.ui.theme.AmberSage
import com.example.ui.theme.AmberText
import com.example.ui.theme.LocalAmberTheme
import com.example.ui.viewmodel.AmberViewModel

@Composable
fun PlayerScreen(
    viewModel: AmberViewModel,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPosMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val theme = LocalAmberTheme.current
    val context = LocalContext.current

    var selectedPane by remember { mutableIntStateOf(0) } // 0: Now Playing, 1: Up Next
    val isCurrentLiked = currentTrack?.let { ct -> likedTracks.any { it.id == ct.id } } ?: false

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Two-pane segmented toggle (Now Playing | Up Next)
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(99.dp)),
            shape = RoundedCornerShape(99.dp),
            color = AmberGlassRaised,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberLineHighlight)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pane0Mod = if (selectedPane == 0) {
                    Modifier.background(Brush.linearGradient(listOf(theme.accentLight, theme.accent)))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .then(pane0Mod)
                        .clickable { selectedPane = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Now playing",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPane == 0) Color(0xFF1D110B) else AmberDim
                    )
                }

                val pane1Mod = if (selectedPane == 1) {
                    Modifier.background(Brush.linearGradient(listOf(theme.accentLight, theme.accent)))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .then(pane1Mod)
                        .clickable { selectedPane = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Up next (${queue.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPane == 1) Color(0xFF1D110B) else AmberDim
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPane == 0) {
            // NOW PLAYING PANE
            NowPlayingPane(
                track = currentTrack,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPosMs = currentPosMs,
                durationMs = durationMs,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                speed = speed,
                isLiked = isCurrentLiked,
                onTogglePlay = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onCycleSpeed = { viewModel.cyclePlaybackSpeed() },
                onToggleLike = { viewModel.toggleLikeCurrentTrack() },
                onDownload = {
                    currentTrack?.let {
                        viewModel.startSingleVideoDownload()
                    }
                },
                onShare = {
                    currentTrack?.let { ct ->
                        val url = "https://www.youtube.com/watch?v=${ct.id}"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Amber Link", url)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showToast("Link copied to clipboard")
                    }
                },
                onChannelClick = {
                    currentTrack?.let {
                        viewModel.setBrowseQuery(it.artist)
                        viewModel.selectTab(AmberTab.BROWSE)
                        viewModel.executeSearch()
                    }
                }
            )
        } else {
            // UP NEXT QUEUE PANE
            UpNextPane(
                queue = queue,
                currentIndex = currentIndex,
                onTrackClick = { track, index ->
                    viewModel.playTrack(track, newQueue = false)
                },
                onRemove = { index ->
                    viewModel.removeFromQueue(index)
                },
                onAutoQueue = {
                    viewModel.triggerAutoQueue()
                },
                onClear = {
                    viewModel.clearQueue()
                }
            )
        }
    }
}

@Composable
fun NowPlayingPane(
    track: Track?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: Int,
    speed: Float,
    isLiked: Boolean,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onChannelClick: () -> Unit
) {
    val theme = LocalAmberTheme.current
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large album art with ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (theme.isAmbientGlow) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = theme.accent,
                            spotColor = theme.accent
                        )
                )
            }

            AmberThumb(
                url = track?.thumbnail,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, AmberLineHighlight, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp)
            )

            if (isBuffering) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = theme.accent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title and channel
        Text(
            text = track?.title ?: "Nothing playing",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            color = AmberText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = track?.artist?.ifEmpty { "Search or paste a link in Browse" } ?: "Select a song to start",
            style = MaterialTheme.typography.bodyLarge.copy(color = theme.accentLight),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(enabled = track != null, onClick = onChannelClick)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Seek Bar
        val effectiveDuration = if (durationMs > 0) durationMs else (track?.seconds?.times(1000) ?: 0L)
        val currentProgress = if (isDraggingSlider) {
            dragPositionMs
        } else {
            if (effectiveDuration > 0) (currentPosMs.toFloat() / effectiveDuration) else 0f
        }

        Slider(
            value = currentProgress.coerceIn(0f, 1f),
            onValueChange = {
                isDraggingSlider = true
                dragPositionMs = it
            },
            onValueChangeFinished = {
                val targetMs = (dragPositionMs * effectiveDuration).toLong()
                onSeek(targetMs)
                isDraggingSlider = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = theme.accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            ),
            modifier = Modifier.fillMaxWidth(0.95f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayCur = if (isDraggingSlider) (dragPositionMs * effectiveDuration).toLong() else currentPosMs
            Text(
                text = AmberEngine.formatDuration(displayCur / 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = AmberFaint
            )
            Text(
                text = if (track?.isLive == true) "LIVE" else AmberEngine.formatDuration(effectiveDuration / 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = if (track?.isLive == true) AmberEmber else AmberFaint
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main controls (Shuffle, Previous, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) theme.accent else AmberDim,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onSkipPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = AmberText,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Big 72dp play/pause button with glowing gradient
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(theme.accentLight, theme.accent, theme.accentDark)
                        )
                    )
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF1D110B),
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = AmberText,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode > 0) theme.accent else AmberDim,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tools Row (Speed, Like, Download, Share)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = AmberGlassRaised,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberLine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed pill button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(theme.accent.copy(alpha = 0.16f))
                        .clickable(onClick = onCycleSpeed)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${speed}×",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.accentLight
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) theme.accent else AmberDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download audio",
                            tint = AmberDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = AmberDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpNextPane(
    queue: List<Track>,
    currentIndex: Int,
    onTrackClick: (Track, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAutoQueue: () -> Unit,
    onClear: () -> Unit
) {
    val theme = LocalAmberTheme.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Queue (${queue.size})",
                style = MaterialTheme.typography.titleLarge,
                color = AmberText
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = theme.accent.copy(alpha = 0.14f),
                    modifier = Modifier.clickable(onClick = onAutoQueue)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = theme.accentLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Auto Queue",
                            color = theme.accentLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = AmberGlassRaised,
                    modifier = Modifier.clickable(onClick = onClear)
                ) {
                    Text(
                        text = "Clear",
                        color = AmberDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty\nSearch or pick a song to start",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AmberFaint,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(queue, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    TrackRow(
                        track = track,
                        isPlaying = index == currentIndex,
                        onClick = { onTrackClick(track, index) },
                        onRemove = { onRemove(index) }
                    )
                }
            }
        }
    }
}
