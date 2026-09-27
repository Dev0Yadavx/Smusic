package com.example.data.model

enum class TrackSource {
    JIOSAAVN, LOCAL, DOWNLOAD
}

data class PlayableTrack(
    val id: String,
    val token: String = "",
    val title: String,
    val artist: String,
    val album: String = "",
    val albumId: String = "",
    val artwork: String = "",
    val duration: Long = 0L, // in seconds
    val streamUrl: String = "",
    val encryptedMediaUrl: String = "",
    val mediaPreviewUrl: String = "",
    val lyricsId: String = "",
    val lyricsSnippet: String = "",
    val source: TrackSource = TrackSource.JIOSAAVN,
    val year: String = "",
    val language: String = "",
    val copyright: String = ""
) {
    val durationFormatted: String
        get() {
            if (duration <= 0) return "0:00"
            val m = duration / 60
            val s = duration % 60
            return "%d:%02d".format(m, s)
        }
}
