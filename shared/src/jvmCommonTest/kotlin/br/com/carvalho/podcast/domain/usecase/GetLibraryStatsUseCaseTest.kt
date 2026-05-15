package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetLibraryStatsUseCaseTest {

    private val repository = mockk<PodcastRepository>()
    private val useCase = GetLibraryStatsUseCase(repository)

    @Test
    fun `combines data from repository and returns stats`() = runTest {
        val podcasts = listOf(mockk<Podcast>(), mockk<Podcast>())
        val unplayed = listOf(mockk<Episode>(), mockk<Episode>(), mockk<Episode>())
        val downloaded = listOf(mockk<Episode>())

        every { repository.getPodcasts() } returns flowOf(podcasts)
        every { repository.getUnplayedEpisodes() } returns flowOf(unplayed)
        every { repository.getDownloadedEpisodes() } returns flowOf(downloaded)

        val stats = useCase().first()

        assertEquals(2, stats.podcastCount)
        assertEquals(3, stats.unplayedCount)
        assertEquals(1, stats.downloadCount)
    }
}
