package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_in_progress
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.PlayerState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.LocalMiniPlayerInset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class SearchContentTest {

    private fun text(res: StringResource) = runBlocking { getString(res) }

    // The paging items collect on Dispatchers.Main, which a desktop test does not have.
    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    @Test
    fun theFiltersChangeWhatTheListShows() = runComposeUiTest {
        var chosen: EpisodeListFilter? = null
        setContent {
            PodcastTheme {
                SearchContent(
                    state = SearchUiState(),
                    results = flowOf(PagingData.empty<Episode>()).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = SearchActions(onFilterChange = { chosen = it }),
                )
            }
        }

        onNodeWithText(text(Res.string.filter_in_progress)).performClick()

        assertEquals(EpisodeListFilter.IN_PROGRESS, chosen)
    }

    @Test
    fun theDownloadedFilterShowsTheSpaceTheDownloadsTake() = runComposeUiTest {
        val episode = Episode(
            id = "e1", podcastId = "p", title = "Downloaded episode", description = null, audioUrl = "a", imageUrl = null,
            duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = true, fileSize = null
        )
        setContent {
            PodcastTheme {
                SearchContent(
                    state = SearchUiState(filter = EpisodeListFilter.DOWNLOADED, usedBytes = 52_428_800L),
                    results = flowOf(PagingData.from(listOf(episode))).collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = SearchActions(),
                )
            }
        }

        waitUntilExactlyOneExists(hasText("Downloaded episode"))
        onNodeWithText(text(Res.string.filter_downloaded)).assertExists()
        onNodeWithText("50", substring = true).assertExists()
    }

    @Test
    fun anotherFilterStartsAtTheTop() = runComposeUiTest {
        val episodes = (1..30).map {
            Episode(
                id = "e$it", podcastId = "p", title = "Episode $it", description = null, audioUrl = "a",
                imageUrl = null, duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0,
                isDownloaded = false, fileSize = null,
            )
        }
        // Like the view model: one flow, a new pager when the filter changes, loading through a paging source.
        var state by mutableStateOf(SearchUiState())
        val filters = MutableStateFlow(state.filter)
        val pages = filters.flatMapLatest { Pager(PagingConfig(pageSize = 10)) { ListPagingSource(episodes) }.flow }
        setContent {
            PodcastTheme {
                SearchContent(
                    state = state,
                    results = pages.collectAsLazyPagingItems(),
                    playerState = PlayerState(),
                    activeDownloads = emptyMap(),
                    actions = SearchActions(onFilterChange = {
                        state = state.copy(filter = it)
                        filters.value = it
                    }),
                )
            }
        }
        waitUntilExactlyOneExists(hasText("Episode 1"))
        // The filter chips scroll too: aim at the list of episodes.
        onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Episode 1"))).performScrollToIndex(episodes.lastIndex)
        onNodeWithText("Episode 1").assertDoesNotExist()

        onNodeWithText(text(Res.string.filter_in_progress)).performClick()

        waitUntilExactlyOneExists(hasText("Episode 1"))
        onNodeWithText("Episode 1").assertIsDisplayed()
    }

    @Test
    fun messagesShowAboveTheMiniPlayer() = runComposeUiTest {
        val snackbar = SnackbarHostState()
        setContent {
            PodcastTheme {
                CompositionLocalProvider(LocalMiniPlayerInset provides MINI_PLAYER) {
                    SearchContent(
                        state = SearchUiState(),
                        results = flowOf(PagingData.empty<Episode>()).collectAsLazyPagingItems(),
                        playerState = PlayerState(),
                        activeDownloads = emptyMap(),
                        actions = SearchActions(),
                        snackbarHostState = snackbar,
                    )
                }
            }
            LaunchedEffect(Unit) { snackbar.showSnackbar("Download removed") }
        }

        val screen = onRoot().getBoundsInRoot()
        val message = onNodeWithText("Download removed").getBoundsInRoot()
        assertTrue(message.bottom <= screen.bottom - MINI_PLAYER, "${message.bottom} vs ${screen.bottom}")
    }

    private companion object {
        val MINI_PLAYER = 64.dp
    }
}

/** All of [items] in one page: enough for the list to go through a real refresh. */
private class ListPagingSource(private val items: List<Episode>) : PagingSource<Int, Episode>() {
    override fun getRefreshKey(state: PagingState<Int, Episode>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> =
        LoadResult.Page(items, prevKey = null, nextKey = null)
}
