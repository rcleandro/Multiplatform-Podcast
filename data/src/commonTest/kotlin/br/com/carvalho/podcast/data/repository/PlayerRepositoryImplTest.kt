package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.AppDatabase
import br.com.carvalho.podcast.data.local.createInMemoryDatabase
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.isDatabaseSupported
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.domain.model.ListeningStats
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
        repository = PlayerRepositoryImpl(database.playbackStateDao(), database.podcastDao())
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

        database.episodeDao().updatePlayback("e2", played = true, position = 0, updatedAt = 1)

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

    @Test
    fun `bookmarks come in episode order lose their surrounding spaces and go with the episode`() = runTest {
        if (!isDatabaseSupported) return@runTest
        database.podcastDao().insert(podcast)
        database.episodeDao().insertAll(episodes)

        repository.addBookmark("e1", positionMs = 90_000, note = "  the quote ")
        repository.addBookmark("e1", positionMs = 30_000, note = "")
        repository.addBookmark("e2", positionMs = 10_000, note = "other episode")
        val marked = repository.bookmarks("e1").first()
        assertEquals(listOf(30_000L to "", 90_000L to "the quote"), marked.map { it.positionMs to it.note })

        assertEquals(listOf("e1", "e1", "e2"), repository.allBookmarks().map { it.episodeId })

        repository.deleteBookmark(marked.first().id)
        assertEquals(listOf("the quote"), repository.bookmarks("e1").first().map { it.note })

        database.podcastDao().deleteById("p1")
        assertEquals(emptyList(), repository.bookmarks("e1").first())
    }

    @Test
    fun `time heard adds up by day into weeks starting on monday and by podcast`() = runTest {
        if (!isDatabaseSupported) return@runTest
        // Wednesday 2026-10-14, 12:00 UTC.
        val wednesday = 1_791_979_200_000L
        val stats = PlayerRepositoryImpl(
            database.playbackStateDao(),
            database.podcastDao(),
            timeZone = { TimeZone.UTC },
            now = { wednesday },
        )
        database.podcastDao().insert(podcast)
        val monday = wednesday - 2 * DAY_MS
        val lastSunday = monday - 13 * HOUR_MS // Sunday 23:00

        stats.addListening("p1", at = wednesday, listenedMs = 60_000, savedMs = 15_000)
        stats.addListening("p1", at = monday, listenedMs = 30_000, savedMs = 5_000)
        stats.addListening("p1", at = lastSunday, listenedMs = 10_000, savedMs = 0)
        stats.addListening("gone", at = wednesday, listenedMs = 1_000, savedMs = 0)

        val result = stats.listeningStats().first()
        assertEquals(ListeningStats.WEEKS, result.weeks.size)
        assertEquals(91_000L, result.thisWeekListenedMs)
        assertEquals(10_000L, result.weeks[ListeningStats.WEEKS - 2].listenedMs)
        assertEquals(LocalDate(2026, 10, 12).toEpochDays(), result.weeks.last().startEpochDay)
        // A podcast no longer in the library still counts in the totals.
        assertEquals(listOf("p1" to 100_000L), result.podcasts.map { it.podcast.id to it.listenedMs })
        assertEquals(101_000L to 20_000L, result.totalListenedMs to result.totalSavedMs)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 24 * HOUR_MS
    }
}
