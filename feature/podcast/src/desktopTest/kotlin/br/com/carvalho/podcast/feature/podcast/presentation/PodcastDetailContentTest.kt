package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_to_queue
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.close_selection
import br.com.carvalho.podcast.core.ui.generated.resources.episode_options
import br.com.carvalho.podcast.core.ui.generated.resources.episode_trailer
import br.com.carvalho.podcast.core.ui.generated.resources.episodes_oldest_first
import br.com.carvalho.podcast.core.ui.generated.resources.explicit
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_unplayed_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.play_latest
import br.com.carvalho.podcast.core.ui.generated.resources.play_next
import br.com.carvalho.podcast.core.ui.generated.resources.podcast
import br.com.carvalho.podcast.core.ui.generated.resources.season_number
import br.com.carvalho.podcast.core.ui.generated.resources.selected_count
import br.com.carvalho.podcast.core.ui.generated.resources.share
import br.com.carvalho.podcast.core.ui.generated.resources.show_less
import br.com.carvalho.podcast.core.ui.generated.resources.show_more
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeType
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.usecase.EpisodeAction
import br.com.carvalho.podcast.presentation.component.OlderMark
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private fun text(res: StringResource, vararg args: Any) = runBlocking { getString(res, *args) }

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
    fun thePodcastIsSharedFromTheBarOnceItIsLoaded() = runComposeUiTest {
        var shared = 0
        var state by mutableStateOf(PodcastDetailUiState(isLoading = true))
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = state,
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onShare = { shared++ }),
                )
            }
        }
        onAllNodesWithContentDescription(text(Res.string.share)).assertCountEquals(0)

        state = PodcastDetailUiState(podcast = podcast(), isLoading = false)
        onNodeWithContentDescription(text(Res.string.share)).performClick()

        assertEquals(1, shared)
    }

    @Test
    fun theBarMenuListsTheEpisodesOldestFirst() = runComposeUiTest {
        var oldestFirst: Boolean? = null
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast().copy(serial = true), isLoading = false),
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onOldestFirstChange = { oldestFirst = it }),
                )
            }
        }

        onNodeWithContentDescription(text(Res.string.library_sort)).performClick()
        // A serial show starts oldest first.
        onNodeWithText(text(Res.string.episodes_oldest_first)).assertIsSelected().performClick()

        assertEquals(true, oldestFirst)
    }

    @Test
    fun seasonsGetAHeaderAndTrailersSaySo() = runComposeUiTest {
        val seasons = listOf(
            episodes[0].copy(season = 2, type = EpisodeType.TRAILER),
            episodes[1].copy(season = 2),
            episodes[2].copy(season = 1),
        )
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false),
                    episodes = flowOf(PagingData.from(seasons)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(),
                )
            }
        }

        waitUntilExactlyOneExists(hasText(text(Res.string.season_number, 2)))
        onAllNodesWithText(text(Res.string.season_number, 1)).assertCountEquals(1)
        onNodeWithText(text(Res.string.episode_trailer), substring = true).assertExists()
    }

    @Test
    fun whileSelectingTheBarCountsTheEpisodesAndActsOnThem() = runComposeUiTest {
        var acted: EpisodeAction? = null
        var closed = 0
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(
                        podcast = podcast(),
                        isLoading = false,
                        selection = episodes.take(2),
                    ),
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onActOnSelection = { acted = it }, onClearSelection = { closed++ }),
                )
            }
        }

        onNodeWithText(text(Res.string.selected_count, 2)).assertExists()
        onNodeWithContentDescription(text(Res.string.mark_as_played)).performClick()
        onNodeWithContentDescription(text(Res.string.close_selection)).performClick()

        assertEquals(EpisodeAction.MARK_PLAYED, acted)
        assertEquals(1, closed)
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
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false, olderMark = OlderMark(episodes.first(), played = true)),
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onDismissMarkOlder = { cancelled++ }),
                )
            }
        }

        onNodeWithText(text(Res.string.cancel)).performClick()

        assertEquals(1, cancelled)
    }

    @Test
    fun anExplicitEpisodeSaysSo() = runComposeUiTest {
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false),
                    episodes = flowOf(PagingData.from(listOf(episodes.first().copy(explicit = true))))
                        .collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(),
                )
            }
        }
        waitUntilExactlyOneExists(hasText("Episode 1"))

        onNodeWithText(text(Res.string.explicit), substring = true).assertExists()
    }

    @Test
    fun eachEpisodeHasItsActionsInAMenu() = runComposeUiTest {
        var older: OlderMark? = null
        val enqueued = mutableListOf<Pair<String, Boolean>>()
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false),
                    episodes = flowOf(PagingData.from(episodes.take(1))).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(
                        onRequestMarkOlder = { older = it },
                        onEnqueue = { episode, next -> enqueued += episode.id to next },
                    ),
                )
            }
        }
        waitUntilExactlyOneExists(hasText("Episode 1"))

        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.play)).assertExists()
        onNodeWithText(text(Res.string.play_next)).performClick()
        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.add_to_queue)).performClick()
        assertEquals(listOf(episodes.first().id to true, episodes.first().id to false), enqueued)

        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.mark_older_as_played)).performClick()

        assertEquals(OlderMark(episodes.first(), played = true), older)
        // Older ones may be played even when this one is not.
        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.mark_older_as_unplayed)).performClick()
        assertEquals(OlderMark(episodes.first(), played = false), older)
    }

    @Test
    fun aPlayedEpisodeAndTheOlderOnesCanBeMarkedUnplayed() = runComposeUiTest {
        var older: OlderMark? = null
        val played = episodes.first().copy(isPlayed = true)
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(podcast = podcast(), isLoading = false),
                    episodes = flowOf(PagingData.from(listOf(played))).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(onRequestMarkOlder = { older = it }),
                )
            }
        }
        waitUntilExactlyOneExists(hasText("Episode 1"))

        onNodeWithContentDescription(text(Res.string.episode_options)).performClick()
        onNodeWithText(text(Res.string.mark_older_as_unplayed)).performClick()

        assertEquals(OlderMark(played, played = false), older)
    }

    @Test
    fun markingOlderAsUnplayedAsksWithItsOwnMessage() = runComposeUiTest {
        setContent {
            PodcastTheme {
                PodcastDetailContent(
                    state = PodcastDetailUiState(
                        podcast = podcast(), isLoading = false, olderMark = OlderMark(episodes.first(), played = false)
                    ),
                    episodes = flowOf(PagingData.from(episodes)).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = PodcastDetailActions(),
                )
            }
        }

        onNodeWithText(text(Res.string.mark_older_unplayed_confirmation, "Episode 1")).assertExists()
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
