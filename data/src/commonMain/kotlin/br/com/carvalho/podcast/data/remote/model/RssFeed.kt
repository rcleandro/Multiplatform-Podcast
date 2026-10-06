package br.com.carvalho.podcast.data.remote.model

import br.com.carvalho.podcast.domain.model.FeedVersion

data class RssFeed(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: List<String>,
    val link: String?,
    val ttl: Int?,
    val episodes: List<RssEpisode>,
    /** `atom:link rel="self"`: the address the feed gives for itself. */
    val selfUrl: String? = null,
    /** `itunes:new-feed-url`: where the feed moved to. */
    val newFeedUrl: String? = null,
    /** The server's `ETag`/`Last-Modified` for this copy; set by the data source, not the parser. */
    val version: FeedVersion? = null,
)

data class RssEpisode(
    /** `null` when the item has no `<guid>`. */
    val guid: String?,
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
