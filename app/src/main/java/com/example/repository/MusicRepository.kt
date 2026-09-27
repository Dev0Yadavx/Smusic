package com.example.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.*
import com.example.data.mapper.*
import com.example.data.model.*
import com.example.data.remote.JioSaavnApiService
import com.example.data.remote.NetworkResult
import com.example.data.remote.RetrofitClient
import com.example.data.remote.StreamUrlResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val api: JioSaavnApiService = RetrofitClient.apiService,
    private val db: SMusicDatabase = SMusicDatabase.getInstance(context)
) {
    private val tag = "MusicRepository"
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val searchHistoryDao = db.searchHistoryDao()

    // Cached home shelves for instant startup & offline support
    @Volatile
    private var cachedShelves: List<MusicShelf> = emptyList()

    /**
     * Loads Home content with shelves (Trending, Albums, Playlists, etc.)
     */
    fun getHomeContent(languages: String = "hindi,english,punjabi,bhojpuri,haryanvi"): Flow<NetworkResult<List<MusicShelf>>> = flow {
        emit(NetworkResult.Loading)
        if (cachedShelves.isNotEmpty()) {
            emit(NetworkResult.Success(cachedShelves))
        }

        try {
            val response = api.getLaunchData(languages = languages)
            if (response.isSuccessful && response.body() != null) {
                val shelves = HomeMapper.map(response.body()!!)
                cachedShelves = shelves
                emit(NetworkResult.Success(shelves))
            } else {
                if (cachedShelves.isEmpty()) {
                    emit(NetworkResult.Error("Failed to load music: ${response.code()} ${response.message()}"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Home content error: ${e.message}", e)
            if (cachedShelves.isEmpty()) {
                emit(NetworkResult.Error("Unable to connect to music service. Please check your internet connection.", e))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Search songs
     */
    suspend fun searchSongs(query: String, page: Int = 1): NetworkResult<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchSongs(query = query, page = page)
            if (response.isSuccessful && response.body() != null) {
                val resultsArr = response.body()!!.getAsJsonArray("results")
                val songs = SongMapper.mapList(resultsArr)
                NetworkResult.Success(songs)
            } else {
                NetworkResult.Error("Search failed: ${response.message()}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown search error", e)
        }
    }

    /**
     * Search albums
     */
    suspend fun searchAlbums(query: String, page: Int = 1): NetworkResult<List<Album>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchAlbums(query = query, page = page)
            if (response.isSuccessful && response.body() != null) {
                val resultsArr = response.body()!!.getAsJsonArray("results")
                val albums = AlbumMapper.mapList(resultsArr)
                NetworkResult.Success(albums)
            } else {
                NetworkResult.Error("Album search failed")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Album search error", e)
        }
    }

    /**
     * Search playlists
     */
    suspend fun searchPlaylists(query: String, page: Int = 1): NetworkResult<List<Playlist>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchPlaylists(query = query, page = page)
            if (response.isSuccessful && response.body() != null) {
                val resultsArr = response.body()!!.getAsJsonArray("results")
                val playlists = PlaylistMapper.mapList(resultsArr)
                NetworkResult.Success(playlists)
            } else {
                NetworkResult.Error("Playlist search failed")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Playlist search error", e)
        }
    }

    /**
     * Search artists
     */
    suspend fun searchArtists(query: String, page: Int = 1): NetworkResult<List<Artist>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchArtists(query = query, page = page)
            if (response.isSuccessful && response.body() != null) {
                val resultsArr = response.body()!!.getAsJsonArray("results")
                val artists = ArtistMapper.mapList(resultsArr)
                NetworkResult.Success(artists)
            } else {
                NetworkResult.Error("Artist search failed")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Artist search error", e)
        }
    }

    /**
     * Autocomplete suggestions
     */
    suspend fun getAutocomplete(query: String): SearchResultCategory = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext SearchResultCategory(query)
        try {
            val response = api.getAutocomplete(query = query)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val songs = body.getAsJsonObject("songs")?.getAsJsonArray("data")?.let { SongMapper.mapList(it) } ?: emptyList()
                val albums = body.getAsJsonObject("albums")?.getAsJsonArray("data")?.let { AlbumMapper.mapList(it) } ?: emptyList()
                val artists = body.getAsJsonObject("artists")?.getAsJsonArray("data")?.let { ArtistMapper.mapList(it) } ?: emptyList()
                val playlists = body.getAsJsonObject("playlists")?.getAsJsonArray("data")?.let { PlaylistMapper.mapList(it) } ?: emptyList()
                return@withContext SearchResultCategory(query, songs, albums, artists, playlists)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Autocomplete error: ${e.message}")
        }
        SearchResultCategory(query)
    }

    /**
     * Album details
     */
    suspend fun getAlbumDetails(albumId: String): NetworkResult<Album> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAlbumDetails(albumId = albumId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(AlbumMapper.map(response.body()!!))
            } else {
                NetworkResult.Error("Failed to load album details")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to load album", e)
        }
    }

    /**
     * Playlist details
     */
    suspend fun getPlaylistDetails(playlistId: String): NetworkResult<Playlist> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPlaylistDetails(listId = playlistId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(PlaylistMapper.map(response.body()!!))
            } else {
                NetworkResult.Error("Failed to load playlist details")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to load playlist", e)
        }
    }

    /**
     * Artist details
     */
    suspend fun getArtistDetails(artistId: String): NetworkResult<Artist> = withContext(Dispatchers.IO) {
        try {
            val response = api.getArtistDetails(artistId = artistId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(ArtistMapper.map(response.body()!!))
            } else {
                NetworkResult.Error("Failed to load artist details")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to load artist", e)
        }
    }

    /**
     * Song details (with numeric PID and string Token fallback)
     */
    suspend fun getSongDetails(songId: String): Song? = withContext(Dispatchers.IO) {
        try {
            // First try by PID if numeric, otherwise try by Token
            val response = if (songId.all { it.isDigit() }) {
                api.getSongByPid(pid = songId)
            } else {
                api.getSongByToken(token = songId)
            }
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val songsArr = body.getAsJsonArray("songs") ?: body.getAsJsonArray("results")
                if (songsArr != null && songsArr.size() > 0) {
                    return@withContext SongMapper.map(songsArr.get(0).asJsonObject)
                }
            }
            // Fallback: try the other method if first returned empty
            if (songId.all { it.isDigit() }) {
                val fallbackResp = api.getSongByToken(token = songId)
                if (fallbackResp.isSuccessful && fallbackResp.body() != null) {
                    val fallbackArr = fallbackResp.body()!!.getAsJsonArray("songs")
                    if (fallbackArr != null && fallbackArr.size() > 0) {
                        return@withContext SongMapper.map(fallbackArr.get(0).asJsonObject)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Song details error: ${e.message}")
        }
        null
    }

    /**
     * Recommendations / Autoplay next songs
     */
    suspend fun getRecommendations(songId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRecommendations(pid = songId)
            if (response.isSuccessful && response.body() != null) {
                val songs = RecommendationMapper.map(response.body()!!)
                if (songs.isNotEmpty()) return@withContext songs
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Recommendations error: ${e.message}")
        }
        // Fallback: use trending songs or cached home shelf if available
        cachedShelves.firstOrNull { it.id == "trending" }?.items?.mapNotNull {
            if (it is ShelfItem.SongItem) it.song else null
        } ?: emptyList()
    }

    /**
     * Lyrics
     */
    suspend fun getLyrics(lyricsId: String, song: Song?): String = withContext(Dispatchers.IO) {
        if (lyricsId.isNotBlank()) {
            try {
                val response = api.getLyrics(lyricsId = lyricsId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val text = LyricsMapper.map(body)
                    if (text.isNotBlank()) return@withContext text
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "Lyrics endpoint error: ${e.message}")
            }
        }

        // Fallback to song's lyrics snippet if present
        if (!song?.lyricsSnippet.isNullOrBlank()) {
            return@withContext song!!.lyricsSnippet
        }
        "Lyrics not available for this song."
    }

    /**
     * Stream URL resolution
     */
    suspend fun resolveStreamUrl(
        track: PlayableTrack,
        quality: StreamUrlResolver.AudioQuality = StreamUrlResolver.AudioQuality.HIGH
    ): String? = StreamUrlResolver.resolve(track, quality)

    // --- LOCAL PERSISTENCE ---

    val likedSongs: Flow<List<PlayableTrack>> = songDao.getAllLikedSongs()
        .map { list -> list.map { it.toPlayableTrack() } }
        .flowOn(Dispatchers.IO)

    fun isSongLiked(id: String): Flow<Boolean> = songDao.isLiked(id).flowOn(Dispatchers.IO)

    suspend fun toggleLike(track: PlayableTrack) = withContext(Dispatchers.IO) {
        val currentlyLiked = songDao.isLikedSync(track.id)
        if (currentlyLiked) {
            songDao.deleteLikedSong(track.id)
        } else {
            songDao.insertLikedSong(
                LikedSongEntity(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    albumId = track.albumId,
                    artwork = track.artwork,
                    duration = track.duration,
                    streamUrl = track.streamUrl,
                    encryptedMediaUrl = track.encryptedMediaUrl,
                    mediaPreviewUrl = track.mediaPreviewUrl
                )
            )
        }
    }

    val recentlyPlayed: Flow<List<PlayableTrack>> = songDao.getRecentlyPlayed()
        .map { list -> list.map { it.toPlayableTrack() } }
        .flowOn(Dispatchers.IO)

    suspend fun recordRecentlyPlayed(track: PlayableTrack) = withContext(Dispatchers.IO) {
        songDao.deleteRecentlyPlayedBySongId(track.id)
        songDao.insertRecentlyPlayed(
            RecentlyPlayedEntity(
                songId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                albumId = track.albumId,
                artwork = track.artwork,
                duration = track.duration,
                streamUrl = track.streamUrl,
                encryptedMediaUrl = track.encryptedMediaUrl,
                mediaPreviewUrl = track.mediaPreviewUrl,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRecentlyPlayed() = withContext(Dispatchers.IO) {
        songDao.clearRecentlyPlayed()
    }

    // Playlists
    val userPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists().flowOn(Dispatchers.IO)

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name, description = description))
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
        playlistDao.deleteSongsByPlaylist(id)
    }

    fun getPlaylistSongs(playlistId: Long): Flow<List<PlayableTrack>> =
        playlistDao.getSongsForPlaylist(playlistId)
            .map { list -> list.map { it.toPlayableTrack() } }
            .flowOn(Dispatchers.IO)

    suspend fun addSongToPlaylist(playlistId: Long, track: PlayableTrack) = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                albumId = track.albumId,
                artwork = track.artwork,
                duration = track.duration,
                streamUrl = track.streamUrl,
                encryptedMediaUrl = track.encryptedMediaUrl
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    // Search History
    val searchHistory: Flow<List<String>> = searchHistoryDao.getRecentSearches()
        .map { list -> list.map { it.query } }
        .flowOn(Dispatchers.IO)

    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            searchHistoryDao.deleteSearch(query.trim())
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearHistory()
    }

    /**
     * Scans local audio files from MediaStore
     */
    suspend fun scanLocalAudio(): List<PlayableTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<PlayableTrack>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val durationMs = cursor.getLong(durationCol)
                    val year = cursor.getString(yearCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )

                    // Album art URI
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    tracks.add(
                        PlayableTrack(
                            id = "local_$mediaId",
                            title = title,
                            artist = artist,
                            album = album,
                            artwork = artworkUri,
                            duration = durationMs / 1000,
                            streamUrl = contentUri.toString(),
                            source = TrackSource.LOCAL,
                            year = year
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Local audio scan failed: ${e.message}", e)
        }
        tracks
    }
}
