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
    /** Where a chain of permanent redirects (301/308) ended; set by the data source. */
    val permanentRedirect: String? = null,
    /** `itunes:explicit` on the channel: the whole show (18.5). */
    val explicit: Boolean = false,
    /** `itunes:type` is `serial`: the show is heard in order, oldest first (18.10). */
    val serial: Boolean = false,
)

data class RssEpisode(
    /** `null` when the item has no `<guid>`. */
    val guid: String?,
    val title: String,
    val description: String?,
    val enclosureUrl: String,
    /** The episode's page on the podcast's site (`<link>`), shared instead of the audio when there is one (18.4). */
    val link: String? = null,
    val enclosureType: String?,
    val duration: String?,
    val publishDate: String,
    val imageUrl: String?,
    val explicit: Boolean,
    val season: Int?,
    val episode: Int?,
    /** `itunes:episodeType`: full, trailer or bonus (18.10). */
    val episodeType: String? = null,
)
