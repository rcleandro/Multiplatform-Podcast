package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,
    val episodeId: String?,
    val position: Long,
    val speed: Float,
    val queueJson: String
)
