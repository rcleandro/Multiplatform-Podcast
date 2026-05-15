package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.local.dao.PodcastDao
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.domain.model.Podcast
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PodcastRepositoryImplTest {

    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val repository = PodcastRepositoryImpl(podcastDao, episodeDao)

    private val podcastId = "p1"
    private val podcastEntity = PodcastEntity(
        id = podcastId,
        title = "Test Podcast",
        description = "Desc",
        imageUrl = "img",
        author = "Author",
        language = "pt",
        categories = """["Cat1", "Cat2"]""",
        feedUrl = "feed",
        siteUrl = "site",
        lastUpdated = 123L,
        isSubscribed = true
    )

    private val episodeEntity = EpisodeEntity(
        id = "e1",
        podcastId = podcastId,
        podcastTitle = "Test Podcast",
        title = "Ep 1",
        description = "Desc",
        audioUrl = "audio",
        imageUrl = "img",
        duration = 1000L,
        publishDate = 456L,
        isPlayed = false,
        playbackPosition = 0L,
        isDownloaded = false,
        fileSize = null
    )

    @Test
    fun `getPodcasts maps and returns podcasts from dao`() = runTest {
        every { podcastDao.getAll() } returns flowOf(listOf(podcastEntity))

        val podcasts = repository.getPodcasts().first()

        assertEquals(1, podcasts.size)
        assertEquals(podcastId, podcasts[0].id)
        assertEquals("Test Podcast", podcasts[0].title)
    }

    @Test
    fun `getPodcastById returns mapped podcast if exists`() = runTest {
        coEvery { podcastDao.getById(podcastId) } returns podcastEntity

        val podcast = repository.getPodcastById(podcastId)

        assertEquals(podcastId, podcast?.id)
        assertEquals("Test Podcast", podcast?.title)
    }

    @Test
    fun `getPodcastById returns null if not exists`() = runTest {
        coEvery { podcastDao.getById("non-existent") } returns null

        val podcast = repository.getPodcastById("non-existent")

        assertNull(podcast)
    }

    @Test
    fun `getEpisodes returns mapped episodes for podcast`() = runTest {
        every { episodeDao.getByPodcast(podcastId) } returns flowOf(listOf(episodeEntity))

        val episodes = repository.getEpisodes(podcastId).first()

        assertEquals(1, episodes.size)
        assertEquals("e1", episodes[0].id)
        assertEquals(podcastId, episodes[0].podcastId)
    }

    @Test
    fun `savePodcast delegates to dao`() = runTest {
        val podcast = Podcast(
            id = podcastId,
            title = "Test Podcast",
            description = "Desc",
            imageUrl = "img",
            author = "Author",
            language = "pt",
            categories = listOf("Cat1", "Cat2"),
            feedUrl = "feed",
            siteUrl = "site",
            lastUpdated = 123L,
            isSubscribed = true
        )

        repository.savePodcast(podcast)

        coVerify { podcastDao.insert(any()) }
    }

    @Test
    fun `deletePodcast deletes podcast and its episodes`() = runTest {
        repository.deletePodcast(podcastId)

        coVerify { episodeDao.deleteByPodcast(podcastId) }
        coVerify { podcastDao.deleteById(podcastId) }
    }

    @Test
    fun `updateEpisodeProgress updates playback in dao`() = runTest {
        repository.updateEpisodeProgress("e1", 500L)

        coVerify { episodeDao.updatePlayback("e1", false, 500L) }
    }

    @Test
    fun `markEpisodeAsPlayed updates playback in dao`() = runTest {
        repository.markEpisodeAsPlayed("e1")

        coVerify { episodeDao.updatePlayback("e1", true, 0L) }
    }

    @Test
    fun `searchEpisodes returns results from dao`() = runTest {
        every { episodeDao.search("query") } returns flowOf(listOf(episodeEntity))

        val results = repository.searchEpisodes("query").first()

        assertEquals(1, results.size)
        assertEquals("e1", results[0].id)
    }
}
