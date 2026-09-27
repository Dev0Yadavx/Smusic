package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import com.example.ui.details.AlbumDetailScreen
import com.example.ui.details.ArtistDetailScreen
import com.example.ui.details.PlaylistDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.player.MiniPlayer
import com.example.ui.player.NowPlayingModal
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.SMusicTheme

enum class RootScreen {
    HOME, SEARCH, LIBRARY
}

sealed class SubScreen {
    object None : SubScreen()
    object Settings : SubScreen()
    data class AlbumDetail(val id: String) : SubScreen()
    data class PlaylistDetail(val id: String) : SubScreen()
    data class ArtistDetail(val id: String) : SubScreen()
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val playerManager = PlayerManager.getInstance(applicationContext)
        val repository = MusicRepository(applicationContext)

        setContent {
            SMusicTheme(darkTheme = true) {
                var currentRootScreen by remember { mutableStateOf(RootScreen.HOME) }
                var currentSubScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
                var isNowPlayingOpen by remember { mutableStateOf(false) }

                val currentTrack by playerManager.currentTrack.collectAsState()

                // Intercept back button when Now Playing or Subscreen is open
                BackHandler(enabled = isNowPlayingOpen || currentSubScreen !is SubScreen.None) {
                    if (isNowPlayingOpen) {
                        isNowPlayingOpen = false
                    } else if (currentSubScreen !is SubScreen.None) {
                        currentSubScreen = SubScreen.None
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Scaffold(
                        bottomBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                // Persistent Air-Style Mini Player above Bottom Navigation
                                AnimatedVisibility(
                                    visible = currentTrack != null && !isNowPlayingOpen,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        MiniPlayer(
                                            playerManager = playerManager,
                                            onClick = { isNowPlayingOpen = true }
                                        )
                                    }
                                }

                                // Rounded Top Navigation Bar with Animated Icon Switch
                                RoundedAnimatedBottomBar(
                                    currentRootScreen = currentRootScreen,
                                    isSubScreenOpen = currentSubScreen !is SubScreen.None,
                                    onSelectTab = { screen ->
                                        currentRootScreen = screen
                                        currentSubScreen = SubScreen.None
                                    }
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (val sub = currentSubScreen) {
                                is SubScreen.Settings -> {
                                    SettingsScreen(
                                        repository = repository,
                                        playerManager = playerManager,
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                is SubScreen.AlbumDetail -> {
                                    AlbumDetailScreen(
                                        albumId = sub.id,
                                        repository = repository,
                                        playerManager = playerManager,
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                is SubScreen.PlaylistDetail -> {
                                    PlaylistDetailScreen(
                                        playlistId = sub.id,
                                        repository = repository,
                                        playerManager = playerManager,
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                is SubScreen.ArtistDetail -> {
                                    ArtistDetailScreen(
                                        artistId = sub.id,
                                        repository = repository,
                                        playerManager = playerManager,
                                        onNavigateToAlbum = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                SubScreen.None -> {
                                    when (currentRootScreen) {
                                        RootScreen.HOME -> {
                                            HomeScreen(
                                                viewModel = homeViewModel,
                                                playerManager = playerManager,
                                                onNavigateToSearch = { currentRootScreen = RootScreen.SEARCH },
                                                onNavigateToSettings = { currentSubScreen = SubScreen.Settings },
                                                onNavigateToAlbum = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                                onNavigateToPlaylist = { currentSubScreen = SubScreen.PlaylistDetail(it) }
                                            )
                                        }
                                        RootScreen.SEARCH -> {
                                            SearchScreen(
                                                viewModel = searchViewModel,
                                                playerManager = playerManager,
                                                onNavigateToAlbum = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                                onNavigateToPlaylist = { currentSubScreen = SubScreen.PlaylistDetail(it) },
                                                onNavigateToArtist = { currentSubScreen = SubScreen.ArtistDetail(it) }
                                            )
                                        }
                                        RootScreen.LIBRARY -> {
                                            LibraryScreen(
                                                viewModel = libraryViewModel,
                                                playerManager = playerManager
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Full Screen Now Playing Modal (Slide in from bottom, covers root)
                    AnimatedVisibility(
                        visible = isNowPlayingOpen && currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NowPlayingModal(
                            playerManager = playerManager,
                            repository = repository,
                            onDismiss = { isNowPlayingOpen = false },
                            onViewAlbum = { albumId ->
                                isNowPlayingOpen = false
                                currentSubScreen = SubScreen.AlbumDetail(albumId)
                            },
                            onViewArtist = { artistId ->
                                isNowPlayingOpen = false
                                currentSubScreen = SubScreen.ArtistDetail(artistId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoundedAnimatedBottomBar(
    currentRootScreen: RootScreen,
    isSubScreenOpen: Boolean,
    onSelectTab: (RootScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        tonalElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .testTag("bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedNavTabItem(
                selected = currentRootScreen == RootScreen.HOME && !isSubScreenOpen,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = "Home",
                onClick = { onSelectTab(RootScreen.HOME) },
                testTag = "nav_item_home"
            )

            AnimatedNavTabItem(
                selected = currentRootScreen == RootScreen.SEARCH && !isSubScreenOpen,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                label = "Search",
                onClick = { onSelectTab(RootScreen.SEARCH) },
                testTag = "nav_item_search"
            )

            AnimatedNavTabItem(
                selected = currentRootScreen == RootScreen.LIBRARY && !isSubScreenOpen,
                selectedIcon = Icons.Filled.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic,
                label = "Library",
                onClick = { onSelectTab(RootScreen.LIBRARY) },
                testTag = "nav_item_library"
            )
        }
    }
}

@Composable
fun AnimatedNavTabItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tab_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "tab_color"
    )

    val pillScaleAnim by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pill_scale"
    )

    val pillAlphaAnim by animateFloatAsState(
        targetValue = if (selected) 0.75f else 0.0f,
        animationSpec = tween(220),
        label = "pill_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            // Animated Pill Indicator background
            if (pillAlphaAnim > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 30.dp)
                        .scale(pillScaleAnim)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = pillAlphaAnim))
                )
            }

            // Animated Icon with bounce and morph
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(scaleAnim)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                color = iconColor
            )
        )
    }
}
