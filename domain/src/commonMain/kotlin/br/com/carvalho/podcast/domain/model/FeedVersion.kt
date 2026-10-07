package br.com.carvalho.podcast.domain.model

/** What the feed's server said about the copy we have (`ETag`, `Last-Modified`), sent back to skip unchanged feeds. */
data class FeedVersion(val etag: String?, val lastModified: String?)
