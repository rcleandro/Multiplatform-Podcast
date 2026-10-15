package br.com.carvalho.podcast.core.refresh

import android.app.NotificationManager
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.RefreshInBackgroundUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RefreshWorkerTest {
    private val context = RuntimeEnvironment.getApplication()
    private val notifications = shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()

    private val alpha = podcast("alpha")
    private val beta = podcast("beta")

    @Before
    fun setUp() {
        val refresh = RefreshInBackgroundUseCase(
            RefreshPodcastUseCase(feedSource, repository),
            repository,
            FakePreferencesRepository(),
        )
        startKoin { modules(module { single { refresh } }) }
        repository.podcasts.value = listOf(alpha, beta)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun oneNewEpisodeIsNamedWithItsPodcast() = runTest {
        arrive(alpha, "Pilot")
        arrive(beta)

        assertEquals(Result.success(), work())

        val notification = notifications.allNotifications.single()
        assertEquals("alpha", shadowOf(notification).contentTitle)
        assertEquals("Pilot", shadowOf(notification).contentText)
    }

    @Test
    fun severalNewEpisodesAreCountedWithTheirPodcasts() = runTest {
        arrive(alpha, "One", "Two")
        arrive(beta, "Three")

        assertEquals(Result.success(), work())

        val notification = notifications.allNotifications.single()
        assertEquals("3 new episodes", shadowOf(notification).contentTitle)
        assertEquals("alpha, beta", shadowOf(notification).contentText)
    }

    @Test
    fun nothingNewTellsNothing() = runTest {
        arrive(alpha)
        arrive(beta)

        assertEquals(Result.success(), work())
        assertTrue(notifications.allNotifications.isEmpty())
    }

    @Test
    fun blockedNotificationsTellNothing() = runTest {
        notifications.setNotificationsEnabled(false)
        arrive(alpha, "Pilot")
        arrive(beta)

        assertEquals(Result.success(), work())
        assertTrue(notifications.allNotifications.isEmpty())
    }

    @Test
    fun everyFeedFailingTriesAgainSoon() = runTest {
        assertEquals(Result.retry(), work())
        assertTrue(notifications.allNotifications.isEmpty())
    }

    private suspend fun work() = TestListenableWorkerBuilder<RefreshWorker>(context).build().doWork()

    private fun arrive(podcast: Podcast, vararg titles: String) {
        val episodes = titles.map { episode(it, podcast) }
        feedSource.resultsByUrl[podcast.feedUrl] = kotlin.Result.success(FetchedFeed(podcast, episodes))
    }

    private fun podcast(id: String) = Podcast(
        id = id, title = id, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "https://$id.example.com/feed", siteUrl = null, lastUpdated = 0,
        isSubscribed = true,
    )

    private fun episode(title: String, podcast: Podcast) = Episode(
        id = title, podcastId = podcast.id, podcastTitle = podcast.title, title = title, description = null,
        audioUrl = "a", imageUrl = null, duration = 60, publishDate = 0, isPlayed = false, playbackPosition = 0,
        isDownloaded = false, fileSize = null, isNew = true,
    )
}
