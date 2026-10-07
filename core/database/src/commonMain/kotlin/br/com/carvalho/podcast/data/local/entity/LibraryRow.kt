package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Embedded

/** A podcast with what the library lists about its episodes, read in one query. */
data class LibraryRow(
    @Embedded val podcast: PodcastEntity,
    val unplayedCount: Int,
    val latestEpisodeDate: Long?,
)
