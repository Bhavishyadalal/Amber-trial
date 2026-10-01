package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmberTab
import com.example.ui.components.TrackRow
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberFaint
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.AmberLine
import com.example.ui.theme.AmberLineHighlight
import com.example.ui.theme.AmberText
import com.example.ui.theme.LocalAmberTheme
import com.example.ui.viewmodel.AmberViewModel

@Composable
fun BrowseScreen(
    viewModel: AmberViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.browseQuery.collectAsState()
    val filter by viewModel.browseFilter.collectAsState()
    val results by viewModel.browseResults.collectAsState()
    val isLoading by viewModel.isBrowseLoading.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val theme = LocalAmberTheme.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val filterChips = listOf(
        "" to "All",
        "music" to "Music",
        "ambient" to "Ambient",
        "podcast" to "Podcasts",
        "lofi" to "Lo-fi"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 120.dp)
    ) {
        // Mode toggle: YouTube Music | YouTube
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(99.dp)),
            shape = RoundedCornerShape(99.dp),
            color = AmberGlassRaised,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberLineHighlight)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isYtm = preferences.browseMode == "ytm"

                val ytmMod = if (isYtm) {
                    Modifier.background(Brush.linearGradient(listOf(theme.accentLight, theme.accent)))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .then(ytmMod)
                        .clickable { viewModel.setBrowseMode("ytm") },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isYtm) Color(0xFF1D110B) else AmberDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "YouTube Music",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isYtm) Color(0xFF1D110B) else AmberDim
                        )
                    }
                }

                val ytMod = if (!isYtm) {
                    Modifier.background(Brush.linearGradient(listOf(theme.accentLight, theme.accent)))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .then(ytMod)
                        .clickable { viewModel.setBrowseMode("yt") },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = if (!isYtm) Color(0xFF1D110B) else AmberDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "YouTube",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isYtm) Color(0xFF1D110B) else AmberDim
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search text field
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setBrowseQuery(it) },
            placeholder = {
                Text(
                    text = if (preferences.browseMode == "ytm") "Search songs, or paste link" else "Search videos, or paste link",
                    color = AmberFaint,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = theme.accent
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setBrowseQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = AmberFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotEmpty()) {
                                        viewModel.setBrowseQuery(text)
                                        viewModel.executeSearch()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = AmberDim,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
            shape = RoundedCornerShape(22.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    viewModel.executeSearch()
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filterChips) { (chipKey, label) ->
                val isSelected = filter == chipKey
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (isSelected) theme.accent else AmberGlassRaised,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) theme.accent else AmberLine
                    ),
                    modifier = Modifier.clickable {
                        viewModel.setBrowseFilter(chipKey)
                    }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF1D110B) else AmberDim,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Results or Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = theme.accent)
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isEmpty()) "Search for songs or artists\nor paste any YouTube link above" else "No results found for '$query'",
                    color = AmberFaint,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results, key = { it.id }) { track ->
                    val isLiked = likedTracks.any { it.id == track.id }
                    TrackRow(
                        track = track,
                        isPlaying = currentTrack?.id == track.id,
                        onClick = { viewModel.playTrack(track) },
                        onPlayNext = { viewModel.playNext(track) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onToggleLike = { viewModel.toggleLikeTrack(track) },
                        isLiked = isLiked,
                        onDownload = {
                            viewModel.setDownloadUrlInput("https://www.youtube.com/watch?v=${track.id}")
                            viewModel.selectTab(AmberTab.DOWNLOAD)
                            viewModel.fetchDownloadTarget()
                        }
                    )
                }
            }
        }
    }
}
