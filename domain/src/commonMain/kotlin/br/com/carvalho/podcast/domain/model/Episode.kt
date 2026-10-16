package br.com.carvalho.podcast.domain.model

data class Episode(
    val id: String,
    val podcastId: String,
    val podcastTitle: String? = null,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val imageUrl: String?,
    val duration: Long,
    val publishDate: Long,
    val isPlayed: Boolean,
    val playbackPosition: Long,
    val isDownloaded: Boolean,
    val fileSize: Long?,
    val localPath: String? = null,
    /** The episode's page on the podcast's site, when the feed gives one (18.4). */
    val link: String? = null,
    /** The feed marks this episode as explicit (`itunes:explicit` on the item, 18.5). */
    val explicit: Boolean = false,
    /** In "New" until heard or dismissed (18.7). */
    val isNew: Boolean = false,
    /** `itunes:season`, for the season headers of the podcast's list (18.10). */
    val season: Int? = null,
    val type: EpisodeType = EpisodeType.FULL,
    /** Kept in "Saved" until the user takes it out (18.12). */
    val isSaved: Boolean = false,
    /** When it last started playing (epoch ms), for "History" (18.12); null if never played here. */
    val lastPlayedAt: Long? = null,
    /** When its download finished (epoch ms), for deleting old downloads (18.19); null if not downloaded since. */
    val downloadedAt: Long? = null,
    /** `podcast:chapters`: where the feed keeps the episode's chapters as JSON (19.1). */
    val chaptersUrl: String? = null,
    /** `podcast:transcript` (19.2): where the feed keeps a timed transcript, and its type (VTT, SRT or JSON). */
    val transcriptUrl: String? = null,
    val transcriptType: String? = null,
    /** When played or progress last changed (epoch ms), here or on a synced device (19.9b); 0 never. */
    val updatedAt: Long = 0,
    /** The enclosure's type as the feed gives it (`audio/mpeg`, `video/mp4`); null when it gives none (19.10). */
    val mediaType: String? = null,
) {
    /** A video episode: the player shows its picture where the platform can (19.10). */
    val isVideo: Boolean get() = mediaType?.startsWith("video/", ignoreCase = true) == true
}
