package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshInBackgroundUseCaseTest {
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()
    private val preferences = FakePreferencesRepository()
    private val refresh = RefreshInBackgroundUseCase(RefreshPodcastUseCase(feedSource, repository), repository, preferences)

    private val loud = podcast("loud")
    private val quiet = podcast("quiet").copy(notifyNewEpisodes = false)

    @Test
    fun onlyWhatArrivedNowFromPodcastsThatNotifyIsToldAbout() = runTest {
        repository.podcasts.value = listOf(loud, quiet)
        repository.episodes.value = listOf(episode("waiting", loud.id))
        feedSource.resultsByUrl[loud.feedUrl] = Result.success(FetchedFeed(loud, listOf(episode("arrived", loud.id))))
        feedSource.resultsByUrl[quiet.feedUrl] = Result.success(FetchedFeed(quiet, listOf(episode("muted", quiet.id))))

        val result = refresh()

        // "waiting" was already in New; "muted" belongs to a podcast that keeps quiet.
        assertEquals(listOf("arrived"), result.toNotify.map { it.id })
    }

    @Test
    fun underTheEcaDigitalNothingExplicitIsToldAbout() = runTest {
        val adult = podcast("adult").copy(explicit = true)
        repository.podcasts.value = listOf(loud, adult)
        feedSource.resultsByUrl[loud.feedUrl] = Result.success(
            FetchedFeed(loud, listOf(episode("clean", loud.id), episode("swearing", loud.id).copy(explicit = true)))
        )
        feedSource.resultsByUrl[adult.feedUrl] = Result.success(FetchedFeed(adult, listOf(episode("late", adult.id))))
        val hidden = RefreshInBackgroundUseCase(
            RefreshPodcastUseCase(feedSource, repository),
            repository,
            preferences,
            ExplicitContent(MutableStateFlow(ExplicitRule(hidesExplicit = true))),
        )

        assertEquals(listOf("clean"), hidden().toNotify.map { it.id })
    }

    @Test
    fun settingsCanSilenceEveryPodcast() = runTest {
        preferences.setNotifyNewEpisodes(false)
        repository.podcasts.value = listOf(loud)
        feedSource.resultsByUrl[loud.feedUrl] = Result.success(FetchedFeed(loud, listOf(episode("arrived", loud.id))))

        assertTrue(refresh().toNotify.isEmpty())
    }

    private fun podcast(id: String) = Podcast(
        id = id, title = id, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "https://$id.example.com/feed", siteUrl = null, lastUpdated = 0,
        isSubscribed = true,
    )

    private fun episode(id: String, podcastId: String) = Episode(
        id = id, podcastId = podcastId, title = id, description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null, isNew = true,
    )
}
