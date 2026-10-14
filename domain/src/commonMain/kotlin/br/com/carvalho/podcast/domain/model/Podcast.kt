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
    /** Whether new episodes found in the background say so in a notification (18.18). */
    val notifyNewEpisodes: Boolean = true,
    /** How many of the newest unheard episodes stay downloaded (18.19); 0 downloads nothing by itself. */
    val autoDownloadCount: Int = 0,
) {
    /** Serial shows list their episodes oldest first unless the user chose otherwise. */
    val episodesOldestFirst: Boolean get() = oldestFirst ?: serial
}
