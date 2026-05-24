package br.com.carvalho.podcast.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Episode(
    val id: String,
    val podcastId: String,
    val podcastTitle: String? = null,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val imageUrl: String?,
    val duration: Long,
    val publishDate: Long,
    val isPlayed: Boolean,
    val playbackPosition: Long,
    val isDownloaded: Boolean,
    val fileSize: Long?,
    val localPath: String? = null
)
