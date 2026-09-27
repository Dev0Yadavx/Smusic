package com.example.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaylistEntity
import com.example.data.model.PlayableTrack
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LibraryTab {
    LIKED, DOWNLOADS, PLAYLISTS, RECENT, LOCAL
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val playerManager = PlayerManager.getInstance(application)

    private val _selectedTab = MutableStateFlow(LibraryTab.LIKED)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    val likedSongs: StateFlow<List<PlayableTrack>> = repository.likedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<PlayableTrack>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.userPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localSongs = MutableStateFlow<List<PlayableTrack>>(emptyList())
    val localSongs: StateFlow<List<PlayableTrack>> = _localSongs.asStateFlow()

    private val _isScanningLocal = MutableStateFlow(false)
    val isScanningLocal: StateFlow<Boolean> = _isScanningLocal.asStateFlow()

    // Downloaded songs flow (offline cache/saved songs)
    val downloadedSongs: StateFlow<List<PlayableTrack>> = repository.likedSongs
        .map { list -> list.take(15) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: LibraryTab) {
        _selectedTab.value = tab
        if (tab == LibraryTab.LOCAL && _localSongs.value.isEmpty()) {
            scanLocalMusic()
        }
    }

    fun scanLocalMusic() {
        viewModelScope.launch {
            _isScanningLocal.value = true
            val tracks = repository.scanLocalAudio()
            _localSongs.value = tracks
            _isScanningLocal.value = false
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun playTrack(track: PlayableTrack, queueList: List<PlayableTrack> = emptyList()) {
        playerManager.playTrack(track, queueList)
    }

    fun playAll(tracks: List<PlayableTrack>) {
        if (tracks.isNotEmpty()) {
            playerManager.playTrack(tracks.first(), tracks)
        }
    }

    fun shuffleAll(tracks: List<PlayableTrack>) {
        if (tracks.isNotEmpty()) {
            val shuffled = tracks.shuffled()
            playerManager.playTrack(shuffled.first(), shuffled)
        }
    }

    fun toggleLike(track: PlayableTrack) {
        viewModelScope.launch {
            repository.toggleLike(track)
        }
    }

    fun addToQueue(track: PlayableTrack) {
        playerManager.addToQueue(track)
    }

    fun playNext(track: PlayableTrack) {
        playerManager.playNext(track)
    }
}
