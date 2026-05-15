package br.com.carvalho.podcast.data.mapper

import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.domain.model.Episode

fun EpisodeEntity.toDomain(): Episode = Episode(
    id = id,
    podcastId = podcastId,
    podcastTitle = podcastTitle,
    title = title,
    description = description,
    audioUrl = audioUrl,
    imageUrl = imageUrl,
    duration = duration,
    publishDate = publishDate,
    isPlayed = isPlayed,
    playbackPosition = playbackPosition,
    isDownloaded = isDownloaded,
    fileSize = fileSize
)

fun Episode.toEntity(): EpisodeEntity = EpisodeEntity(
    id = id,
    podcastId = podcastId,
    podcastTitle = podcastTitle,
    title = title,
    description = description,
    audioUrl = audioUrl,
    imageUrl = imageUrl,
    duration = duration,
    publishDate = publishDate,
    isPlayed = isPlayed,
    playbackPosition = playbackPosition,
    isDownloaded = isDownloaded,
    fileSize = fileSize
)
