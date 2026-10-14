package br.com.carvalho.podcast.domain.model

data class Podcast(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: List<String>,
    val feedUrl: String,
    val siteUrl: String?,
    val lastUpdated: Long,
    val isSubscribed: Boolean,
    val episodeCount: Int = 0,
    val feedVersion: FeedVersion? = null,
    /** When the podcast entered the library (epoch ms); refreshing the feed keeps it. */
    val subscribedAt: Long = 0,
    /** Place in the custom order of the library; a new podcast goes to the end. */
    val position: Int = 0,
    /** The feed marks the whole show as explicit (`itunes:explicit` on the channel, 18.5). */
    val explicit: Boolean = false,
    /** The feed says the show is heard in order (`itunes:type` serial, 18.10). */
    val serial: Boolean = false,
    /** The order the user chose for the episodes; `null` follows the feed. */
    val oldestFirst: Boolean? = null,
    val playback: PodcastPlayback = PodcastPlayback(),
) {
    /** Serial shows list their episodes oldest first unless the user chose otherwise. */
    val episodesOldestFirst: Boolean get() = oldestFirst ?: serial
}
