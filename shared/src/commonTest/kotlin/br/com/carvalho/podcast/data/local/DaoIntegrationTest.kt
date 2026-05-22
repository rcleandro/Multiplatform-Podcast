package br.com.carvalho.podcast.data.local

import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var podcastDao: br.com.carvalho.podcast.data.local.dao.PodcastDao
    private lateinit var episodeDao: br.com.carvalho.podcast.data.local.dao.EpisodeDao

    @BeforeTest
    fun setup() {
        database = createInMemoryDatabase()
        podcastDao = database.podcastDao()
        episodeDao = database.episodeDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve podcast`() = runTest {
        val podcast = createPodcastEntity("p1")
        podcastDao.insert(podcast)

        val retrieved = podcastDao.getById("p1")
        assertEquals(podcast.title, retrieved?.title)
    }

    @Test
    fun `insert podcast and episodes then retrieve episodes by podcast id`() = runTest {
        val podcast = createPodcastEntity("p1")
        podcastDao.insert(podcast)

        val episode = createEpisodeEntity("e1", "p1")
        episodeDao.insertAll(listOf(episode))

        val episodes = episodeDao.getByPodcast("p1").first()
        assertEquals(1, episodes.size)
        assertEquals("e1", episodes[0].id)
    }

    @Test
    fun `delete podcast also deletes its episodes due to cascade`() = runTest {
        val podcast = createPodcastEntity("p1")
        podcastDao.insert(podcast)

        val episode = createEpisodeEntity("e1", "p1")
        episodeDao.insertAll(listOf(episode))

        podcastDao.deleteById("p1")

        val episodes = episodeDao.getByPodcast("p1").first()
        assertTrue(episodes.isEmpty(), "Episodes should be deleted when parent podcast is deleted")
    }

    @Test
    fun `search episodes by title or description`() = runTest {
        val podcast = createPodcastEntity("p1")
        podcastDao.insert(podcast)

        val ep1 = createEpisodeEntity("e1", "p1", title = "Kotlin Programming")
        val ep2 = createEpisodeEntity("e2", "p1", description = "Learn Swift UI")
        episodeDao.insertAll(listOf(ep1, ep2))

        val results = episodeDao.search("Kotlin").first()
        assertEquals(1, results.size)
        assertEquals("e1", results[0].id)

        val results2 = episodeDao.search("Swift").first()
        assertEquals(1, results2.size)
        assertEquals("e2", results2[0].id)
    }

    private fun createPodcastEntity(id: String) = PodcastEntity(
        id = id,
        title = "Title $id",
        description = "Desc",
        imageUrl = "img",
        author = "Author",
        language = "en",
        categories = "[]",
        feedUrl = "feed",
        siteUrl = "site",
        lastUpdated = 0L,
        isSubscribed = true
    )

    private fun createEpisodeEntity(id: String, podcastId: String, title: String = "Ep", description: String = "Desc") = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        podcastTitle = "Podcast",
        title = title,
        description = description,
        audioUrl = "audio",
        imageUrl = "img",
        duration = 1000L,
        publishDate = 0L,
        isPlayed = false,
        playbackPosition = 0L,
        isDownloaded = false,
        fileSize = null
    )
}
