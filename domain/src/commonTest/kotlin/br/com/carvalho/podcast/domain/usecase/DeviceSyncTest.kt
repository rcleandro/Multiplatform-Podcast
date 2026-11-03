package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceSyncTest {
    private class Device {
        val repository = FakePodcastRepository()
        val feeds = FakeFeedSource()
        val sync = DeviceSync(
            LibrarySyncUseCase(
                repository,
                FakeAudioPlayer(),
                AddPodcastFromUrlUseCase(feeds, repository),
                DeletePodcastUseCase(repository, FakeEpisodeDownloader()),
            ),
        )
    }

    @Test
    fun whatThePhoneSendsSubscribesTheWatch() = runTest {
        val phone = Device()
        phone.repository.podcasts.value = listOf(podcast(FEED))
        val watch = Device()
        watch.feeds.resultsByUrl[FEED] = Result.success(FetchedFeed(podcast(FEED), emptyList()))

        watch.sync.receive(phone.sync.outgoing().first())

        assertEquals(listOf(FEED), watch.repository.podcasts.value.map { it.feedUrl })
    }

    @Test
    fun aBrokenSnapshotChangesNothing() = runTest {
        val watch = Device()

        assertNull(watch.sync.receive("""{"podcasts": [{"feedUrl": "https:"""))
        assertEquals(emptyList(), watch.repository.podcasts.value)
    }

    private fun podcast(feedUrl: String) = Podcast(
        id = feedUrl, title = feedUrl, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
        subscribedAt = 1,
    )

    private companion object {
        const val FEED = "https://a.example.com/feed"
    }
}
