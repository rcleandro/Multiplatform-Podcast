package br.com.carvalho.podcast.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Podcast(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: List<String>,
    val feedUrl: String,
    val siteUrl: String?,
    val lastUpdated: Long,
    val isSubscribed: Boolean,
    val episodeCount: Int = 0
)
