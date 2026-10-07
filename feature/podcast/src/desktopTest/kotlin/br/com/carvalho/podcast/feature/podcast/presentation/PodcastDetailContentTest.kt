package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.podcast
import br.com.carvalho.podcast.core.ui.generated.resources.show_less
import br.com.carvalho.podcast.core.ui.generated.resources.show_more
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import br.com.carvalho.podcast.core.ui.generated.resources.play_latest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.episode_options
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_unplayed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PodcastDetailContentTest {

    private fun text(res: StringResource) = runBlocking { getString(res) }

    // The paging items collect on Dispatchers.Main, which a desktop test does not have.
    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun podcast(description: String = "Short") = Podcast(
        id = "p", title = "Hipsters Ponto Tech", description = description, imageUrl = null, author = "Alura",
        language = null, categories = emptyList(), feedUrl = "p", siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private val episodes = (1..20).map {
        Episode(
            id = "e$it", podcastId = "p", title = "Episode $it", description = null, audioUrl = "a",
            imageUrl = null, duration = 60, publishDate = 0, isPlayed = false, playbackPosition = 0,
            isDownloaded = false, fileSize = null,
        )
    }

    @Test
    fun theBarNamesThePodcastOnceItsHeaderScrollsAway() = runComposeUiTest {
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false),
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(),
                )
            }
        }
        waitUntilExactlyOneExists(hasText("Episode 1"))
        onAllNodesWithText(text(Res.string.podcast)).assertCountEquals(0)
        onAllNodesWithText("Hipsters Ponto Tech").assertCountEquals(1)

        onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Episode 1"))).performScrollToIndex(15)

        // The header is gone and the bar carries the name instead.
        onAllNodesWithText("Hipsters Ponto Tech").assertCountEquals(1)
    }

    @Test
    fun theHeaderCountsTheEpisodesAndPlaysTheLatest() = runComposeUiTest {
        var played: Episode? = null
        var state by mutableStateOf(PodcastDetailUiState(podcast = podcast(), isLoading = false, episodeCount = 312))
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = state,
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onPlay = { played = it }),
                )
            }
        }
        onNodeWithText("312", substring = true).assertExists()
        // Nothing left to hear: no button.
        onNodeWithText(text(Res.string.play_latest)).assertDoesNotExist()

        state = state.copy(latestUnplayed = episodes.first())
        onNodeWithText(text(Res.string.play_latest)).performClick()

        assertEquals("e1", played?.id)
    }

    @Test
    fun aLongDescriptionIsCollapsedUntilAsked() = runComposeUiTest {
        val long = (1..40).joinToString(" ") { "Uma frase longa sobre tecnologia e carreira número $it." }
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(description = long), isLoading = false),
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(),
                )
            }
        }

        onNodeWithText(text(Res.string.show_more)).performClick()

        onNodeWithText(text(Res.string.show_less)).assertExists()
    }

    @Test
    fun markingOlderEpisodesAsksFirstAndCanBeCancelled() = runComposeUiTest {
        var cancelled = 0
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false, selectedEpisode = episodes.first()),
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onDismissMarkPlayed = { cancelled++ }),
                )
            }
        }

        onNodeWithText(text(Res.string.cancel)).performClick()

        assertEquals(1, cancelled)
    }

    @Test
    fun eachEpisodeHasItsActionsInAMenu() = runComposeUiTest {
        var older: Episode? = null
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false),
                    episodes = flowOf(PagingData.from(episodes.take(1))).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onEpisodeLongClick = { older = it }),
                )
            }
        }
        waitUntilExactlyOneExists(hasText("Episode 1"))

        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.play)).assertExists()
        onNodeWithText(text(Res.string.mark_older_as_played)).performClick()

        assertEquals("e1", older?.id)
    }

    @Test
    fun aPlayedEpisodeCanBeMarkedUnplayed() = runComposeUiTest {
        var unplayed: Episode? = null
        val played = episodes.first().copy(isPlayed = true)
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false),
                    episodes = flowOf(PagingData.from(listOf(played))).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onMarkUnplayed = { unplayed = it }),
                )
            }
        }
        waitUntilExactlyOneExists(hasText("Episode 1"))

        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.mark_as_played)).assertDoesNotExist()
        onNodeWithText(text(Res.string.mark_as_unplayed)).performClick()

        assertEquals("e1", unplayed?.id)
    }
}
