package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.components.AmberGlassCard
import com.example.ui.components.AmberThumb
import com.example.ui.theme.AmberAccentLightDefault
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberFaint
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.AmberLine
import com.example.ui.theme.AmberLineHighlight
import com.example.ui.theme.AmberText
import com.example.ui.theme.LocalAmberTheme
import com.example.ui.viewmodel.AmberViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: AmberViewModel,
    modifier: Modifier = Modifier
) {
    val shelves by viewModel.shelves.collectAsState()
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val theme = LocalAmberTheme.current

    val greeting = rememberGreeting()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            // Header with greeting & Amber Music badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WELCOME BACK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accentLight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AmberText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = AmberGlassRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberLineHighlight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = theme.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Amber Music",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentLight
                        )
                    }
                }
            }
        }

        if (isHomeLoading && shelves.all { it.tracks.isEmpty() }) {
            items(4) {
                ShelfSkeleton()
            }
        } else {
            items(shelves, key = { it.id }) { shelf ->
                ShelfSection(
                    title = shelf.title,
                    subtitle = shelf.subtitle,
                    tracks = shelf.tracks,
                    isLoading = shelf.isLoading,
                    currentPlayingId = currentTrack?.id,
                    onTrackClick = { track ->
                        // Play this track and queue the rest of the shelf!
                        viewModel.playTrack(track, newQueue = true)
                        val remaining = shelf.tracks.filter { it.id != track.id }
                        remaining.forEach { viewModel.addToQueue(it) }
                    },
                    onPlayAll = {
                        if (shelf.tracks.isNotEmpty()) {
                            viewModel.playTrack(shelf.tracks.first(), newQueue = true)
                            shelf.tracks.drop(1).forEach { viewModel.addToQueue(it) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ShelfSection(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    isLoading: Boolean,
    currentPlayingId: String?,
    onTrackClick: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    val theme = LocalAmberTheme.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = AmberText
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberDim
                )
            }

            if (tracks.isNotEmpty()) {
                IconButton(
                    onClick = onPlayAll,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "Play all",
                        tint = theme.accentLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading && tracks.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    TrackCardSkeleton()
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    ShelfTrackCard(
                        track = track,
                        isPlaying = currentPlayingId == track.id,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShelfTrackCard(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalAmberTheme.current

    Surface(
        modifier = Modifier
            .width(145.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AmberGlassRaised,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) theme.accent else AmberLine
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
            ) {
                AmberThumb(
                    url = track.thumbnail,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.dp, end = 4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(theme.accentLight, theme.accent, theme.accentDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF1D110B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.sp,
                    color = if (isPlaying) theme.accentLight else AmberText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = track.artist.ifEmpty { "Amber Music" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.5.sp,
                    color = AmberDim
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ShelfSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AmberGlassRaised)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AmberGlassRaised)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                TrackCardSkeleton()
            }
        }
    }
}

@Composable
fun TrackCardSkeleton() {
    Box(
        modifier = Modifier
            .width(145.dp)
            .height(175.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AmberGlassRaised)
    )
}

@Composable
fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
