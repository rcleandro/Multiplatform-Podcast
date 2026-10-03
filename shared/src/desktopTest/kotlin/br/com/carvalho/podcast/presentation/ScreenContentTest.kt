package br.com.carvalho.podcast.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.feature.library.presentation.LibraryActions
import br.com.carvalho.podcast.feature.library.presentation.LibraryContent
import br.com.carvalho.podcast.feature.library.presentation.LibraryUiState
import br.com.carvalho.podcast.feature.player.presentation.PlayerActions
import br.com.carvalho.podcast.feature.player.presentation.PlayerContent
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.add_podcast
import br.com.carvalho.podcast.shared.next
import br.com.carvalho.podcast.shared.no_podcasts_found
import br.com.carvalho.podcast.shared.previous
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScreenContentTest {

    private fun text(res: StringResource) = runBlocking { getString(res) }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p", podcastTitle = "Podcast", title = "Episode $id", description = null,
        audioUrl = "", imageUrl = null, duration = 60, publishDate = 0, isPlayed = false,
        playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    @Test
    fun emptyLibraryOffersToAddAPodcast() = runComposeUiTest {
        var addClicks = 0
        setContent {
            PodcastTheme { LibraryContent(state = LibraryUiState(), actions = LibraryActions(onAddClick = { addClicks++ })) }
        }
        onNodeWithText(text(Res.string.no_podcasts_found)).assertExists()
        // The empty state button and the floating button share the label; the empty state one is a text button.
        onNodeWithText(text(Res.string.add_podcast)).performClick()
        assertEquals(1, addClicks)
    }

    @Test
    fun libraryOpensTheTappedPodcast() = runComposeUiTest {
        var opened: String? = null
        val podcast = Podcast("id-1", "Hipsters", "", null, "Alura", null, emptyList(), "url", null, 0, true)
        setContent {
            PodcastTheme {
                LibraryContent(
                    state = LibraryUiState(podcasts = listOf(podcast)),
                    actions = LibraryActions(onPodcastClick = { opened = it }),
                )
            }
        }
        onNodeWithText("Hipsters").performClick()
        assertEquals("id-1", opened)
    }

    @Test
    fun playerDisablesNextOnTheLastEpisodeOfTheQueue() = runComposeUiTest {
        val queue = listOf(episode("1"), episode("2"))
        setContent {
            PodcastTheme {
                PlayerContent(state = PlayerState(currentEpisode = queue.last(), queue = queue), actions = PlayerActions())
            }
        }
        onNodeWithContentDescription(text(Res.string.next)).assertIsNotEnabled()
        onNodeWithContentDescription(text(Res.string.previous)).assertIsEnabled()
    }
}
