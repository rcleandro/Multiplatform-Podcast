package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeRef
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.SyncEpisode
import br.com.carvalho.podcast.domain.model.SyncPodcast
import br.com.carvalho.podcast.domain.model.SyncRemoval
import br.com.carvalho.podcast.domain.model.SyncSnapshot
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibrarySyncUseCaseTest {

    @Test
    fun podcastsJoinUnlessTakenOutAfterTheOtherSideAddedThem() {
        val local = SyncSnapshot(
            podcasts = listOf(SyncPodcast(FEED_A, subscribedAt = 10), SyncPodcast(FEED_B, subscribedAt = 10)),
            removed = listOf(SyncRemoval(FEED_C, removedAt = 50), SyncRemoval(FEED_D, removedAt = 5)),
        )
        val remote = SyncSnapshot(
            // The same feed under another scheme and host case is the same podcast.
            podcasts = listOf(
                SyncPodcast("http://A.example.com/feed", 1),
                SyncPodcast(FEED_C, 20),
                SyncPodcast(FEED_D, 20),
            ),
            removed = listOf(SyncRemoval(FEED_B, removedAt = 30)),
        )

        val plan = planSync(local, remote)

        assertEquals(listOf(FEED_D), plan.subscribe)
        assertEquals(listOf(FEED_B), plan.unsubscribe)
    }

    @Test
    fun eachEpisodeTakesTheNewerChangeAndTheQueueTheOneArrangedLast() {
        val local = SyncSnapshot(
            episodes = listOf(state("a1", played = false, at = 10), state("a2", played = false, at = 30)),
            queue = listOf(ref("a1")),
            queueUpdatedAt = 10,
        )
        val remote = SyncSnapshot(
            episodes = listOf(
                state("a1", played = true, at = 20),
                state("a2", played = true, at = 20),
                state("a3", played = true, at = 1),
            ),
            queue = listOf(ref("a2"), ref("a3")),
            queueUpdatedAt = 20,
        )

        val plan = planSync(local, remote)

        assertEquals(listOf("a1", "a3"), plan.episodes.map { it.episode.id })
        assertEquals(listOf("a2", "a3"), plan.queue?.map { it.id })
        assertNull(planSync(remote, local).queue)
    }

    @Test
    fun twoDevicesAgreeAfterOneExchangeEachWayAndThenHaveNothingToSend() = runTest {
        val phone = Device()
        phone.repository.podcasts.value = listOf(podcast(FEED_A, subscribedAt = 1))
        phone.repository.episodes.value = listOf(
            episode("a1").copy(isPlayed = true, updatedAt = 40),
            episode("a2").copy(playbackPosition = POSITION, updatedAt = 10),
            episode("a3"),
        )
        phone.player.clock = 50
        phone.player.setQueue(listOf(episode("a3")))
        val watch = Device()
        watch.feeds.resultsByUrl[FEED_A] = Result.success(FetchedFeed(podcast(FEED_A), ids("a1", "a2", "a3")))
        watch.feeds.resultsByUrl[FEED_B] = Result.success(FetchedFeed(podcast(FEED_B), emptyList()))
        watch.repository.podcasts.value = listOf(podcast(FEED_B, subscribedAt = 1))
        watch.repository.clock = 60
        watch.delete(FEED_B)

        watch.sync.apply(phone.sync.snapshot())
        // The watch hears further into a2 after the phone did.
        watch.repository.applyEpisodeState("a2", played = false, position = POSITION * 2, updatedAt = 70)
        phone.sync.apply(watch.sync.snapshot())

        assertEquals(listOf(FEED_A), watch.repository.podcasts.value.map { it.feedUrl })
        assertEquals(true, watch.episode("a1").isPlayed)
        assertEquals(listOf("a3"), watch.player.playerState.value.queue.map { it.id })
        assertEquals(POSITION * 2, phone.episode("a2").playbackPosition)
        assertTrue(phone.sync.apply(watch.sync.snapshot()).isEmpty)
        assertTrue(watch.sync.apply(phone.sync.snapshot()).isEmpty)
    }

    /** One install of the app: its library, its player and the feeds it can reach. */
    private class Device {
        val repository = FakePodcastRepository()
        val player = FakeAudioPlayer()
        val feeds = FakeFeedSource()
        val deletePodcast = DeletePodcastUseCase(repository, FakeEpisodeDownloader())
        val sync = LibrarySyncUseCase(repository, player, AddPodcastFromUrlUseCase(feeds, repository), deletePodcast)

        suspend fun delete(feedUrl: String) = deletePodcast(feedUrl)
        fun episode(id: String) = repository.episodes.value.first { it.id == id }
    }

    private fun state(id: String, played: Boolean, at: Long) = SyncEpisode(ref(id), played, 0, at)

    private fun ref(id: String) = EpisodeRef(id, FEED_A, audio(id))

    private fun ids(vararg ids: String) = ids.map { episode(it) }

    private fun podcast(feedUrl: String, subscribedAt: Long = 0) = Podcast(
        id = feedUrl, title = feedUrl, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
        subscribedAt = subscribedAt,
    )

    private fun episode(id: String) = Episode(
        id = id, podcastId = FEED_A, title = id, description = null, audioUrl = audio(id), imageUrl = null,
        duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    private fun audio(id: String) = "https://cdn.example.com/$id.mp3"

    private companion object {
        const val FEED_A = "https://a.example.com/feed"
        const val FEED_B = "https://b.example.com/feed"
        const val FEED_C = "https://c.example.com/feed"
        const val FEED_D = "https://d.example.com/feed"
        const val POSITION = 60_000L
    }
}
