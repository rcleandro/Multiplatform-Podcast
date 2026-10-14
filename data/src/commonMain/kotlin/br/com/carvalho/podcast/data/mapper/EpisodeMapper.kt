package br.com.carvalho.podcast.data.mapper

import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeType

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
    fileSize = fileSize,
    link = link,
    explicit = explicit,
    isNew = isNew,
    season = season,
    type = EpisodeType.of(episodeType),
    isSaved = isSaved,
    lastPlayedAt = lastPlayedAt,
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
    fileSize = fileSize,
    link = link,
    explicit = explicit,
    isNew = isNew,
    season = season,
    episodeType = type.takeIf { it != EpisodeType.FULL }?.name?.lowercase(),
    isSaved = isSaved,
    lastPlayedAt = lastPlayedAt,
)
