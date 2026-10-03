package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** The player's queue, by reference: the episode rows stay the source of truth, and removed episodes leave it. */
@Entity(
    tableName = "queue_items",
    foreignKeys = [ForeignKey(
        entity = EpisodeEntity::class,
        parentColumns = ["id"],
        childColumns = ["episodeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("episodeId")]
)
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val episodeId: String,
)
