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
    val isNew: Boolean = false
)
