package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePlayerRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupUseCasesTest {

    /** One install of the app: its library, player, preferences and the feeds it can reach. */
    private class Device {
        val repository = FakePodcastRepository()
        val playerRepository = FakePlayerRepository()
        val preferences = FakePreferencesRepository()
        val player = FakeAudioPlayer()
        val feeds = FakeFeedSource()
        val export = ExportBackupUseCase(repository, playerRepository, preferences, player)
        val restore = RestoreBackupUseCase(
            repository, playerRepository, preferences, player, AddPodcastFromUrlUseCase(feeds, repository),
        )
    }

    @Test
    fun aBackupBringsTheWholeLibraryToAnotherDevice() = runTest {
        val old = Device()
        old.repository.podcasts.value = listOf(
            podcast(FEED_B, position = 1),
            podcast(FEED_A, position = 0).copy(
                playback = PodcastPlayback(speed = FAST, skipIntroSeconds = INTRO),
                oldestFirst = true,
                notifyNewEpisodes = false,
                autoDownloadCount = 2,
            ),
        )
        old.repository.episodes.value = listOf(
            episode("a1", FEED_A).copy(isPlayed = true, lastPlayedAt = PLAYED_AT),
            episode("a2", FEED_A).copy(playbackPosition = POSITION, isSaved = true),
            episode("a3", FEED_A),
            episode("b1", FEED_B),
        )
        old.playerRepository.addBookmark("a2", BOOKMARK, "the quote")
        old.player.setQueue(listOf(episode("a3", FEED_A), episode("b1", FEED_B)))
        old.preferences.setThemeMode(ThemeMode.DARK)
        old.preferences.setSkipIntervals(SkipIntervals(backwardSeconds = 5, forwardSeconds = 30))

        val backup = old.export()
        assertEquals(2, backup.podcasts)

        val new = Device()
        new.feeds.resultsByUrl[FEED_A] = Result.success(fetched(FEED_A, "a1", "a2", "a3"))
        new.feeds.resultsByUrl[FEED_B] = Result.success(fetched(FEED_B, "b1"))
        val progress = mutableListOf<Pair<Int, Int>>()

        val result = new.restore(backup.text) { done, total -> progress += done to total }

        assertEquals(OpmlImport(added = 2, known = 0, failed = 0), result)
        assertEquals(2 to 2, progress.last())
        val podcasts = new.repository.podcasts.value.sortedBy { it.position }
        assertEquals(listOf(FEED_A, FEED_B), podcasts.map { it.feedUrl })
        val a = podcasts.first()
        assertEquals(PodcastPlayback(speed = FAST, skipIntroSeconds = INTRO), a.playback)
        assertEquals(listOf(true, false, 2), listOf(a.oldestFirst, a.notifyNewEpisodes, a.autoDownloadCount))
        val episodes = new.repository.episodes.value.associateBy { it.id }
        assertEquals(true to PLAYED_AT, episodes.getValue("a1").let { it.isPlayed to it.lastPlayedAt })
        assertEquals(POSITION to true, episodes.getValue("a2").let { it.playbackPosition to it.isSaved })
        assertEquals(listOf(BOOKMARK to "the quote"), new.playerRepository.savedBookmarks.value.map { it.positionMs to it.note })
        assertEquals(listOf("a3", "b1"), new.player.queueSet?.map { it.id })
        assertEquals(ThemeMode.DARK, new.preferences.themeMode.value)
        assertEquals(SkipIntervals(5, 30), new.preferences.skipIntervals.value)
    }

    @Test
    fun restoringNeverLosesWhatThisDeviceHeardAndAddsNothingTwice() = runTest {
        val old = Device()
        old.repository.podcasts.value = listOf(podcast(FEED_A, position = 0))
        old.repository.episodes.value = listOf(
            episode("a1", FEED_A).copy(isPlayed = true),
            episode("a2", FEED_A).copy(playbackPosition = POSITION),
            episode("a3", FEED_A).copy(playbackPosition = POSITION),
        )
        old.playerRepository.addBookmark("a2", BOOKMARK, "")
        old.player.setQueue(listOf(episode("a1", FEED_A)))
        val text = old.export().text

        val here = Device()
        here.repository.podcasts.value = listOf(podcast(FEED_A, position = 0))
        here.repository.episodes.value = listOf(
            episode("a1", FEED_A),
            // Heard further here than in the backup.
            episode("a2", FEED_A).copy(playbackPosition = POSITION * 2),
            // Another id for the same audio, as when the feed was added under another address.
            episode("other-id", FEED_A).copy(audioUrl = audio("a3")),
        )
        here.player.setQueue(listOf(episode("a2", FEED_A)))

        val result = here.restore(text)
        here.restore(text)

        assertEquals(OpmlImport(added = 0, known = 1, failed = 0), result)
        val episodes = here.repository.episodes.value.associateBy { it.id }
        assertEquals(true, episodes.getValue("a1").isPlayed)
        assertEquals(POSITION * 2, episodes.getValue("a2").playbackPosition)
        assertEquals(POSITION, episodes.getValue("other-id").playbackPosition)
        assertEquals(1, here.playerRepository.bookmarks("a2").first().size)
        assertEquals(listOf("a2"), here.player.playerState.value.queue.map { it.id })
    }

    @Test
    fun aFileThatIsNotABackupChangesNothing() = runTest {
        val device = Device()
        listOf("<opml/>", "{\"format\":\"other\"}", "{\"format\":\"br.com.carvalho.podcast.backup\",\"version\":99}")
            .forEach { assertNull(device.restore(it)) }
        assertEquals(emptyList(), device.repository.podcasts.value)
    }

    private fun podcast(feedUrl: String, position: Int) = Podcast(
        id = feedUrl, title = feedUrl, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
        position = position,
    )

    private fun episode(id: String, feedUrl: String) = Episode(
        id = id, podcastId = feedUrl, title = id, description = null, audioUrl = audio(id), imageUrl = null,
        duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    private fun audio(id: String) = "https://cdn.example.com/$id.mp3"

    private fun fetched(feedUrl: String, vararg ids: String) =
        FetchedFeed(podcast(feedUrl, position = 0), ids.map { episode(it, feedUrl) })

    private companion object {
        const val FEED_A = "https://a.example.com/feed"
        const val FEED_B = "https://b.example.com/feed"
        const val FAST = 1.5f
        const val INTRO = 20
        const val POSITION = 60_000L
        const val BOOKMARK = 30_000L
        const val PLAYED_AT = 1_700_000_000_000L
    }
}
