package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoDownloadUseCaseTest {
    private val repository = FakePodcastRepository()
    private val downloader = FakeEpisodeDownloader()
    private val autoDownload = AutoDownloadUseCase(repository, downloader)

    private val podcast = Podcast(
        id = "p", title = "P", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "https://p.example.com/feed", siteUrl = null, lastUpdated = 0,
        isSubscribed = true, autoDownloadCount = 2,
    )

    @Test
    fun theNewestUnheardOnesMissingAreDownloaded() = runTest {
        repository.podcasts.value = listOf(podcast)
        repository.episodes.value = listOf(
            episode("heard", 5).copy(isPlayed = true),
            episode("downloaded", 4).copy(isDownloaded = true),
            episode("newest", 3),
            episode("older", 2),
        )

        autoDownload("p")

        // The two newest unheard are "downloaded" (already there) and "newest".
        assertEquals(listOf("newest"), downloader.downloaded.map { it.id })
    }

    @Test
    fun nothingWhenThePodcastDoesNotAskOrTheEpisodeIsAlreadyOnItsWay() = runTest {
        repository.podcasts.value = listOf(podcast.copy(autoDownloadCount = 0))
        repository.episodes.value = listOf(episode("newest", 3))
        autoDownload("p")

        repository.podcasts.value = listOf(podcast)
        downloader.activeDownloads.value = mapOf("newest" to DownloadStatus.Queued())
        autoDownload("p")

        assertEquals(emptyList(), downloader.downloaded)
    }

    @Test
    fun aRefreshKeepsThePodcastsNewestDownloaded() = runTest {
        repository.podcasts.value = listOf(podcast)
        val feedSource = FakeFeedSource().apply {
            result = Result.success(FetchedFeed(podcast, listOf(episode("arrived", 9))))
        }

        RefreshPodcastUseCase(feedSource, repository, autoDownload)("p")

        assertEquals(listOf("arrived"), downloader.downloaded.map { it.id })
    }

    private fun episode(id: String, publishDate: Long) = Episode(
        id = id, podcastId = "p", title = id, description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = publishDate, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )
}
