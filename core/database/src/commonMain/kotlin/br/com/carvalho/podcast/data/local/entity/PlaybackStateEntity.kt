package br.com.carvalho.podcast.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,
    val episodeId: String?,
    val position: Long,
    val speed: Float,
    /** When the queue was last arranged (19.9b), added in version 26: the newer queue wins a sync. */
    @ColumnInfo(defaultValue = "0") val queueUpdatedAt: Long = 0,
)
