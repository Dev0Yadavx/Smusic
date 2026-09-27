package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.PlayableTrack
import com.example.data.model.TrackSource
import com.example.data.remote.StreamUrlResolver
import com.example.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode {
    OFF, ALL, ONE
}

class PlayerManager private constructor(private val appContext: Context) {

    private val tag = "PlayerManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repository = MusicRepository(appContext)

    private var exoPlayer: ExoPlayer? = null
    private var serviceContext: Context? = null

    // State flows
    private val _currentTrack = MutableStateFlow<PlayableTrack?>(null)
    val currentTrack: StateFlow<PlayableTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<PlayableTrack>>(emptyList())
    val queue: StateFlow<List<PlayableTrack>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var progressJob: Job? = null
    private var originalQueueBeforeShuffle: List<PlayableTrack> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _isBuffering.value = true
                }
                Player.STATE_READY -> {
                    _isBuffering.value = false
                    _errorMessage.value = null
                    val dur = exoPlayer?.duration ?: 0L
                    if (dur > 0) _durationMs.value = dur
                }
                Player.STATE_ENDED -> {
                    _isBuffering.value = false
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _isBuffering.value = false
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(tag, "Playback error: ${error.message}", error)
            _isBuffering.value = false
            handlePlaybackError(error)
        }
    }

    init {
        getOrCreatePlayer()
    }

    private fun ensureServiceStarted() {
        try {
            val intent = Intent(appContext, MusicPlaybackService::class.java)
            appContext.startService(intent)
        } catch (e: Exception) {
            Log.w(tag, "Service start deferred: ${e.message}")
        }
    }

    @OptIn(UnstableApi::class)
    fun getOrCreatePlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(appContext).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(playerListener)
        player.volume = _volume.value
        exoPlayer = player
        syncPlayerModes()
        return player
    }

    fun attachPlayer(player: ExoPlayer, service: MusicPlaybackService) {
        if (exoPlayer !== player) {
            exoPlayer?.removeListener(playerListener)
            exoPlayer?.release()
            exoPlayer = player
            player.addListener(playerListener)
            player.volume = _volume.value
            syncPlayerModes()
        }
        serviceContext = service
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        exoPlayer?.let { player ->
            _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
        }
    }

    fun playTrack(track: PlayableTrack, newQueue: List<PlayableTrack> = emptyList()) {
        scope.launch {
            _errorMessage.value = null

            val currentQ = if (newQueue.isNotEmpty()) {
                newQueue
            } else {
                val existing = _queue.value.toMutableList()
                if (!existing.any { it.id == track.id }) {
                    existing.add(track)
                }
                existing
            }

            _queue.value = currentQ
            val idx = currentQ.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            _currentIndex.value = idx

            startPlaybackForTrack(track)
        }
    }

    private suspend fun startPlaybackForTrack(track: PlayableTrack) {
        _currentTrack.value = track
        _isBuffering.value = true
        _currentPositionMs.value = 0L
        _durationMs.value = if (track.duration > 0) track.duration * 1000 else 0L

        // Record recently played
        repository.recordRecentlyPlayed(track)

        // Resolve stream URL if not ready
        val resolvedUrl = repository.resolveStreamUrl(track)
        if (resolvedUrl.isNullOrBlank()) {
            _isBuffering.value = false
            _errorMessage.value = "Unable to resolve stream URL for ${track.title}. Retry?"
            return
        }

        val updatedTrack = track.copy(streamUrl = resolvedUrl)
        _currentTrack.value = updatedTrack

        ensureServiceStarted()

        val player = getOrCreatePlayer()
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.artwork.isNotBlank()) Uri.parse(track.artwork) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(resolvedUrl)
            .setMediaMetadata(mediaMetadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun handlePlaybackError(error: PlaybackException) {
        val current = _currentTrack.value ?: return
        val currentUrl = current.streamUrl

        // Try fallback quality
        val fallbackUrl = StreamUrlResolver.getFallbackUrl(currentUrl)
        if (fallbackUrl != null) {
            Log.d(tag, "Retrying with fallback stream quality: $fallbackUrl")
            val fallbackTrack = current.copy(streamUrl = fallbackUrl)
            _currentTrack.value = fallbackTrack
            val player = getOrCreatePlayer()
            val mediaItem = MediaItem.Builder()
                .setMediaId(fallbackTrack.id)
                .setUri(fallbackUrl)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } else if (current.mediaPreviewUrl.isNotBlank() && currentUrl != current.mediaPreviewUrl) {
            Log.d(tag, "Retrying with media preview URL: ${current.mediaPreviewUrl}")
            val previewUrl = current.mediaPreviewUrl.replace("http://", "https://")
            val previewTrack = current.copy(streamUrl = previewUrl)
            _currentTrack.value = previewTrack
            val player = getOrCreatePlayer()
            val mediaItem = MediaItem.Builder()
                .setMediaId(previewTrack.id)
                .setUri(previewUrl)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } else {
            _errorMessage.value = "Unable to play this song. Tap Retry."
        }
    }

    fun retryCurrentTrack() {
        val current = _currentTrack.value ?: return
        scope.launch {
            startPlaybackForTrack(current)
        }
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                resume()
            }
            RepeatMode.ALL -> {
                skipToNext()
            }
            RepeatMode.OFF -> {
                val q = _queue.value
                val nextIdx = _currentIndex.value + 1
                if (nextIdx < q.size) {
                    skipToNext()
                } else if (_isAutoplayEnabled.value) {
                    // Trigger Autoplay recommendations!
                    triggerAutoplay()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun triggerAutoplay() {
        val current = _currentTrack.value ?: return
        scope.launch {
            _isBuffering.value = true
            val recos = repository.getRecommendations(current.id)
            if (recos.isNotEmpty()) {
                val newTracks = recos.map { it.toPlayableTrack() }
                val updatedQueue = _queue.value + newTracks
                _queue.value = updatedQueue
                skipToNext()
            } else {
                _isPlaying.value = false
                _isBuffering.value = false
            }
        }
    }

    fun playPause() {
        val player = getOrCreatePlayer()
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && _currentTrack.value != null) {
                retryCurrentTrack()
            } else {
                player.play()
            }
        }
    }

    fun pause() {
        getOrCreatePlayer().pause()
    }

    fun resume() {
        getOrCreatePlayer().play()
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        getOrCreatePlayer().seekTo(positionMs)
    }

    fun skipToNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        var nextIdx = _currentIndex.value + 1
        if (nextIdx >= q.size) {
            if (_repeatMode.value == RepeatMode.ALL) {
                nextIdx = 0
            } else if (_isAutoplayEnabled.value) {
                triggerAutoplay()
                return
            } else {
                return
            }
        }
        _currentIndex.value = nextIdx
        val track = q[nextIdx]
        scope.launch {
            startPlaybackForTrack(track)
        }
    }

    fun skipToPrevious() {
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000L) {
            seekTo(0)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return
        var prevIdx = _currentIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = if (_repeatMode.value == RepeatMode.ALL) q.size - 1 else 0
        }
        _currentIndex.value = prevIdx
        val track = q[prevIdx]
        scope.launch {
            startPlaybackForTrack(track)
        }
    }

    fun toggleRepeat() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        syncPlayerModes()
    }

    fun toggleShuffle() {
        val enabled = !_isShuffleEnabled.value
        _isShuffleEnabled.value = enabled
        val q = _queue.value
        val curTrack = _currentTrack.value

        if (enabled) {
            originalQueueBeforeShuffle = q
            if (curTrack != null) {
                val others = q.filter { it.id != curTrack.id }.shuffled()
                _queue.value = listOf(curTrack) + others
                _currentIndex.value = 0
            } else {
                _queue.value = q.shuffled()
            }
        } else {
            if (originalQueueBeforeShuffle.isNotEmpty()) {
                _queue.value = originalQueueBeforeShuffle
                curTrack?.let { ct ->
                    _currentIndex.value = originalQueueBeforeShuffle.indexOfFirst { it.id == ct.id }.coerceAtLeast(0)
                }
            }
        }
        syncPlayerModes()
    }

    private fun syncPlayerModes() {
        exoPlayer?.let { player ->
            when (_repeatMode.value) {
                RepeatMode.OFF -> player.repeatMode = Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> player.repeatMode = Player.REPEAT_MODE_ALL
                RepeatMode.ONE -> player.repeatMode = Player.REPEAT_MODE_ONE
            }
            player.shuffleModeEnabled = _isShuffleEnabled.value
        }
    }

    fun addToQueue(track: PlayableTrack) {
        val updated = _queue.value.toMutableList()
        updated.add(track)
        _queue.value = updated
    }

    fun playNext(track: PlayableTrack) {
        val updated = _queue.value.toMutableList()
        val insertIdx = (_currentIndex.value + 1).coerceIn(0, updated.size)
        updated.add(insertIdx, track)
        _queue.value = updated
    }

    fun removeFromQueue(index: Int) {
        val updated = _queue.value.toMutableList()
        if (index in updated.indices) {
            val isCurrent = index == _currentIndex.value
            updated.removeAt(index)
            _queue.value = updated
            if (isCurrent) {
                if (updated.isNotEmpty()) {
                    val nextIdx = index.coerceAtMost(updated.size - 1)
                    _currentIndex.value = nextIdx
                    scope.launch { startPlaybackForTrack(updated[nextIdx]) }
                } else {
                    _currentTrack.value = null
                    _isPlaying.value = false
                    exoPlayer?.stop()
                }
            } else if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        _currentTrack.value = null
        _isPlaying.value = false
        exoPlayer?.stop()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val updated = _queue.value.toMutableList()
        if (from in updated.indices && to in updated.indices) {
            val item = updated.removeAt(from)
            updated.add(to, item)
            _queue.value = updated
            val cur = _currentTrack.value
            if (cur != null) {
                _currentIndex.value = updated.indexOfFirst { it.id == cur.id }
            }
        }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        _isAutoplayEnabled.value = enabled
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        exoPlayer?.volume = clamped
    }

    companion object {
        @Volatile
        private var INSTANCE: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
