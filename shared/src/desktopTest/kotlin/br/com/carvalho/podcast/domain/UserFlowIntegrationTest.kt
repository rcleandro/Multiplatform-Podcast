package br.com.carvalho.podcast.domain

import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.local.createInMemoryDatabase
import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.data.repository.PodcastRepositoryImpl
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserFlowIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PodcastRepositoryImpl
    private val rssDataSource = mockk<RssFeedDataSource>()

    private lateinit var addPodcastUseCase: AddPodcastFromUrlUseCase
    private lateinit var refreshPodcastUseCase: RefreshPodcastUseCase

    @BeforeTest
    fun setup() {
        database = createInMemoryDatabase()
        repository = PodcastRepositoryImpl(database.podcastDao(), database.episodeDao())
        addPodcastUseCase = AddPodcastFromUrlUseCase(rssDataSource, repository)
        refreshPodcastUseCase = RefreshPodcastUseCase(rssDataSource, repository)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `full flow - add podcast and then refresh it`() = runTest {
        val url = "https://test.com/rss"
        val rssFeed = RssFeed(
            title = "Flow Podcast",
            description = "Desc",
            imageUrl = "img",
            author = "Author",
            language = "en",
            categories = listOf("Tech"),
            link = "link",
            ttl = null,
            episodes = listOf(
                RssEpisode(
                    guid = "e1",
                    title = "Ep 1",
                    description = "Desc 1",
                    enclosureUrl = "url1",
                    enclosureType = "audio/mpeg",
                    duration = "10:00",
                    publishDate = "Fri, 15 May 2026 10:00:00 GMT",
                    imageUrl = null,
                    explicit = false,
                    season = null,
                    episode = null
                )
            )
        )

        coEvery { rssDataSource.fetchFeed(url) } returns Result.success(rssFeed)

        // 1. Add Podcast
        val addResult = addPodcastUseCase(url)
        assertEquals(true, addResult.isSuccess)

        // 2. Verify in library
        val podcasts = repository.getPodcasts().first()
        assertEquals(1, podcasts.size)
        assertEquals("Flow Podcast", podcasts[0].title)

        // 3. Verify episodes
        val episodes = repository.getEpisodes(url).first()
        assertEquals(1, episodes.size)
        assertEquals("Ep 1", episodes[0].title)

        // 4. Refresh (simulate new episode)
        val updatedRssFeed = rssFeed.copy(
            episodes = rssFeed.episodes + RssEpisode(
                guid = "e2",
                title = "Ep 2",
                description = "Desc 2",
                enclosureUrl = "url2",
                enclosureType = "audio/mpeg",
                duration = "15:00",
                publishDate = "Sat, 16 May 2026 10:00:00 GMT",
                imageUrl = null,
                explicit = false,
                season = null,
                episode = null
            )
        )
        coEvery { rssDataSource.fetchFeed(url) } returns Result.success(updatedRssFeed)

        refreshPodcastUseCase(url)

        // 5. Verify updated episodes
        val updatedEpisodes = repository.getEpisodes(url).first()
        assertEquals(2, updatedEpisodes.size)
        assertEquals("Ep 2", updatedEpisodes[0].title)
    }
}
