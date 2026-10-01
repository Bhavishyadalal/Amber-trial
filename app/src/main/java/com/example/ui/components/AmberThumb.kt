package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.LocalAmberTheme

@Composable
fun AmberThumb(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val theme = LocalAmberTheme.current
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(AmberGlassRaised),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = theme.accent.copy(alpha = 0.6f),
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}
