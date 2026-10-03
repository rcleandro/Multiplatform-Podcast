package br.com.carvalho.podcast.data.remote.model

data class RssFeed(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: List<String>,
    val link: String?,
    val ttl: Int?,
    val episodes: List<RssEpisode>
)

data class RssEpisode(
    val guid: String,
    val title: String,
    val description: String?,
    val enclosureUrl: String,
    val enclosureType: String?,
    val duration: String?,
    val publishDate: String,
    val imageUrl: String?,
    val explicit: Boolean,
    val season: Int?,
    val episode: Int?
)
