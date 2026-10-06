package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.FeedVersion
import br.com.carvalho.podcast.domain.model.Podcast

/** Reads a podcast feed and returns it already as domain models; the feed format stays in the data layer. */
interface FeedSource {
    suspend fun fetch(feedUrl: String): Result<FetchedFeed>

    /** Like [fetch], but `null` when the feed has not changed since [version]. */
    suspend fun fetchIfChanged(feedUrl: String, version: FeedVersion?): Result<FetchedFeed?>
}

/** [declaredUrls]: addresses the feed gives for itself (its canonical and its new address), used to spot duplicates. */
data class FetchedFeed(val podcast: Podcast, val episodes: List<Episode>, val declaredUrls: List<String> = emptyList())
