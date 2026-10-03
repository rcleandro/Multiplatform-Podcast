package br.com.carvalho.podcast.feature.library.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.library_title
import br.com.carvalho.podcast.core.ui.generated.resources.no_podcasts_found
import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class LibraryContentTest {

    private fun text(res: StringResource) = runBlocking { getString(res) }

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
    fun screenTitlesAreHeadings() = runComposeUiTest {
        setContent { PodcastTheme { LibraryContent(state = LibraryUiState(), actions = LibraryActions()) } }
        onNode(isHeading() and hasText(text(Res.string.library_title))).assertExists()
    }
}
