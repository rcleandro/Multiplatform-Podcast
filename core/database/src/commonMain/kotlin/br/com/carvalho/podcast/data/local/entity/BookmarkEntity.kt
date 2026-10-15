package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A marked moment of an episode (19.3), added in version 22; it goes with the episode when the podcast is removed. */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("episodeId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val episodeId: String,
    val positionMs: Long,
    val note: String,
    val createdAt: Long,
)
