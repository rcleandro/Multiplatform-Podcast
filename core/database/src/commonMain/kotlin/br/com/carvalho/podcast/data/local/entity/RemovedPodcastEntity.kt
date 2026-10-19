package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A podcast taken out of the library and when (19.9b), so a synced device does not bring it back. */
@Entity(tableName = "removed_podcasts")
data class RemovedPodcastEntity(
    @PrimaryKey val feedUrl: String,
    val removedAt: Long,
)
