package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.StreamUrlResolver
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: MusicRepository,
    playerManager: PlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val scope = rememberCoroutineScope()

    var autoplayEnabled by remember { mutableStateOf(true) }
    var selectedQuality by remember { mutableStateOf(StreamUrlResolver.AudioQuality.HIGH) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearRecentDialog by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Playback section
            item {
                SettingsSectionHeader("Playback")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Autorenew,
                            title = "Autoplay",
                            subtitle = "Automatically fetch and play recommendations when queue finishes",
                            checked = autoplayEnabled,
                            onCheckedChange = {
                                autoplayEnabled = it
                                playerManager.setAutoplayEnabled(it)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        SettingsClickableItem(
                            icon = Icons.Outlined.HighQuality,
                            title = "Streaming Quality",
                            subtitle = when (selectedQuality) {
                                StreamUrlResolver.AudioQuality.HIGH -> "320 kbps (Extreme)"
                                StreamUrlResolver.AudioQuality.MEDIUM -> "160 kbps (High)"
                                StreamUrlResolver.AudioQuality.LOW -> "96 kbps (Normal)"
                            },
                            onClick = { showQualityDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        SettingsSwitchItem(
                            icon = Icons.Outlined.Headphones,
                            title = "Pause on Disconnect",
                            subtitle = "Automatically pause when headphones or Bluetooth device disconnects",
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                }
            }

            // Data & Storage
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionHeader("Data & Storage")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        SettingsClickableItem(
                            icon = Icons.Outlined.History,
                            title = "Clear Search History",
                            subtitle = "Remove all past searches from suggestions",
                            onClick = { showClearHistoryDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        SettingsClickableItem(
                            icon = Icons.Outlined.DeleteSweep,
                            title = "Clear Recently Played",
                            subtitle = "Reset your recently played songs history",
                            onClick = { showClearRecentDialog = true }
                        )
                    }
                }
            }

            // About SMusic
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionHeader("About SMusic")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "SMusic",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0 (Pixel Edition)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "High performance music streaming app powered by JioSaavn Web API and AndroidX Media3 / ExoPlayer playback engine.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Quality dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Audio Quality", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    StreamUrlResolver.AudioQuality.values().forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedQuality = q
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedQuality == q,
                                onClick = {
                                    selectedQuality = q
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when (q) {
                                    StreamUrlResolver.AudioQuality.HIGH -> "320 kbps (Extreme)"
                                    StreamUrlResolver.AudioQuality.MEDIUM -> "160 kbps (High)"
                                    StreamUrlResolver.AudioQuality.LOW -> "96 kbps (Normal)"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Search History") },
            text = { Text("Are you sure you want to clear your recent searches?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearSearchHistory()
                            showClearHistoryDialog = false
                            snackbarHostState.showSnackbar("Search history cleared")
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearRecentDialog) {
        AlertDialog(
            onDismissRequest = { showClearRecentDialog = false },
            title = { Text("Clear Recently Played") },
            text = { Text("Are you sure you want to clear your recently played history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearRecentlyPlayed()
                            showClearRecentDialog = false
                            snackbarHostState.showSnackbar("Recently played cleared")
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearRecentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}
