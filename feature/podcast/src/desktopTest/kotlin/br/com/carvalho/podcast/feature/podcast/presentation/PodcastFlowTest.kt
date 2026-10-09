package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.delete_download
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.episode_options
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/** The podcast screen wired to its real view model, over fakes: play an episode, download it and remove the file. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PodcastFlowTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val audioPlayer = FakeAudioPlayer()
    private val downloader = FakeEpisodeDownloader()
    private val repository = FakePodcastRepository().apply {
        podcasts.value = listOf(
            Podcast(
                id = PODCAST_ID, title = "Hipsters", description = "", imageUrl = null, author = null, language = null,
                categories = emptyList(), feedUrl = PODCAST_ID, siteUrl = null, lastUpdated = 0, isSubscribed = true,
            )
        )
        episodes.value = listOf(episode)
    }

    private fun text(res: StringResource, vararg args: Any) = runBlocking { getString(res, *args) }

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel() = PodcastDetailViewModel(
        PODCAST_ID, audioPlayer, PlayEpisodeUseCase(audioPlayer, repository),
        RefreshPodcastUseCase(FakeFeedSource(), repository), downloader, repository,
        CoroutineDispatchers(main = dispatcher, io = dispatcher, default = dispatcher), FakeAnalytics(),
    )

    @Test
    fun anEpisodeIsPlayedDownloadedAndItsDownloadRemovedFromItsMenu() = runComposeUiTest {
        setContent {
            PodcastTheme {
                PodcastDetailScreen(PODCAST_ID, viewModel(), onBackClick = {}, onEpisodeClick = { _, _ -> })
            }
        }
        waitUntilExactlyOneExists(hasText(episode.title))

        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.play)).performClick()
        waitUntil { audioPlayer.playCalledWith?.id == episode.id }

        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.download_cd)).performClick()
        waitUntil { downloader.downloadCalledWith?.id == episode.id }

        // The downloader finishes: the menu now offers to remove the file, after asking.
        downloader.activeDownloads.value = mapOf(episode.id to DownloadStatus.Completed("/downloads/e1.mp3"))
        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.delete_download)).performClick()
        onNodeWithText(text(Res.string.delete)).performClick()
        waitUntil { downloader.deleteCalledWith == episode.id }
    }

    private companion object {
        const val PODCAST_ID = "https://feeds.example.com/rss"
        val episode = Episode(
            id = "e1", podcastId = PODCAST_ID, title = "Episode 1", description = null, audioUrl = "a", imageUrl = null,
            duration = 60, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
        )
    }
}
