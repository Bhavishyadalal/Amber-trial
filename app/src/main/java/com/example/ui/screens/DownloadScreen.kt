package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DownloadJob
import com.example.data.model.JobStatus
import com.example.data.model.VideoDetails
import com.example.data.model.VideoFormat
import com.example.data.repository.SavedVideoFile
import com.example.engine.AmberEngine
import com.example.ui.components.AmberGlassCard
import com.example.ui.components.AmberPrimaryButton
import com.example.ui.components.AmberThumb
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
import java.io.File

@Composable
fun DownloadScreen(
    viewModel: AmberViewModel,
    modifier: Modifier = Modifier
) {
    val urlInput by viewModel.downloadUrlInput.collectAsState()
    val isFetching by viewModel.isFetchingDetails.collectAsState()
    val fetchError by viewModel.fetchError.collectAsState()
    val fetchedVideo by viewModel.fetchedVideo.collectAsState()
    val fetchedPlaylist by viewModel.fetchedPlaylist.collectAsState()
    val downloadMode by viewModel.downloadTabMode.collectAsState()
    val selectedFormatIdx by viewModel.selectedVideoFormatIndex.collectAsState()
    val selectedAudioFmtId by viewModel.selectedAudioFormatId.collectAsState()
    val selectedPlaylistItems by viewModel.selectedPlaylistItems.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val downloadJobs by viewModel.downloadJobs.collectAsState()
    val savedVideos by viewModel.savedVideos.collectAsState()
    val theme = LocalAmberTheme.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    val hasActiveJob = downloadJobs.any { it.status == JobStatus.QUEUED || it.status == JobStatus.DOWNLOADING || it.status == JobStatus.PAUSED }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Download",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                    color = AmberText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Works with videos, Shorts, playlists and channels. Save high-res video or direct audio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberDim
                )
            }

            // Link Input & Fetch Box
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AmberGlassRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberLineHighlight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = theme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { viewModel.setDownloadUrlInput(it) },
                            placeholder = { Text("Paste a YouTube link", color = AmberFaint, fontSize = 13.5.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = AmberText,
                                unfocusedTextColor = AmberText
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                keyboard?.hide()
                                viewModel.fetchDownloadTarget()
                            }),
                            modifier = Modifier.weight(1f)
                        )

                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setDownloadUrlInput("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AmberFaint, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Text(
                                text = "Paste",
                                color = theme.accentLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val t = clip.getItemAt(0).text?.toString() ?: ""
                                            if (t.isNotEmpty()) {
                                                viewModel.setDownloadUrlInput(t)
                                                viewModel.fetchDownloadTarget()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        AmberPrimaryButton(
                            onClick = {
                                keyboard?.hide()
                                viewModel.fetchDownloadTarget()
                            },
                            enabled = !isFetching && urlInput.isNotEmpty()
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    color = Color(0xFF1D110B),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text("Fetch", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1D110B))
                            }
                        }
                    }
                }
            }

            if (fetchError != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AmberEmber.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberEmber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fetchError ?: "",
                            color = AmberEmber,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Fetched Single Video Options Card
            if (fetchedVideo != null) {
                item {
                    val video = fetchedVideo!!
                    SingleVideoCard(
                        video = video,
                        downloadMode = downloadMode,
                        selectedFormatIndex = selectedFormatIdx,
                        selectedAudioFmt = preferences.audioFormat,
                        selectedContainer = preferences.container,
                        onModeChange = { viewModel.setDownloadTabMode(it) },
                        onFormatSelect = { viewModel.setSelectedVideoFormat(it) },
                        onAudioFormatSelect = { viewModel.setAudioFormat(it) },
                        onContainerSelect = { viewModel.setContainer(it) }
                    )
                }
            }

            // Fetched Playlist / Channel Checkbox Picker
            if (fetchedPlaylist != null) {
                item {
                    val pl = fetchedPlaylist!!
                    PlaylistPickerCard(
                        playlist = pl,
                        downloadMode = downloadMode,
                        selectedIds = selectedPlaylistItems,
                        onModeChange = { viewModel.setDownloadTabMode(it) },
                        onToggleItem = { viewModel.togglePlaylistItemSelection(it) },
                        onSelectAll = { viewModel.selectAllPlaylistItems(true) },
                        onSelectNone = { viewModel.selectAllPlaylistItems(false) }
                    )
                }
            }

            // In Progress Section
            if (downloadJobs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "In progress",
                                style = MaterialTheme.typography.titleLarge,
                                color = AmberText
                            )
                            val activeCount = downloadJobs.count { it.status == JobStatus.DOWNLOADING || it.status == JobStatus.QUEUED }
                            if (activeCount > 0) {
                                Surface(shape = RoundedCornerShape(99.dp), color = theme.accent.copy(alpha = 0.16f)) {
                                    Text(
                                        text = activeCount.toString(),
                                        color = theme.accentLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Clear finished",
                            color = AmberDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .clickable { viewModel.clearFinishedJobs() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                items(downloadJobs, key = { it.id }) { job ->
                    DownloadJobRow(
                        job = job,
                        onPause = { viewModel.pauseDownloadJob(job.id) },
                        onCancel = { viewModel.cancelDownloadJob(job.id) },
                        onRetry = { viewModel.retryDownloadJob(job.id) },
                        onDismiss = { viewModel.dismissDownloadJob(job.id) },
                        onOpenFile = {
                            if (job.filePath.isNotEmpty()) {
                                openMediaFile(context, job.filePath, isVideo = job.kind == "video")
                            }
                        }
                    )
                }
            }

            // Saved Videos Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved videos (${savedVideos.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = AmberText
                    )
                }
            }

            if (savedVideos.isEmpty()) {
                item {
                    Text(
                        text = "No downloaded video files found in Movies/Amber yet.",
                        color = AmberFaint,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(savedVideos, key = { it.filePath }) { videoFile ->
                    SavedVideoRow(
                        file = videoFile,
                        onPlay = { openMediaFile(context, videoFile.filePath, isVideo = true) },
                        onDelete = { viewModel.deleteSavedVideo(videoFile.filePath) }
                    )
                }
            }

            // Legal Footer Notice
            item {
                Text(
                    text = "Personal use only. Downloading copies of YouTube content may breach YouTube's Terms of Service.",
                    color = AmberFaint,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                )
            }
        }

        // Sticky Bottom Ticket (Summary & Big Download Button)
        if (fetchedVideo != null || fetchedPlaylist != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 72.dp)
                    .clip(RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xF018111D),
                tonalElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberLineHighlight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (fetchedVideo != null) {
                            val v = fetchedVideo!!
                            val format = v.videos.getOrNull(selectedFormatIdx)
                            val isAudio = downloadMode == "audio"
                            Text(
                                text = if (isAudio) "${preferences.audioFormat.uppercase()} Audio" else "${format?.height ?: 720}p ${format?.codec ?: ""}",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                color = theme.accentLight
                            )
                            val sizeEstimate = if (isAudio) {
                                AmberEngine.formatDuration(v.seconds)
                            } else {
                                format?.size?.let { formatBytes(it) } ?: "~"
                            }
                            Text(
                                text = if (isAudio) "Direct tags & cover \u2022 $sizeEstimate" else "${preferences.container.uppercase()} \u2022 $sizeEstimate",
                                style = MaterialTheme.typography.bodyMedium.copy(color = AmberDim)
                            )
                        } else if (fetchedPlaylist != null) {
                            val count = selectedPlaylistItems.size
                            val isAudio = downloadMode == "audio"
                            Text(
                                text = if (isAudio) "Download $count Songs" else "Download $count Videos",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                color = theme.accentLight
                            )
                            Text(
                                text = if (isAudio) "${preferences.audioFormat.uppercase()} format" else "${preferences.container.uppercase()} video",
                                style = MaterialTheme.typography.bodyMedium.copy(color = AmberDim)
                            )
                        }
                    }

                    AmberPrimaryButton(
                        onClick = {
                            if (fetchedVideo != null) {
                                viewModel.startSingleVideoDownload()
                            } else {
                                viewModel.startPlaylistDownload()
                            }
                        },
                        enabled = if (fetchedPlaylist != null) selectedPlaylistItems.isNotEmpty() else true
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFF1D110B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1D110B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SingleVideoCard(
    video: VideoDetails,
    downloadMode: String,
    selectedFormatIndex: Int,
    selectedAudioFmt: String,
    selectedContainer: String,
    onModeChange: (String) -> Unit,
    onFormatSelect: (Int) -> Unit,
    onAudioFormatSelect: (String) -> Unit,
    onContainerSelect: (String) -> Unit
) {
    val theme = LocalAmberTheme.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AmberGlassRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Preview row
            Row(modifier = Modifier.fillMaxWidth()) {
                AmberThumb(
                    url = video.thumbnail,
                    size = 80.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${video.channel} \u2022 ${video.duration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Switch: Video | Audio only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(3.dp)
            ) {
                val videoMod = if (downloadMode == "video") {
                    Modifier.background(Brush.linearGradient(listOf(theme.accentLight, theme.accent)))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .then(videoMod)
                        .clickable { onModeChange("video") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Video",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (downloadMode == "video") Color(0xFF1D110B) else AmberDim
                    )
                }

                val audioMod = if (downloadMode == "audio") {
                    Modifier.background(Brush.linearGradient(listOf(theme.accentLight, theme.accent)))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .then(audioMod)
                        .clickable { onModeChange("audio") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Audio only",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (downloadMode == "audio") Color(0xFF1D110B) else AmberDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (downloadMode == "video") {
                // Video Qualities List
                Text(
                    text = "Resolution & Codec",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = AmberText
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(video.videos.indices.toList()) { idx ->
                        val fmt = video.videos[idx]
                        val isSelected = idx == selectedFormatIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) theme.accent.copy(alpha = 0.2f) else AmberGlassCardColor,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) theme.accent else AmberLine
                            ),
                            modifier = Modifier.clickable { onFormatSelect(idx) }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "${fmt.height}p${if (fmt.fps > 30) fmt.fps else ""}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) theme.accentLight else AmberText
                                )
                                Text(
                                    text = "${fmt.codec} \u2022 ${formatBytes(fmt.size)}",
                                    fontSize = 11.sp,
                                    color = AmberDim
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Container selector (Auto / MP4 / MKV)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Save as:", fontSize = 13.sp, color = AmberDim)
                    listOf("auto" to "Auto", "mp4" to "MP4", "mkv" to "MKV").forEach { (code, label) ->
                        val isSelected = selectedContainer == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) theme.accent else AmberGlassCardColor,
                            modifier = Modifier.clickable { onContainerSelect(code) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF1D110B) else AmberDim,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            } else {
                // Audio only: M4A, MP3, Opus Cards
                Text(
                    text = "Audio Format",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = AmberText
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf(
                        "m4a" to Pair("M4A (AAC)", "Native, universal"),
                        "mp3" to Pair("MP3", "192 kbps"),
                        "opus" to Pair("Opus", "Smallest file")
                    )

                    formats.forEach { (fmtCode, info) ->
                        val isSelected = selectedAudioFmt == fmtCode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAudioFormatSelect(fmtCode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) theme.accent.copy(alpha = 0.2f) else AmberGlassCardColor,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) theme.accent else AmberLine
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = info.first,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) theme.accentLight else AmberText
                                )
                                Text(
                                    text = info.second,
                                    fontSize = 10.5.sp,
                                    color = AmberDim
                                )
                                if (fmtCode == "opus") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AmberSage.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Smallest",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberSage,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Title, artist and album art are embedded inside the file. Songs appear in your Library.",
                    fontSize = 11.5.sp,
                    color = AmberFaint
                )
            }
        }
    }
}

@Composable
fun PlaylistPickerCard(
    playlist: com.example.data.model.PlaylistDetails,
    downloadMode: String,
    selectedIds: Set<String>,
    onModeChange: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit
) {
    val theme = LocalAmberTheme.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AmberGlassRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.count} items in playlist \u2022 ${selectedIds.size} selected",
                fontSize = 12.sp,
                color = AmberDim
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Select all / none buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberGlassCardColor,
                    modifier = Modifier.clickable(onClick = onSelectAll)
                ) {
                    Text("Select all", fontSize = 11.5.sp, color = theme.accentLight, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberGlassCardColor,
                    modifier = Modifier.clickable(onClick = onSelectNone)
                ) {
                    Text("None", fontSize = 11.5.sp, color = AmberDim, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                playlist.items.take(25).forEach { item ->
                    val isChecked = selectedIds.contains(item.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleItem(item.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleItem(item.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = theme.accent,
                                uncheckedColor = AmberDim
                            )
                        )
                        AmberThumb(url = item.thumbnail, size = 36.dp, shape = RoundedCornerShape(6.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AmberText)
                            Text(text = item.duration, fontSize = 11.sp, color = AmberFaint)
                        }
                    }
                }
                if (playlist.items.size > 25) {
                    Text(
                        text = "+ ${playlist.items.size - 25} more items",
                        fontSize = 11.5.sp,
                        color = AmberFaint,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadJobRow(
    job: DownloadJob,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit
) {
    val theme = LocalAmberTheme.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AmberGlassRaised,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (job.status) {
                JobStatus.DONE -> AmberSage.copy(alpha = 0.4f)
                JobStatus.ERROR -> AmberEmber.copy(alpha = 0.4f)
                else -> AmberLine
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AmberThumb(url = job.thumbnail, size = 44.dp, shape = RoundedCornerShape(8.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${job.label} \u2022 ${job.stage}",
                        fontSize = 11.5.sp,
                        color = when (job.status) {
                            JobStatus.DONE -> AmberSage
                            JobStatus.ERROR -> AmberEmber
                            else -> AmberDim
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (job.status) {
                        JobStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AmberDim, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AmberEmber, modifier = Modifier.size(18.dp))
                            }
                        }
                        JobStatus.PAUSED -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = theme.accentLight, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AmberEmber, modifier = Modifier.size(18.dp))
                            }
                        }
                        JobStatus.ERROR -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = theme.accentLight, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AmberDim, modifier = Modifier.size(18.dp))
                            }
                        }
                        JobStatus.DONE -> {
                            IconButton(onClick = onOpenFile, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Folder, contentDescription = "Open file", tint = AmberSage, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AmberDim, modifier = Modifier.size(18.dp))
                            }
                        }
                        else -> {
                            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AmberDim, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (job.status == JobStatus.DOWNLOADING || job.status == JobStatus.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (job.progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = theme.accent,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${job.progress.toInt()}%" + (if (job.speed > 0) " \u2022 ${formatBytes(job.speed)}/s" else ""),
                        fontSize = 11.sp,
                        color = AmberDim
                    )
                    if (job.eta > 0) {
                        Text(
                            text = "${AmberEngine.formatDuration(job.eta)} left",
                            fontSize = 11.sp,
                            color = AmberFaint
                        )
                    }
                }
            }

            if (job.error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = job.error,
                    fontSize = 11.5.sp,
                    color = AmberEmber,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SavedVideoRow(
    file: SavedVideoFile,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AmberGlassRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberLine),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmberThumb(url = file.thumbnail, size = 48.dp, shape = RoundedCornerShape(8.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.quality.ifEmpty { file.ext }} \u2022 ${formatBytes(file.sizeBytes)}",
                    fontSize = 11.sp,
                    color = AmberDim
                )
            }

            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = AmberText)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete video", tint = AmberDim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}

fun openMediaFile(context: Context, path: String, isVideo: Boolean) {
    try {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (isVideo) "video/*" else "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (_: Exception) {
        // Fallback direct intent
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(File(path)), if (isVideo) "video/*" else "audio/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
