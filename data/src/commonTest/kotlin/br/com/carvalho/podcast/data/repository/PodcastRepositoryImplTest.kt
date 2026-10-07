package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.local.createInMemoryDatabase
import br.com.carvalho.podcast.data.local.isDatabaseSupported
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.domain.model.FeedVersion
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import androidx.paging.testing.asSnapshot
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import androidx.paging.PagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
        if (!isDatabaseSupported) return
        database = createInMemoryDatabase()
        repository = PodcastRepositoryImpl(database.podcastDao(), database.episodeDao())
    }

    @AfterTest
    fun tearDown() {
        if (!isDatabaseSupported) return
        database.close()
    }

    @Test
    fun `getPodcasts maps and returns podcasts from dao`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)

        val podcasts = repository.getPodcasts().first()

        assertEquals(1, podcasts.size)
        assertEquals(podcastId, podcasts[0].id)
        assertEquals("Test Podcast", podcasts[0].title)
    }

    @Test
    fun `getPodcastById returns mapped podcast if exists`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)

        val podcast = repository.getPodcastById(podcastId)

        assertEquals(podcastId, podcast?.id)
        assertEquals("Test Podcast", podcast?.title)
    }

    @Test
    fun `getPodcastById returns null if not exists`() = runTest {
        if (!isDatabaseSupported) return@runTest
        val podcast = repository.getPodcastById("non-existent")
        assertNull(podcast)
    }

    @Test
    fun `getEpisodes returns mapped episodes for podcast`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        val episodes = repository.getEpisodes(podcastId).first()

        assertEquals(1, episodes.size)
        assertEquals("e1", episodes[0].id)
        assertEquals(podcastId, episodes[0].podcastId)
    }

    @Test
    fun `saveFeed saves the podcast`() = runTest {
        if (!isDatabaseSupported) return@runTest
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

        repository.saveFeed(podcast, emptyList())

        val retrieved = database.podcastDao().getById(podcastId)
        assertEquals("Test Podcast", retrieved?.title)
    }

    @Test
    fun `deletePodcast deletes podcast and its episodes`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        repository.deletePodcast(podcastId)

        assertNull(database.podcastDao().getById(podcastId))
        assertTrue(database.episodeDao().getByPodcast(podcastId).first().isEmpty())
    }

    @Test
    fun `updateEpisodeProgress updates playback in dao`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        repository.updateEpisodeProgress("e1", 500L)

        val retrieved = database.episodeDao().getByPodcast(podcastId).first()[0]
        assertEquals(500L, retrieved.playbackPosition)
    }

    @Test
    fun `markEpisodeAsPlayed updates playback in dao`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        repository.markEpisodeAsPlayed("e1")

        val retrieved = database.episodeDao().getByPodcast(podcastId).first()[0]
        assertTrue(retrieved.isPlayed)
    }

    @Test
    fun `searchEpisodes returns results from dao`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))

        val results = repository.searchEpisodes("Ep").first()

        assertEquals(1, results.size)
        assertEquals("e1", results[0].id)
    }

    @Test
    fun `saving a feed again updates the episode text but keeps what the user did`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(
            listOf(episodeEntity.copy(isPlayed = true, playbackPosition = 500L, isDownloaded = true))
        )
        val fromFeed = episodeEntity.toDomain().copy(title = "Ep 1 (fixed)", audioUrl = "audio-v2")

        repository.saveFeed(podcastEntity.toDomain(), listOf(fromFeed))

        val saved = database.episodeDao().getById("e1")!!
        assertEquals("Ep 1 (fixed)", saved.title)
        assertEquals("audio-v2", saved.audioUrl)
        assertTrue(saved.isPlayed)
        assertEquals(500L, saved.playbackPosition)
        assertTrue(saved.isDownloaded)
    }

    @Test
    fun `saving a feed keeps the version the server sent`() = runTest {
        if (!isDatabaseSupported) return@runTest
        val version = FeedVersion(etag = "\"v2\"", lastModified = null)

        repository.saveFeed(podcastEntity.toDomain().copy(feedVersion = version), emptyList())

        assertEquals(version, repository.getPodcastById(podcastId)?.feedVersion)
    }

    @Test
    fun `saving a feed removes episodes saved earlier without audio`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.podcastDao().insert(podcastEntity.copy(id = "p2"))
        database.episodeDao().insertAll(
            listOf(
                episodeEntity.copy(id = "post", audioUrl = ""),
                episodeEntity.copy(id = "other-post", podcastId = "p2", audioUrl = ""),
            )
        )

        repository.saveFeed(podcastEntity.toDomain(), listOf(episodeEntity.toDomain()))

        assertNull(database.episodeDao().getById("post"))
        assertEquals("other-post", database.episodeDao().getById("other-post")?.id) // another podcast's turn comes
    }

    @Test
    fun `a feed whose episodes fail to save leaves no podcast behind`() = runTest {
        if (!isDatabaseSupported) return@runTest
        val podcast = podcastEntity.toDomain()
        // The episode points to a podcast that does not exist, so its foreign key fails.
        val orphan = episodeEntity.copy(podcastId = "missing").toDomain()
        val feedSource = FakeFeedSource().apply { result = Result.success(FetchedFeed(podcast, listOf(orphan))) }

        val result = AddPodcastFromUrlUseCase(feedSource, repository)(podcastId)

        assertTrue(result.isFailure)
        assertNull(database.podcastDao().getById(podcastId))
    }

    @Test
    fun `the downloaded filter finds an episode beyond the first page`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        val notDownloaded = (1..PAGE_AND_MORE).map { episodeEntity.copy(id = "e$it", publishDate = 1_000L + it) }
        val downloadedAndOldest = episodeEntity.copy(id = "downloaded", publishDate = 0L, isDownloaded = true)
        database.episodeDao().insertAll(notDownloaded + downloadedAndOldest)

        val shown = repository.getEpisodesPaged(podcastId, EpisodeFilter.DOWNLOADED).asSnapshot()

        assertEquals(listOf("downloaded"), shown.map { it.id })
    }

    @Test
    fun `a shown page reloads when an episode changes`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcastEntity)
        database.episodeDao().insertAll(listOf(episodeEntity))
        val source = database.episodeDao().pagingSourceByPodcast(podcastId, onlyUnplayed = true, onlyDownloaded = false)
        source.load(PagingSource.LoadParams.Refresh(key = null, loadSize = PAGE, placeholdersEnabled = false))

        repository.markEpisodeAsPlayed("e1")

        // Room's invalidation tracker runs on a real dispatcher, so wait in real time.
        withContext(Dispatchers.Default) {
            withTimeout(INVALIDATION_TIMEOUT_MS) { while (!source.invalid) delay(POLL_MS) }
        }
    }

    private fun assertTrue(condition: Boolean) {
        assertEquals(true, condition)
    }

    private companion object {
        /** More than the initial load (three pages of 20), so filtering loaded pages in memory would miss it. */
        const val PAGE_AND_MORE = 70
        const val PAGE = 20
        const val INVALIDATION_TIMEOUT_MS = 5_000L
        const val POLL_MS = 10L
    }
}
