package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberBgDeep
import com.example.ui.theme.LocalAmberTheme

@Composable
fun LivingBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalAmberTheme.current
    val accentAnim by animateColorAsState(
        targetValue = theme.accent,
        animationSpec = tween(durationMillis = 1000),
        label = "accentBg"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Main deep dark radial gradient
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentAnim.copy(alpha = 0.12f),
                        AmberBg,
                        AmberBgDeep
                    ),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 1.2f
                )
            )

            if (theme.isAmbientGlow) {
                // Soft top-left ambient orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentAnim.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.65f
                )

                // Soft bottom-right ambient orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentAnim.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(size.width, size.height),
                    radius = size.width * 0.7f
                )
            }
        }
        content()
    }
}
