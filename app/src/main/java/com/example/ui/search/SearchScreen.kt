package com.example.ui.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.PlayableTrack
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.player.PlayerManager
import com.example.ui.common.*

data class DiscoverPlate(
    val title: String,
    val searchQuery: String,
    val startColor: Color,
    val endColor: Color,
    val icon: ImageVector
)

val discoverPlatesList = listOf(
    DiscoverPlate("Pop Hits", "Pop Hits", Color(0xFFE91E63), Color(0xFF9C27B0), Icons.Default.Favorite),
    DiscoverPlate("Bollywood", "Bollywood Hits", Color(0xFFFF5722), Color(0xFFC2185B), Icons.Default.Movie),
    DiscoverPlate("Punjabi Beats", "Punjabi Hits", Color(0xFFFF9800), Color(0xFFE65100), Icons.Default.Audiotrack),
    DiscoverPlate("Hip-Hop & Rap", "Hip Hop Rap", Color(0xFFF59E0B), Color(0xFFD97706), Icons.Default.GraphicEq),
    DiscoverPlate("Romantic", "Romantic Love Songs", Color(0xFFF43F5E), Color(0xFFBE123C), Icons.Default.FavoriteBorder),
    DiscoverPlate("Lo-Fi & Chill", "Lofi Chill Beats", Color(0xFF6366F1), Color(0xFF4338CA), Icons.Default.Headphones),
    DiscoverPlate("Dance & Party", "Party Dance Hits", Color(0xFF06B6D4), Color(0xFF0284C7), Icons.Default.Celebration),
    DiscoverPlate("Rock Classics", "Rock Classics", Color(0xFF64748B), Color(0xFF334155), Icons.Default.ElectricBolt),
    DiscoverPlate("Devotional", "Bhakti Songs", Color(0xFFFBBF24), Color(0xFFD97706), Icons.Default.SelfImprovement),
    DiscoverPlate("Indie Vibes", "Indie Music", Color(0xFF10B981), Color(0xFF047857), Icons.Default.NaturePeople),
    DiscoverPlate("Workout Gym", "Workout Gym Beats", Color(0xFFEF4444), Color(0xFFB91C1C), Icons.Default.FitnessCenter),
    DiscoverPlate("Acoustic", "Acoustic Unplugged", Color(0xFFD97706), Color(0xFF92400E), Icons.Default.MusicNote),
    DiscoverPlate("Top 50 Global", "Top 50 Charts", Color(0xFF8B5CF6), Color(0xFF6D28D9), Icons.Default.TrendingUp),
    DiscoverPlate("Late Night", "Night Drive Vibes", Color(0xFF3B82F6), Color(0xFF1D4ED8), Icons.Default.NightsStay),
    DiscoverPlate("Retro 90s", "90s Bollywood Hits", Color(0xFFEC4899), Color(0xFFBE185D), Icons.Default.Radio),
    DiscoverPlate("Haryanvi Hits", "Haryanvi Songs", Color(0xFF14B8A6), Color(0xFF0F766E), Icons.Default.LibraryMusic),
    DiscoverPlate("Gaming EDM", "Gaming EDM", Color(0xFFA855F7), Color(0xFF7E22CE), Icons.Default.SportsEsports),
    DiscoverPlate("Instrumental", "Peaceful Instrumental Piano", Color(0xFF0284C7), Color(0xFF0369A1), Icons.Default.Piano)
)

