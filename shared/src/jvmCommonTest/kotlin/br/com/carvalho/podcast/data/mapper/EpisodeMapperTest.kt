package br.com.carvalho.podcast.data.mapper

import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.domain.model.Episode
import kotlin.test.Test
import kotlin.test.assertEquals

class EpisodeMapperTest {

    @Test
    fun `toDomain maps entity to domain model correctly`() {
        val entity = EpisodeEntity(
            id = "e1",
            podcastId = "p1",
            podcastTitle = "Podcast",
            title = "Episode",
            description = "Desc",
            audioUrl = "url",
            imageUrl = "img",
            duration = 1000L,
            publishDate = 123L,
            isPlayed = true,
            playbackPosition = 500L,
            isDownloaded = true,
            fileSize = 1024L
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.podcastId, domain.podcastId)
        assertEquals(entity.isPlayed, domain.isPlayed)
        assertEquals(entity.playbackPosition, domain.playbackPosition)
    }

    @Test
    fun `toEntity maps domain model to entity correctly`() {
        val domain = Episode(
            id = "e1",
            podcastId = "p1",
            podcastTitle = "Podcast",
            title = "Episode",
            description = "Desc",
            audioUrl = "url",
            imageUrl = "img",
            duration = 1000L,
            publishDate = 123L,
            isPlayed = true,
            playbackPosition = 500L,
            isDownloaded = true,
            fileSize = 1024L
        )

        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.podcastId, entity.podcastId)
        assertEquals(domain.isPlayed, entity.isPlayed)
        assertEquals(domain.playbackPosition, entity.playbackPosition)
    }
}
