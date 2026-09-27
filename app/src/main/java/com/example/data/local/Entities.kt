package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PlayableTrack
import com.example.data.model.Song
import com.example.data.model.TrackSource

@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val albumId: String = "",
    val artwork: String = "",
    val duration: Long = 0L,
    val streamUrl: String = "",
    val encryptedMediaUrl: String = "",
    val mediaPreviewUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artwork = artwork,
        duration = duration,
        streamUrl = streamUrl,
        encryptedMediaUrl = encryptedMediaUrl,
        mediaPreviewUrl = mediaPreviewUrl
    )

    fun toPlayableTrack(): PlayableTrack = PlayableTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artwork = artwork,
        duration = duration,
        streamUrl = streamUrl,
        encryptedMediaUrl = encryptedMediaUrl,
        mediaPreviewUrl = mediaPreviewUrl,
        source = TrackSource.JIOSAAVN
    )
}

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val albumId: String = "",
    val artwork: String = "",
    val duration: Long = 0L,
    val streamUrl: String = "",
    val encryptedMediaUrl: String = "",
    val mediaPreviewUrl: String = "",
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artwork = artwork,
        duration = duration,
        streamUrl = streamUrl,
        encryptedMediaUrl = encryptedMediaUrl,
        mediaPreviewUrl = mediaPreviewUrl
    )

    fun toPlayableTrack(): PlayableTrack = PlayableTrack(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artwork = artwork,
        duration = duration,
        streamUrl = streamUrl,
        encryptedMediaUrl = encryptedMediaUrl,
        mediaPreviewUrl = mediaPreviewUrl
    )
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val artwork: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs")
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playlistId: Long,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val albumId: String = "",
    val artwork: String = "",
    val duration: Long = 0L,
    val streamUrl: String = "",
    val encryptedMediaUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artwork = artwork,
        duration = duration,
        streamUrl = streamUrl,
        encryptedMediaUrl = encryptedMediaUrl
    )

    fun toPlayableTrack(): PlayableTrack = PlayableTrack(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artwork = artwork,
        duration = duration,
        streamUrl = streamUrl,
        encryptedMediaUrl = encryptedMediaUrl
    )
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
