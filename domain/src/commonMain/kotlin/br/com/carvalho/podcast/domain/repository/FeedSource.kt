package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.FeedVersion
import br.com.carvalho.podcast.domain.model.Podcast

/** Reads a podcast feed and returns it already as domain models; the feed format stays in the data layer. */
interface FeedSource {
    /** [podcastId] scopes the episode ids; it stays the same when the feed moves to another address. */
    suspend fun fetch(feedUrl: String, podcastId: String = feedUrl): Result<FetchedFeed>

    /** Like [fetch], but `null` when the feed has not changed since [version]. */
    suspend fun fetchIfChanged(
        feedUrl: String,
        version: FeedVersion?,
        podcastId: String = feedUrl,
    ): Result<FetchedFeed?>
}

/**
 * [declaredUrls]: addresses the feed gives for itself (canonical, new, redirect target), used to spot duplicates.
 * [movedTo]: where the feed says it moved (permanent redirect or `itunes:new-feed-url`), not yet validated.
 */
data class FetchedFeed(
    val podcast: Podcast,
    val episodes: List<Episode>,
    val declaredUrls: List<String> = emptyList(),
    val movedTo: String? = null,
)
