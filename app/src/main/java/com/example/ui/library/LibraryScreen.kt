package com.example.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlayableTrack
import com.example.data.model.Song
import com.example.player.PlayerManager
import com.example.ui.common.SongRowItem
import com.example.ui.common.TrackOptionsBottomSheet

data class LibrarySquareBox(
    val tab: LibraryTab,
    val title: String,
    val subtitle: String,
    val count: Int,
    val icon: ImageVector,
    val startGradient: Color,
    val endGradient: Color,
    val iconTint: Color = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playerManager: PlayerManager,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSongs.collectAsStateWithLifecycle()
    val isScanningLocal by viewModel.isScanningLocal.collectAsStateWithLifecycle()
    val currentTrack by playerManager.currentTrack.collectAsStateWithLifecycle()

    var showOverviewGrid by remember { mutableStateOf(true) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }

    // Handle back navigation when inside a category view
    BackHandler(enabled = !showOverviewGrid) {
        showOverviewGrid = true
    }

    // Storage permission launcher for local audio
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanLocalMusic()
        }
    }

    val squareBoxes = listOf(
        LibrarySquareBox(
            tab = LibraryTab.LIKED,
            title = "Liked Songs",
            subtitle = if (likedSongs.isEmpty()) "Favorite tracks" else "${likedSongs.size} tracks",
            count = likedSongs.size,
            icon = Icons.Filled.Favorite,
            startGradient = Color(0xFFF43F5E), // Rose Pink
            endGradient = Color(0xFFBE123C)
        ),
        LibrarySquareBox(
            tab = LibraryTab.DOWNLOADS,
            title = "Downloads",
            subtitle = if (downloadedSongs.isEmpty()) "Offline music" else "${downloadedSongs.size} saved",
            count = downloadedSongs.size,
            icon = Icons.Filled.DownloadDone,
            startGradient = Color(0xFF10B981), // Emerald Green
            endGradient = Color(0xFF047857)
        ),
        LibrarySquareBox(
            tab = LibraryTab.PLAYLISTS,
            title = "Playlists",
            subtitle = if (playlists.isEmpty()) "Custom mixes" else "${playlists.size} playlists",
            count = playlists.size,
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            startGradient = Color(0xFF8B5CF6), // Purple
            endGradient = Color(0xFF6D28D9)
        ),
        LibrarySquareBox(
            tab = LibraryTab.RECENT,
            title = "History",
            subtitle = if (recentlyPlayed.isEmpty()) "Recent listens" else "${recentlyPlayed.size} played",
            count = recentlyPlayed.size,
            icon = Icons.Filled.History,
            startGradient = Color(0xFFF59E0B), // Amber Orange
            endGradient = Color(0xFFB45309)
        ),
        LibrarySquareBox(
            tab = LibraryTab.LOCAL,
            title = "Local Files",
            subtitle = if (localSongs.isEmpty()) "Device storage" else "${localSongs.size} songs",
            count = localSongs.size,
            icon = Icons.Filled.FolderOpen,
            startGradient = Color(0xFF3B82F6), // Electric Blue
            endGradient = Color(0xFF1D4ED8)
        )
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!showOverviewGrid) {
                            IconButton(
                                onClick = { showOverviewGrid = true },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Library Grid"
                                )
                            }
                        }

                        Text(
                            text = if (showOverviewGrid) "Your Library" else when (selectedTab) {
                                LibraryTab.LIKED -> "Liked Songs"
                                LibraryTab.DOWNLOADS -> "Downloads"
                                LibraryTab.PLAYLISTS -> "Playlists"
                                LibraryTab.RECENT -> "History"
                                LibraryTab.LOCAL -> "Local Files"
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick switch to Grid toggle
                        if (!showOverviewGrid) {
                            IconButton(
                                onClick = { showOverviewGrid = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Grid View",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Create playlist button
                        IconButton(
                            onClick = { showCreatePlaylistDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .testTag("create_playlist_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Playlist",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // If inside category list, show horizontal filter tags
                if (!showOverviewGrid) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(squareBoxes, key = { it.tab.name }) { box ->
                            val isSelected = selectedTab == box.tab
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setTab(box.tab)
                                    if (box.tab == LibraryTab.LOCAL) {
                                        permissionLauncher.launch(permissionToRequest)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = box.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = box.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = box.startGradient,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    iconColor = box.startGradient
                                ),
                                border = null,
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = showOverviewGrid,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                label = "library_content_anim"
            ) { isGridOverview ->
                if (isGridOverview) {
                    // Large Square Box 2-Column Grid Layout (M3 Card Style)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 6.dp,
                            bottom = if (currentTrack != null) 96.dp else 64.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(squareBoxes, key = { it.title }) { box ->
                            LargeSquareBoxCard(
                                box = box,
                                onClick = {
                                    viewModel.setTab(box.tab)
                                    showOverviewGrid = false
                                    if (box.tab == LibraryTab.LOCAL && localSongs.isEmpty()) {
                                        permissionLauncher.launch(permissionToRequest)
                                    }
                                }
                            )
                        }

                        // Recently Played Quick List below Grid if available
                        if (recentlyPlayed.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recently Played",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp
                                            )
                                        )

                                        TextButton(
                                            onClick = {
                                                viewModel.setTab(LibraryTab.RECENT)
                                                showOverviewGrid = false
                                            }
                                        ) {
                                            Text(
                                                text = "See All",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            items(recentlyPlayed.take(4), key = { "rec_${it.id}" }, span = { GridItemSpan(2) }) { track ->
                                SongRowItem(
                                    song = Song(
                                        id = track.id,
                                        title = track.title,
                                        artist = track.artist,
                                        album = track.album,
                                        albumId = track.albumId,
                                        artwork = track.artwork,
                                        duration = track.duration,
                                        streamUrl = track.streamUrl
                                    ),
                                    isPlaying = currentTrack?.id == track.id,
                                    onClick = { viewModel.playTrack(track, recentlyPlayed) },
                                    onMoreClick = { selectedTrackForOptions = track }
                                )
                            }
                        }
                    }
                } else {
                    // Category Songs / Playlists View
                    when (selectedTab) {
                        LibraryTab.LIKED -> {
                            TrackListSection(
                                tracks = likedSongs,
                                currentPlayingId = currentTrack?.id,
                                emptyTitle = "No Liked Songs",
                                emptyMessage = "Tap the heart icon on any song or album to save it to your library!",
                                emptyIcon = Icons.Outlined.FavoriteBorder,
                                emptyAccentColor = Color(0xFFF43F5E),
                                onPlayTrack = { track -> viewModel.playTrack(track, likedSongs) },
                                onPlayAll = { viewModel.playAll(likedSongs) },
                                onShuffleAll = { viewModel.shuffleAll(likedSongs) },
                                onMoreClick = { selectedTrackForOptions = it }
                            )
                        }
                        LibraryTab.DOWNLOADS -> {
                            TrackListSection(
                                tracks = downloadedSongs,
                                currentPlayingId = currentTrack?.id,
                                emptyTitle = "No Offline Downloads",
                                emptyMessage = "Download your favorite songs to play anytime without internet connection.",
                                emptyIcon = Icons.Outlined.Download,
                                emptyAccentColor = Color(0xFF10B981),
                                onPlayTrack = { track -> viewModel.playTrack(track, downloadedSongs) },
                                onPlayAll = { viewModel.playAll(downloadedSongs) },
                                onShuffleAll = { viewModel.shuffleAll(downloadedSongs) },
                                onMoreClick = { selectedTrackForOptions = it }
                            )
                        }
                        LibraryTab.RECENT -> {
                            TrackListSection(
                                tracks = recentlyPlayed,
                                currentPlayingId = currentTrack?.id,
                                emptyTitle = "No History Yet",
                                emptyMessage = "Your recently played songs will automatically appear here as you listen!",
                                emptyIcon = Icons.Outlined.History,
                                emptyAccentColor = Color(0xFFF59E0B),
                                onPlayTrack = { track -> viewModel.playTrack(track, recentlyPlayed) },
                                onPlayAll = { viewModel.playAll(recentlyPlayed) },
                                onShuffleAll = { viewModel.shuffleAll(recentlyPlayed) },
                                onMoreClick = { selectedTrackForOptions = it }
                            )
                        }
                        LibraryTab.LOCAL -> {
                            if (isScanningLocal) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                TrackListSection(
                                    tracks = localSongs,
                                    currentPlayingId = currentTrack?.id,
                                    emptyTitle = "No Local Music Found",
                                    emptyMessage = "Scan your device storage to listen to offline MP3 and audio files.",
                                    emptyIcon = Icons.Outlined.FolderOpen,
                                    emptyAccentColor = Color(0xFF3B82F6),
                                    actionText = "Scan Storage",
                                    onAction = { permissionLauncher.launch(permissionToRequest) },
                                    onPlayTrack = { track -> viewModel.playTrack(track, localSongs) },
                                    onPlayAll = { viewModel.playAll(localSongs) },
                                    onShuffleAll = { viewModel.shuffleAll(localSongs) },
                                    onMoreClick = { selectedTrackForOptions = it }
                                )
                            }
                        }
                        LibraryTab.PLAYLISTS -> {
                            if (playlists.isEmpty()) {
                                EmptyLibraryState(
                                    title = "Create Your First Playlist",
                                    message = "Organize songs by your mood, favorite artists, or workout sessions.",
                                    icon = Icons.Outlined.QueueMusic,
                                    accentColor = Color(0xFF8B5CF6),
                                    actionText = "Create Playlist",
                                    onAction = { showCreatePlaylistDialog = true }
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 14.dp,
                                        end = 14.dp,
                                        top = 8.dp,
                                        bottom = if (currentTrack != null) 96.dp else 64.dp
                                    )
                                ) {
                                    items(playlists, key = { it.id }) { playlist ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                                        )
                                                    ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.QueueMusic,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = playlist.name,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (playlist.description.isNotBlank()) {
                                                        Text(
                                                            text = playlist.description,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete playlist",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        label = { Text("Description (Optional)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName.trim(), playlistDesc.trim())
                            showCreatePlaylistDialog = false
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = selectedTab == LibraryTab.LIKED,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { viewModel.playTrack(track) },
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onToggleLike = { viewModel.toggleLike(track) },
            onAddToPlaylist = {}
        )
    }
}

@Composable
fun LargeSquareBoxCard(
    box: LibrarySquareBox,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.05f) // Large Square Box proportion
            .testTag("square_card_${box.tab.name}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(box.startGradient, box.endGradient)
                    )
                )
                .padding(14.dp)
        ) {
            // Stylized Translucent Background Icon in corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .rotate(14f)
            ) {
                Icon(
                    imageVector = box.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.size(68.dp)
                )
            }

            // Floating Frosted Icon Badge in Top-Left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = box.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Count Tag Badge in Top-Right if > 0
            if (box.count > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "${box.count}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Titles & Subtitle at Bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.85f)
            ) {
                Text(
                    text = box.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = box.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TrackListSection(
    tracks: List<PlayableTrack>,
    currentPlayingId: String?,
    emptyTitle: String,
    emptyMessage: String,
    emptyIcon: ImageVector,
    emptyAccentColor: Color,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    onPlayTrack: (PlayableTrack) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onMoreClick: (PlayableTrack) -> Unit
) {
    if (tracks.isEmpty()) {
        EmptyLibraryState(
            title = emptyTitle,
            message = emptyMessage,
            icon = emptyIcon,
            accentColor = emptyAccentColor,
            actionText = actionText,
            onAction = onAction
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayAll,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = onShuffleAll,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items(tracks, key = { it.id }) { track ->
                SongRowItem(
                    song = Song(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        albumId = track.albumId,
                        artwork = track.artwork,
                        duration = track.duration,
                        streamUrl = track.streamUrl
                    ),
                    isPlaying = currentPlayingId == track.id,
                    onClick = { onPlayTrack(track) },
                    onMoreClick = { onMoreClick(track) }
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    title: String,
    message: String,
    icon: ImageVector,
    accentColor: Color,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expressive M3 Floating Icon Plate
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            ),
            modifier = Modifier.padding(horizontal = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(actionText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
