package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetLibraryStatsUseCaseTest {

    private val repository = FakePodcastRepository()
    private val useCase = GetLibraryStatsUseCase(repository)

    @Test
    fun `combines data from repository and returns stats`() = runTest {
        repository.podcasts.value = listOf(createPodcast("1"), createPodcast("2"))
        repository.episodes.value = listOf(
            createEpisode("e1", isPlayed = false, isDownloaded = true),
            createEpisode("e2", isPlayed = false, isDownloaded = false),
            createEpisode("e3", isPlayed = false, isDownloaded = false),
            createEpisode("e4", isPlayed = true, isDownloaded = true)
        )

        val stats = useCase().first()

        assertEquals(2, stats.podcastCount)
        assertEquals(3, stats.unplayedCount)
        assertEquals(2, stats.downloadCount)
    }

    private fun createPodcast(id: String) = Podcast(
        id = id,
        title = "Title $id",
        description = "Desc",
        imageUrl = null,
        author = null,
        language = null,
        categories = emptyList(),
        feedUrl = "url",
        siteUrl = null,
        lastUpdated = 0L,
        isSubscribed = true
    )

    private fun createEpisode(id: String, isPlayed: Boolean, isDownloaded: Boolean) = Episode(
        id = id,
        podcastId = "p1",
        title = "Title $id",
        description = null,
        audioUrl = "url",
        imageUrl = null,
        duration = 0L,
        publishDate = 0L,
        isPlayed = isPlayed,
        playbackPosition = 0L,
        isDownloaded = isDownloaded,
        fileSize = null
    )
}
