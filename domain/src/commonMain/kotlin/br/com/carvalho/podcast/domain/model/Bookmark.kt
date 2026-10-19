package br.com.carvalho.podcast.domain.model

/** A moment of an episode the user marked, with an optional note (19.3). */
data class Bookmark(val id: Long, val episodeId: String, val positionMs: Long, val note: String, val createdAt: Long)
