package br.com.carvalho.podcast.data.mapper

import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.domain.model.Podcast
import kotlinx.serialization.json.Json

fun PodcastEntity.toDomain(): Podcast = Podcast(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    author = author,
    language = language,
    categories = Json.decodeFromString(categories),
    feedUrl = feedUrl,
    siteUrl = siteUrl,
    isSubscribed = true,
    lastUpdated = lastUpdated
)

fun Podcast.toEntity(): PodcastEntity = PodcastEntity(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    author = author,
    language = language,
    categories = Json.encodeToString(categories),
    feedUrl = feedUrl,
    siteUrl = siteUrl,
    lastUpdated = lastUpdated,
    isSubscribed = true
)
