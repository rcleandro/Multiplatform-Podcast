package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.local.createInMemoryDatabase
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.domain.model.Podcast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PodcastRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PodcastRepositoryImpl

    private val podcastId = "p1"
    private val podcastEntity = PodcastEntity(
        id = podcastId,
        title = "Test Podcast",
        description = "Desc",
        imageUrl = "img",
        author = "Author",
        language = "pt",
        categories = "[\"Cat1\", \"Cat2\"]",
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

    @BeforeTest
    fun setup() {
        database = createInMemoryDatabase()
        repository = PodcastRepositoryImpl(database.podcastDao(), database.episodeDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getPodcasts maps and returns podcasts from dao`() = runTest {
        database.podcastDao().insert(podcastEntity)

        val podcasts = repository.getPodcasts().first()

        assertEquals(1, podcasts.size)
        assertEquals(podcastId, podcasts[0].id)
        assertEquals("Test Podcast", podcasts[0].title)
    }

    @Test
    fun `getPodcastById returns mapped podcast if exists`() = runTest {
        database.podcastDao().insert(podcastEntity)

        val podcast = repository.getPodcastById(podcastId)

        assertEquals(podcastId, podcast?.id)
        assertEquals("Test Podcast", podcast?.title)
    }

    @Test
    fun `getPodcastById returns null if not exists`() = runTest {
        val podcast = repository.getPodcastById("non-existent")
        assertNull(podcast)
    }

    @Test
    fun `getEpisodes returns mapped episodes for podcast`() = runTest {
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

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

        val retrieved = database.podcastDao().getById(podcastId)
        assertEquals("Test Podcast", retrieved?.title)
    }

    @Test
    fun `deletePodcast deletes podcast and its episodes`() = runTest {
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        repository.deletePodcast(podcastId)

        assertNull(database.podcastDao().getById(podcastId))
        assertTrue(database.episodeDao().getByPodcast(podcastId).first().isEmpty())
    }

    @Test
    fun `updateEpisodeProgress updates playback in dao`() = runTest {
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        repository.updateEpisodeProgress("e1", 500L)

        val retrieved = database.episodeDao().getByPodcast(podcastId).first()[0]
        assertEquals(500L, retrieved.playbackPosition)
    }

    @Test
    fun `markEpisodeAsPlayed updates playback in dao`() = runTest {
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        repository.markEpisodeAsPlayed("e1")

        val retrieved = database.episodeDao().getByPodcast(podcastId).first()[0]
        assertTrue(retrieved.isPlayed)
    }

    @Test
    fun `searchEpisodes returns results from dao`() = runTest {
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        val results = repository.searchEpisodes("Ep").first()

        assertEquals(1, results.size)
        assertEquals("e1", results[0].id)
    }

    private fun assertTrue(condition: Boolean) {
        assertEquals(true, condition)
    }
}
