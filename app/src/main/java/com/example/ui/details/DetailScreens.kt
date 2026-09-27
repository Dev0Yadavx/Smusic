package com.example.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.PlayableTrack
import com.example.data.model.Playlist
import com.example.data.remote.NetworkResult
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import com.example.ui.common.SongRowItem
import com.example.ui.common.TrackOptionsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    repository: MusicRepository,
    playerManager: PlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var albumResult by remember { mutableStateOf<NetworkResult<Album>>(NetworkResult.Loading) }
    val currentTrack by playerManager.currentTrack.collectAsState()
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }

    LaunchedEffect(albumId) {
        albumResult = repository.getAlbumDetails(albumId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val result = albumResult) {
                is NetworkResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is NetworkResult.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load album", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).run {
                                // retry
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                is NetworkResult.Success -> {
                    val album = result.data
                    val playableTracks = album.songs.map { it.toPlayableTrack() }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = album.artwork,
                                        contentDescription = album.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${album.artist} • ${album.year.ifBlank { "Album" }}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (playableTracks.isNotEmpty()) {
                                                playerManager.playTrack(playableTracks.first(), playableTracks)
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play All")
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            if (playableTracks.isNotEmpty()) {
                                                val shuffled = playableTracks.shuffled()
                                                playerManager.playTrack(shuffled.first(), shuffled)
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        itemsIndexed(album.songs, key = { _, s -> s.id }) { index, song ->
                            SongRowItem(
                                song = song,
                                isPlaying = currentTrack?.id == song.id,
                                onClick = { playerManager.playTrack(song.toPlayableTrack(), playableTracks) },
                                onMoreClick = { selectedTrackForOptions = song.toPlayableTrack() }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = false,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { playerManager.playTrack(track) },
            onPlayNext = { playerManager.playNext(track) },
            onAddToQueue = { playerManager.addToQueue(track) },
            onToggleLike = {
                scope.launch {
                    repository.toggleLike(track)
                }
            },
            onAddToPlaylist = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    repository: MusicRepository,
    playerManager: PlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var playlistResult by remember { mutableStateOf<NetworkResult<Playlist>>(NetworkResult.Loading) }
    val currentTrack by playerManager.currentTrack.collectAsState()
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }

    LaunchedEffect(playlistId) {
        playlistResult = repository.getPlaylistDetails(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val result = playlistResult) {
                is NetworkResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is NetworkResult.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load playlist", style = MaterialTheme.typography.titleMedium)
                    }
                }
                is NetworkResult.Success -> {
                    val playlist = result.data
                    val playableTracks = playlist.songs.map { it.toPlayableTrack() }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = playlist.artwork,
                                        contentDescription = playlist.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                if (playlist.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = playlist.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (playableTracks.isNotEmpty()) {
                                                playerManager.playTrack(playableTracks.first(), playableTracks)
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play All")
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            if (playableTracks.isNotEmpty()) {
                                                val shuffled = playableTracks.shuffled()
                                                playerManager.playTrack(shuffled.first(), shuffled)
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        itemsIndexed(playlist.songs, key = { _, s -> s.id }) { index, song ->
                            SongRowItem(
                                song = song,
                                isPlaying = currentTrack?.id == song.id,
                                onClick = { playerManager.playTrack(song.toPlayableTrack(), playableTracks) },
                                onMoreClick = { selectedTrackForOptions = song.toPlayableTrack() }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = false,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { playerManager.playTrack(track) },
            onPlayNext = { playerManager.playNext(track) },
            onAddToQueue = { playerManager.addToQueue(track) },
            onToggleLike = {
                scope.launch {
                    repository.toggleLike(track)
                }
            },
            onAddToPlaylist = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    repository: MusicRepository,
    playerManager: PlayerManager,
    onNavigateToAlbum: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var artistResult by remember { mutableStateOf<NetworkResult<Artist>>(NetworkResult.Loading) }
    val currentTrack by playerManager.currentTrack.collectAsState()
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }

    LaunchedEffect(artistId) {
        artistResult = repository.getArtistDetails(artistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val result = artistResult) {
                is NetworkResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is NetworkResult.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load artist", style = MaterialTheme.typography.titleMedium)
                    }
                }
                is NetworkResult.Success -> {
                    val artist = result.data
                    val playableTracks = artist.topSongs.map { it.toPlayableTrack() }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = artist.image,
                                        contentDescription = artist.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Text(
                                    text = artist.role.ifBlank { "Artist" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                        if (artist.topSongs.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Popular Songs",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }

                            itemsIndexed(artist.topSongs, key = { _, s -> s.id }) { index, song ->
                                SongRowItem(
                                    song = song,
                                    isPlaying = currentTrack?.id == song.id,
                                    onClick = { playerManager.playTrack(song.toPlayableTrack(), playableTracks) },
                                    onMoreClick = { selectedTrackForOptions = song.toPlayableTrack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = false,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { playerManager.playTrack(track) },
            onPlayNext = { playerManager.playNext(track) },
            onAddToQueue = { playerManager.addToQueue(track) },
            onToggleLike = {
                scope.launch {
                    repository.toggleLike(track)
                }
            },
            onAddToPlaylist = {}
        )
    }
}
