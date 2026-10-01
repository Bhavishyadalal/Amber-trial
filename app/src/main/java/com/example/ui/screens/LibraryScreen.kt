package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.components.TrackRow
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberFaint
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.AmberLine
import com.example.ui.theme.AmberText
import com.example.ui.theme.LocalAmberTheme
import com.example.ui.viewmodel.AmberViewModel

@Composable
fun LibraryScreen(
    viewModel: AmberViewModel,
    modifier: Modifier = Modifier
) {
    val likedTracks by viewModel.likedTracks.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val filter by viewModel.libraryFilter.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val theme = LocalAmberTheme.current

    val filteredLiked = likedTracks.filter {
        filter.isEmpty() || it.title.contains(filter, ignoreCase = true) || it.artist.contains(filter, ignoreCase = true)
    }

    val filteredDownloaded = downloadedTracks.filter {
        filter.isEmpty() || it.title.contains(filter, ignoreCase = true) || it.artist.contains(filter, ignoreCase = true)
    }

    val filteredRecent = recentTracks.filter {
        filter.isEmpty() || it.title.contains(filter, ignoreCase = true) || it.artist.contains(filter, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Filter input field
            OutlinedTextField(
                value = filter,
                onValueChange = { viewModel.setLibraryFilter(it) },
                placeholder = { Text("Filter library…", color = AmberFaint, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Filter",
                        tint = AmberDim
                    )
                },
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setLibraryFilter("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AmberFaint)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmberGlassRaised,
                    unfocusedContainerColor = AmberGlassRaised,
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = AmberLine,
                    focusedTextColor = AmberText,
                    unfocusedTextColor = AmberText
                ),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section: Liked Songs
        item {
            LibrarySectionHeader(
                title = "Liked",
                count = filteredLiked.size,
                actionLabel = "Play all",
                actionIcon = Icons.Default.PlayArrow,
                onAction = {
                    if (filteredLiked.isNotEmpty()) {
                        viewModel.playTrack(filteredLiked.first(), newQueue = true)
                        filteredLiked.drop(1).forEach { viewModel.addToQueue(it) }
                    }
                }
            )
        }

        if (filteredLiked.isEmpty()) {
            item {
                EmptyLibraryNotice(text = if (filter.isEmpty()) "No liked tracks yet. Tap heart on any song." else "No matches found.")
            }
        } else {
            items(filteredLiked, key = { "liked_${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    isPlaying = currentTrack?.id == track.id,
                    onClick = { viewModel.playTrack(track) },
                    onPlayNext = { viewModel.playNext(track) },
                    onAddToQueue = { viewModel.addToQueue(track) },
                    onToggleLike = { viewModel.toggleLikeTrack(track) },
                    isLiked = true
                )
            }
        }

        // Section: Downloaded Songs (Offline)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.titleLarge,
                        color = AmberText
                    )
                    if (filteredDownloaded.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = theme.accent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = filteredDownloaded.size.toString(),
                                color = theme.accentLight,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filteredDownloaded.isNotEmpty()) {
                        Text(
                            text = "Play all",
                            color = theme.accentLight,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .clickable {
                                    viewModel.playTrack(filteredDownloaded.first(), newQueue = true)
                                    filteredDownloaded.drop(1).forEach { viewModel.addToQueue(it) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Text(
                            text = "Shuffle",
                            color = theme.accentLight,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .clickable {
                                    val shuffled = filteredDownloaded.shuffled()
                                    viewModel.playTrack(shuffled.first(), newQueue = true)
                                    shuffled.drop(1).forEach { viewModel.addToQueue(it) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (filteredDownloaded.isEmpty()) {
            item {
                EmptyLibraryNotice(text = if (filter.isEmpty()) "No downloaded tracks yet. Save songs in Player or Download tab." else "No matches found.")
            }
        } else {
            items(filteredDownloaded, key = { "dl_${it.id}_${it.localPath}" }) { track ->
                TrackRow(
                    track = track,
                    isPlaying = currentTrack?.id == track.id,
                    onClick = { viewModel.playTrack(track) },
                    onPlayNext = { viewModel.playNext(track) },
                    onAddToQueue = { viewModel.addToQueue(track) },
                    onRemove = { viewModel.deleteDownloadedTrack(track) }
                )
            }
        }

        // Section: Recently Played
        item {
            LibrarySectionHeader(
                title = "Recently played",
                count = filteredRecent.size,
                actionLabel = "Clear",
                actionIcon = Icons.Default.Clear,
                onAction = { viewModel.clearRecent() }
            )
        }

        if (filteredRecent.isEmpty()) {
            item {
                EmptyLibraryNotice(text = "No recently played tracks.")
            }
        } else {
            items(filteredRecent, key = { "rec_${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    isPlaying = currentTrack?.id == track.id,
                    onClick = { viewModel.playTrack(track) },
                    onPlayNext = { viewModel.playNext(track) },
                    onAddToQueue = { viewModel.addToQueue(track) },
                    onToggleLike = { viewModel.toggleLikeTrack(track) }
                )
            }
        }
    }
}

@Composable
fun LibrarySectionHeader(
    title: String,
    count: Int,
    actionLabel: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit
) {
    val theme = LocalAmberTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = AmberText
            )
            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = theme.accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = count.toString(),
                        color = theme.accentLight,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (count > 0) {
            Text(
                text = actionLabel,
                color = theme.accentLight,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyLibraryNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AmberFaint,
            fontSize = 13.sp
        )
    }
}