fun getTabIcon(tab: SearchTab): ImageVector {
    return when (tab) {
        SearchTab.ALL -> Icons.Default.TravelExplore
        SearchTab.SONGS -> Icons.Default.MusicNote
        SearchTab.ALBUMS -> Icons.Default.Album
        SearchTab.ARTISTS -> Icons.Default.Person
        SearchTab.PLAYLISTS -> Icons.AutoMirrored.Filled.QueueMusic
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    playerManager: PlayerManager,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions by viewModel.autocompleteSuggestions.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val currentTrack by playerManager.currentTrack.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "Search songs, artists, albums...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearQuery() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.submitSearch()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_text_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Clean Category Tabs (No Bulky Black Pill Box - Clean Transparent Design)
                val tabs = SearchTab.values()
                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {},
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEach { tab ->
                        val isSelected = activeTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTab(tab) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getTabIcon(tab),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = Color.Transparent, // Removed black pill background
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isSelected) null else FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("search_tab_${tab.name}")
                        )
                    }
                }
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
            // Autocomplete suggestions dropdown when typing
            val currentSuggestions = suggestions
            if (currentSuggestions != null && query.isNotBlank() && uiState !is SearchUiState.Results) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = 80.dp)
                ) {
                    val s = currentSuggestions
                    if (s.songs.isNotEmpty()) {
                        item(key = "ac_header_songs") {
                            Text(
                                "Songs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        items(s.songs.take(5), key = { "ac_song_${it.id}" }) { song ->
                            SongRowItem(
                                song = song,
                                isPlaying = currentTrack?.id == song.id,
                                onClick = {
                                    viewModel.playTrack(song.toPlayableTrack(), s.songs.map { it.toPlayableTrack() })
                                    focusManager.clearFocus()
                                },
                                onMoreClick = { selectedTrackForOptions = song.toPlayableTrack() }
                            )
                        }
                    }

                    if (s.albums.isNotEmpty()) {
                        item(key = "ac_header_albums") {
                            Text(
                                "Albums",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        items(s.albums.take(4), key = { "ac_album_${it.id}" }) { album ->
                            ListItem(
                                headlineContent = { Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))) {
                                        AsyncImage(model = album.artwork, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onNavigateToAlbum(album.id)
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }

                    if (s.artists.isNotEmpty()) {
                        item(key = "ac_header_artists") {
                            Text(
                                "Artists",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        items(s.artists.take(4), key = { "ac_artist_${it.id}" }) { artist ->
                            ListItem(
                                headlineContent = { Text(artist.name) },
                                leadingContent = {
                                    Box(modifier = Modifier.size(42.dp).clip(CircleShape)) {
                                        AsyncImage(model = artist.image, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onNavigateToArtist(artist.id)
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                }
            } else {
                when (val state = uiState) {
                    is SearchUiState.Idle -> {
                        // Compact Adaptive Discover Grid for all screen sizes
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = if (currentTrack != null) 96.dp else 64.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Recent Searches section if any
                            if (searchHistory.isNotEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent Searches",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearHistory() },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Clear all", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                        }
                                    }
                                }

                                items(searchHistory.take(3), key = { "hist_$it" }, span = { GridItemSpan(2) }) { historyItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.submitSearch(historyItem)
                                                focusManager.clearFocus()
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = historyItem,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.NorthWest,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Compact Discover Plates
                            items(discoverPlatesList, key = { it.title }) { plate ->
                                DiscoverPlateCard(
                                    plate = plate,
                                    onClick = {
                                        viewModel.submitSearch(plate.searchQuery)
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }
                    is SearchUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is SearchUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    is SearchUiState.Results -> {
                        SearchResultsContent(
                            results = state,
                            tab = activeTab,
                            currentPlayingId = currentTrack?.id,
                            onPlaySong = { song, songs ->
                                viewModel.playTrack(song.toPlayableTrack(), songs.map { it.toPlayableTrack() })
                            },
                            onMoreSong = { song -> selectedTrackForOptions = song.toPlayableTrack() },
                            onAlbumClick = { onNavigateToAlbum(it.id) },
                            onPlaylistClick = { onNavigateToPlaylist(it.id) },
                            onArtistClick = { onNavigateToArtist(it.id) }
                        )
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
            onPlayNow = { viewModel.playTrack(track) },
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onToggleLike = { viewModel.toggleLike(track) },
            onAddToPlaylist = { },
            onViewAlbum = if (track.albumId.isNotBlank()) {
                { onNavigateToAlbum(track.albumId) }
            } else null
        )
    }
}

@Composable
fun DiscoverPlateCard(
    plate: DiscoverPlate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp) // Compact size for all phone screens
            .testTag("discover_plate_${plate.title}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(plate.startColor, plate.endColor)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Category Title
            Text(
                text = plate.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.7f)
            )

            // Stylized Decorative Icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .rotate(12f)
            ) {
                Icon(
                    imageVector = plate.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun SearchResultsContent(
    results: SearchUiState.Results,
    tab: SearchTab,
    currentPlayingId: String?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onMoreSong: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onArtistClick: (Artist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        when (tab) {
            SearchTab.ALL -> {
                // Top Songs
                if (results.songs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    items(results.songs.take(5), key = { "song_${it.id}" }) { song ->
                        SongRowItem(
                            song = song,
                            isPlaying = currentPlayingId == song.id,
                            onClick = { onPlaySong(song, results.songs) },
                            onMoreClick = { onMoreSong(song) }
                        )
                    }
                }

                // Albums
                if (results.albums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(results.albums, key = { "album_${it.id}" }) { album ->
                                AlbumCard(album = album, onClick = { onAlbumClick(album) })
                            }
                        }
                    }
                }

                // Artists
                if (results.artists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(results.artists, key = { "artist_${it.id}" }) { artist ->
                                ArtistCard(artist = artist, onClick = { onArtistClick(artist) })
                            }
                        }
                    }
                }

                // Playlists
                if (results.playlists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Playlists",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(results.playlists, key = { "playlist_${it.id}" }) { playlist ->
                                PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                            }
                        }
                    }
                }
            }
            SearchTab.SONGS -> {
                items(results.songs, key = { it.id }) { song ->
                    SongRowItem(
                        song = song,
                        isPlaying = currentPlayingId == song.id,
                        onClick = { onPlaySong(song, results.songs) },
                        onMoreClick = { onMoreSong(song) }
                    )
                }
            }
            SearchTab.ALBUMS -> {
                items(results.albums, key = { it.id }) { album ->
                    ListItem(
                        headlineContent = { Text(album.title, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(album.artist.ifBlank { album.year }) },
                        leadingContent = {
                            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))) {
                                AsyncImage(model = album.artwork, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable { onAlbumClick(album) }
                    )
                }
            }
            SearchTab.ARTISTS -> {
                items(results.artists, key = { it.id }) { artist ->
                    ListItem(
                        headlineContent = { Text(artist.name, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(artist.role.ifBlank { "Artist" }) },
                        leadingContent = {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape)) {
                                AsyncImage(model = artist.image, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable { onArtistClick(artist) }
                    )
                }
            }
            SearchTab.PLAYLISTS -> {
                items(results.playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.title, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(playlist.subtitle.ifBlank { "${playlist.songCount} Songs" }) },
                        leadingContent = {
                            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))) {
                                AsyncImage(model = playlist.artwork, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable { onPlaylistClick(playlist) }
                    )
                }
            }
        }
    }
}
