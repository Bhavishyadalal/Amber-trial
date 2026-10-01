package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AmberEngine
import com.example.ui.components.AmberGlassCard
import com.example.ui.components.AmberPrimaryButton
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberFaint
import com.example.ui.theme.AmberGlassRaised
import com.example.ui.theme.AmberLine
import com.example.ui.theme.AmberLineHighlight
import com.example.ui.theme.AmberSage
import com.example.ui.theme.AmberText
import com.example.ui.theme.LocalAmberTheme
import com.example.ui.viewmodel.AmberViewModel

@Composable
fun SetupScreen(
    viewModel: AmberViewModel,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferences.collectAsState()
    val ytDlpStatus by viewModel.ytDlpStatus.collectAsState()
    val isUpdatingYtDlp by viewModel.isUpdatingYtDlp.collectAsState()
    val theme = LocalAmberTheme.current
    val context = LocalContext.current

    var apiKeyInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Setup",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                color = AmberText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure engine tools, download formats, and app preferences.",
                style = MaterialTheme.typography.bodyMedium,
                color = AmberDim
            )
        }

        // Section: Engine & Audio Tools
        item {
            SetupSection(title = "Audio tools") {
                // ffmpeg row
                SettingRow(
                    title = "Media converter (ffmpeg)",
                    subtitle = if (AmberEngine.isFfmpegAvailable()) "Ready \u2022 bundled with Amber" else "Not available"
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (AmberEngine.isFfmpegAvailable()) AmberSage else Color(0xFFEF8A75))
                    )
                }

                // yt-dlp row
                SettingRow(
                    title = "Stream extractor (yt-dlp)",
                    subtitle = "Version $ytDlpStatus"
                ) {
                    if (isUpdatingYtDlp) {
                        CircularProgressIndicator(
                            color = theme.accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Update",
                            color = theme.accentLight,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateYtDlp() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Section: Downloads Settings
        item {
            SetupSection(title = "Downloads") {
                // Audio format segmented control (M4A | MP3 | Opus)
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        text = "Audio format",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberText
                    )
                    Text(
                        text = "For songs you download",
                        fontSize = 12.sp,
                        color = AmberDim
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("m4a" to "M4A", "mp3" to "MP3", "opus" to "Opus").forEach { (code, label) ->
                            val isSelected = preferences.audioFormat == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(
                                        if (isSelected) theme.accent else Color.Transparent
                                    )
                                    .clickable { viewModel.setAudioFormat(code) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF1D110B) else AmberDim
                                )
                            }
                        }
                    }
                }

                SettingToggleRow(
                    title = "Cover art and tags",
                    subtitle = "Save title, artist and album art inside the file",
                    checked = preferences.embedMeta,
                    onCheckedChange = { viewModel.setEmbedMeta(it) }
                )

                SettingToggleRow(
                    title = "Chapters in videos",
                    subtitle = "Keep video chapter markers",
                    checked = preferences.chapters,
                    onCheckedChange = { viewModel.setChapters(it) }
                )

                SettingToggleRow(
                    title = "Subtitles in videos",
                    subtitle = "Embed subtitles when available",
                    checked = preferences.subtitles,
                    onCheckedChange = { viewModel.setSubtitles(it) }
                )

                SettingRow(
                    title = "Songs folder",
                    subtitle = AmberEngine.getDownloadOutputDir(context, true).absolutePath
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Folder",
                        tint = AmberDim,
                        modifier = Modifier.size(20.dp)
                    )
                }

                SettingRow(
                    title = "Videos folder",
                    subtitle = AmberEngine.getDownloadOutputDir(context, false).absolutePath
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Folder",
                        tint = AmberDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section: Search Access (Optional API Key)
        item {
            SetupSection(title = "Search access") {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        text = "YouTube Data API Key (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberText
                    )
                    Text(
                        text = "Search works without any key by default. You can paste your own official Google Cloud key if desired.",
                        fontSize = 12.sp,
                        color = AmberDim
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (preferences.youtubeApiKey.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "••••••••••••${preferences.youtubeApiKey.takeLast(4)}",
                                color = theme.accentLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Remove",
                                color = AmberDim,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setYoutubeApiKey("") }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                placeholder = { Text("Paste AIza… key", color = AmberFaint, fontSize = 13.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AmberGlassRaised,
                                    unfocusedContainerColor = AmberGlassRaised,
                                    focusedBorderColor = theme.accent,
                                    unfocusedBorderColor = AmberLine,
                                    focusedTextColor = AmberText,
                                    unfocusedTextColor = AmberText
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            AmberPrimaryButton(
                                onClick = {
                                    if (apiKeyInput.isNotBlank()) {
                                        viewModel.setYoutubeApiKey(apiKeyInput.trim())
                                        apiKeyInput = ""
                                        viewModel.showToast("API key saved")
                                    }
                                },
                                enabled = apiKeyInput.isNotBlank()
                            ) {
                                Text("Save", color = Color(0xFF1D110B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: Preferences
        item {
            SetupSection(title = "Preferences") {
                SettingToggleRow(
                    title = "Autoplay",
                    subtitle = "Auto-queue similar songs when the queue ends",
                    checked = preferences.autoplay,
                    onCheckedChange = { viewModel.setAutoplay(it) }
                )

                SettingToggleRow(
                    title = "Ambient glow",
                    subtitle = "Soft radiant glow behind cover art that follows its colors",
                    checked = preferences.ambientGlow,
                    onCheckedChange = { viewModel.setAmbientGlow(it) }
                )

                SettingToggleRow(
                    title = "Low-power mode",
                    subtitle = "Turns off decorative animations to conserve battery",
                    checked = preferences.lowPower,
                    onCheckedChange = { viewModel.setLowPower(it) }
                )

                SettingToggleRow(
                    title = "Performance mode",
                    subtitle = "Fastest: turns off glass blur, glow, and reduces rendering load",
                    checked = preferences.performance,
                    onCheckedChange = { viewModel.setPerformance(it) }
                )
            }
        }

        // Legal Notice
        item {
            Text(
                text = "Personal use only. Downloading copies of YouTube content may breach YouTube's Terms of Service.",
                color = AmberFaint,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
fun SetupSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AmberText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = AmberGlassRaised,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AmberText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AmberDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = LocalAmberTheme.current
    SettingRow(title = title, subtitle = subtitle) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFFF6E4),
                checkedTrackColor = theme.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
