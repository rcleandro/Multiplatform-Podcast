package br.com.carvalho.podcast.domain.model

/** A podcast found in a directory (Apple Podcasts), not yet in the library: subscribing fetches [feedUrl]. */
data class DirectoryPodcast(
    val title: String,
    val author: String?,
    val artworkUrl: String?,
    val feedUrl: String,
    val genre: String?,
    val episodeCount: Int?,
    /** The directory marks the show as explicit (18.5). */
    val explicit: Boolean = false,
)
