package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.local.createInMemoryDatabase
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.isDatabaseSupported
import br.com.carvalho.podcast.data.mapper.toDomain
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: PlayerRepositoryImpl

    private val podcast = PodcastEntity(
        id = "p1", title = "Podcast", description = "", imageUrl = null, author = null, language = null,
        categories = "[]", feedUrl = "p1", siteUrl = null, lastUpdated = 0, isSubscribed = true
    )
    private val episodes = listOf("e1", "e2").mapIndexed { index, id ->
        EpisodeEntity(
            id = id, podcastId = "p1", podcastTitle = "Podcast", title = id, description = null, audioUrl = "",
            imageUrl = null, duration = 0, publishDate = index.toLong(), isPlayed = false, playbackPosition = 0,
            isDownloaded = false, fileSize = null
        )
    }

    @BeforeTest
    fun setup() {
        if (!isDatabaseSupported) return
        database = createInMemoryDatabase()
        repository = PlayerRepositoryImpl(database.playbackStateDao())
    }

    @AfterTest
    fun tearDown() {
        if (!isDatabaseSupported) return
        database.close()
    }

    @Test
    fun `the saved queue shows the episodes as they are now`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcast)
        database.episodeDao().insertAll(episodes)
        repository.savePlaybackState("e1", position = 10, speed = 1f, queue = episodes.map { it.toDomain() })

        database.episodeDao().updatePlayback("e2", played = true, position = 0)

        val queue = repository.getSavedPlaybackState()!!.queue
        assertEquals(listOf("e1", "e2"), queue.map { it.id })
        assertEquals(listOf(false, true), queue.map { it.isPlayed })
    }

    @Test
    fun `an episode removed from the library leaves the saved queue`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcast)
        database.episodeDao().insertAll(episodes)
        repository.savePlaybackState("e1", position = 10, speed = 1f, queue = episodes.map { it.toDomain() })

        database.podcastDao().deleteById("p1")

        assertEquals(emptyList(), repository.getSavedPlaybackState()!!.queue)
    }
}
