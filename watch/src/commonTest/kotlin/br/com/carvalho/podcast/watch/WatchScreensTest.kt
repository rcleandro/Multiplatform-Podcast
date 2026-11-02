package br.com.carvalho.podcast.watch

import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastDirectory
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchScreensTest {
    private val repository = FakePodcastRepository()
    private val player = FakeAudioPlayer()
    private val downloader = FakeEpisodeDownloader()

    @Test
    fun anEpisodeSaysWhereItStandsAsOnWearOs() {
        val started = episode("e", published = 0).copy(duration = 3_600, playbackPosition = 1_800_000)
        assertEquals(EpisodeStatus.Playing, episodeStatus(started, isPlaying = true, download = null))
        assertEquals(EpisodeStatus.Downloading(null), episodeStatus(started, false, DownloadStatus.Queued()))
        assertEquals(EpisodeStatus.Played, episodeStatus(started.copy(isPlayed = true), false, null))
        assertTrue(episodeStatus(started, false, null) is EpisodeStatus.Remaining)
        assertTrue(episodeStatus(started.copy(playbackPosition = 0), false, null) is EpisodeStatus.Length)
    }

    @Test
    fun aPodcastListsItsEpisodesNewestFirstUnlessItSaysOtherwise() = runTest {
        repository.podcasts.value = listOf(podcast("show"))
        repository.episodes.value = listOf(episode("old", published = 1), episode("new", published = 2))
        val page = WatchPodcast("show", repository, player, downloader)

        assertEquals(listOf("new", "old"), page.state.first().episodes.map { it.episode.id })
        repository.podcasts.value = listOf(podcast("show").copy(oldestFirst = true))
        assertEquals(listOf("old", "new"), page.state.first().episodes.map { it.episode.id })
    }

    @Test
    fun subscribingFromPopularMarksThePodcastSubscribed() = runTest {
        val feedUrl = "https://ex.com/feed"
        val directory = FakePodcastDirectory().apply {
            topResult = Result.success(listOf(DirectoryPodcast("Show", null, null, feedUrl, null, null)))
        }
        val feeds = FakeFeedSource().apply { result = Result.success(FetchedFeed(podcast(feedUrl), emptyList())) }
        val popular = WatchPopular(
            SearchPodcastsUseCase(directory, repository, FakePreferencesRepository()),
            AddPodcastFromUrlUseCase(feeds, repository),
        )

        popular.load()
        popular.subscribe(popular.state.value.results.single())

        assertTrue(popular.state.value.results.single().isSubscribed)
        assertTrue(popular.state.value.subscribing.isEmpty())
    }

    @Test
    fun theDownloadButtonCancelsDeletesOrDownloads() {
        val episode = episode("e", published = 0)
        assertEquals(DownloadAction.CANCEL, downloadAction(episode, DownloadStatus.Downloading(0.5f, 1, 2)))
        assertEquals(DownloadAction.DELETE, downloadAction(episode.copy(isDownloaded = true), null))
        assertEquals(DownloadAction.DOWNLOAD, downloadAction(episode, null))
    }

    @Test
    fun theDownloadsListShowsWhatIsOnTheWatch() = runTest {
        repository.episodes.value = listOf(episode("here", 1).copy(isDownloaded = true), episode("there", 2))
        val library = WatchLibrary(repository, player, downloader)

        assertEquals(listOf("here"), library.downloads.first().map { it.episode.id })
    }

    @Test
    fun anEpisodesActionsDownloadAndMarkItHeard() = runTest {
        repository.episodes.value = listOf(episode("e", 1))
        val actions = WatchEpisodeActions("e", repository, player, downloader)

        assertEquals(DownloadAction.DOWNLOAD, actions.load()?.download)
        actions.download(DownloadAction.DOWNLOAD)
        actions.markPlayed(true)

        assertEquals("e", downloader.downloadCalledWith?.id)
        assertTrue(repository.episodes.value.single().isPlayed)
    }

    private fun podcast(id: String) = Podcast(
        id = id, title = id, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = id, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private fun episode(id: String, published: Long) = Episode(
        id = id, podcastId = "show", title = id, description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = published, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )
}
