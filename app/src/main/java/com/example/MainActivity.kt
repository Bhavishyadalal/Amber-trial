package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AmberEngine
import com.example.ui.components.AmberBottomNav
import com.example.ui.components.AmberTab
import com.example.ui.components.LivingBackground
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.BrowseScreen
import com.example.ui.screens.DownloadScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.AmberLineHighlight
import com.example.ui.theme.AmberSage
import com.example.ui.theme.AmberText
import com.example.ui.theme.AmberTheme
import com.example.ui.viewmodel.AmberViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AmberViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val paletteColor by viewModel.paletteAccentColor.collectAsState()
            val preferences by viewModel.preferences.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val currentTrack by viewModel.currentTrack.collectAsState()
            val isPlaying by viewModel.isPlaying.collectAsState()
            val currentPosMs by viewModel.currentPositionMs.collectAsState()
            val durationMs by viewModel.durationMs.collectAsState()
            val activeDownloadCount by viewModel.activeJobCount.collectAsState()
            val userMessage by viewModel.userMessage.collectAsState()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(userMessage) {
                userMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearToast()
                }
            }

            // Back button handling
            BackHandler(enabled = currentTab != AmberTab.HOME) {
                viewModel.selectTab(AmberTab.HOME)
            }

            AmberTheme(
                accentColor = paletteColor,
                isAmbientGlow = preferences.ambientGlow,
                isLowPower = preferences.lowPower,
                isPerformance = preferences.performance
            ) {
                LivingBackground {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        snackbarHost = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 110.dp)
                                    .navigationBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                SnackbarHost(hostState = snackbarHostState)
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                // App Header (Wordmark + Health badge)
                                HeaderBar(
                                    accentColor = paletteColor,
                                    onSetupClick = { viewModel.selectTab(AmberTab.SETUP) }
                                )

                                // Main Content Tabs
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    when (currentTab) {
                                        AmberTab.HOME -> HomeScreen(viewModel = viewModel)
                                        AmberTab.PLAYER -> PlayerScreen(viewModel = viewModel)
                                        AmberTab.BROWSE -> BrowseScreen(viewModel = viewModel)
                                        AmberTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                                        AmberTab.DOWNLOAD -> DownloadScreen(viewModel = viewModel)
                                        AmberTab.SETUP -> SetupScreen(viewModel = viewModel)
                                    }
                                }
                            }

                            // Bottom Dock (MiniPlayer + Floating AmberBottomNav)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .navigationBarsPadding(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // MiniPlayer: visible whenever a track is loaded and user is not on Player tab
                                val showMini = currentTrack != null && currentTab != AmberTab.PLAYER
                                AnimatedVisibility(
                                    visible = showMini,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    val progressFrac = if (durationMs > 0) {
                                        (currentPosMs.toFloat() / durationMs).coerceIn(0f, 1f)
                                    } else 0f

                                    MiniPlayer(
                                        currentTrack = currentTrack,
                                        isPlaying = isPlaying,
                                        progressFraction = progressFrac,
                                        onTogglePlay = { viewModel.togglePlayPause() },
                                        onClick = { viewModel.selectTab(AmberTab.PLAYER) }
                                    )
                                }

                                AmberBottomNav(
                                    currentTab = currentTab,
                                    onTabSelected = { viewModel.selectTab(it) },
                                    activeDownloadCount = activeDownloadCount
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    viewModel.setDownloadUrlInput(sharedText.trim())
                    viewModel.selectTab(AmberTab.DOWNLOAD)
                    viewModel.fetchDownloadTarget()
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val uri: Uri? = intent.data
            if (uri != null) {
                val url = uri.toString()
                viewModel.setDownloadUrlInput(url)
                viewModel.selectTab(AmberTab.DOWNLOAD)
                viewModel.fetchDownloadTarget()
            }
        } else if (intent.getStringExtra("navigate_tab") == "download") {
            viewModel.selectTab(AmberTab.DOWNLOAD)
        }
    }
}

@Composable
fun HeaderBar(
    accentColor: Color,
    onSetupClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Amber Serif Wordmark
        Text(
            text = "Amber",
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = AmberText,
            letterSpacing = (-0.5).sp
        )

        // Status badge
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = AmberGlassRaised,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberLineHighlight),
            modifier = Modifier.clickable(onClick = onSetupClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(AmberSage)
                )
                Text(
                    text = if (AmberEngine.isEngineReady()) "Ready" else "Amber",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AmberDim
                )
            }
        }
    }
}
