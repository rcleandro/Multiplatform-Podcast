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
)
