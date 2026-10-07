package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
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
}
