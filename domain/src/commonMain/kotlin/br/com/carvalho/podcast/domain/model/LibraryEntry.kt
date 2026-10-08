package br.com.carvalho.podcast.domain.model

/** A podcast as the library shows it: with its unplayed count and the date of its newest episode (null if none). */
data class LibraryEntry(
    val podcast: Podcast,
    val unplayedCount: Int,
    val latestEpisodeDate: Long?,
)
