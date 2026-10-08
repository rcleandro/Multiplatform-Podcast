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
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.core.ui.generated.resources.library_show_grid
import br.com.carvalho.podcast.core.ui.generated.resources.library_show_list
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.core.ui.generated.resources.library_reorder
import br.com.carvalho.podcast.core.ui.generated.resources.library_organize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToIndex
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort
import br.com.carvalho.podcast.core.ui.generated.resources.library_sort_first_added
import br.com.carvalho.podcast.core.ui.generated.resources.continue_listening
import br.com.carvalho.podcast.domain.model.Episode
import kotlin.test.Test
import br.com.carvalho.podcast.core.ui.generated.resources.delete_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_options
import br.com.carvalho.podcast.core.designsystem.LocalMiniPlayerInset
import br.com.carvalho.podcast.core.designsystem.Sizes
import kotlin.test.assertTrue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performImeAction
import br.com.carvalho.podcast.core.ui.generated.resources.paste
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

private const val PODCASTS_TO_FILL = 20

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
        // One way to add while empty: the button in the message, no floating "+" next to it.
        onAllNodesWithContentDescription(text(Res.string.add_podcast)).assertCountEquals(0)
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
                    state = LibraryUiState(podcasts = listOf(LibraryEntry(podcast, 0, null))),
                    actions = LibraryActions(onPodcastClick = { opened = it }),
                )
            }
        }
        onNodeWithText("Hipsters").performClick()
        assertEquals("id-1", opened)
    }


    @Test
    fun theListShowsTheUnplayedCountAndTheButtonSwitchesLayout() = runComposeUiTest {
        var toggles = 0
        val podcast = Podcast("id-1", "Hipsters", "", null, "Alura", null, emptyList(), "url", null, 0, true)
        setContent {
            PodcastTheme {
                LibraryContent(
                    state = LibraryUiState(podcasts = listOf(LibraryEntry(podcast, 3, null)), layout = LibraryLayout.LIST),
                    actions = LibraryActions(onToggleLayout = { toggles++ }),
                )
            }
        }
        onNodeWithText("3", substring = true).assertExists()
        onNodeWithContentDescription(text(Res.string.library_show_grid)).performClick()
        assertEquals(1, toggles)
    }

    @Test
    fun theGridCardShowsTheUnplayedBadge() = runComposeUiTest {
        val podcast = Podcast("id-1", "Hipsters", "", null, "Alura", null, emptyList(), "url", null, 0, true)
        setContent {
            PodcastTheme { LibraryContent(LibraryUiState(podcasts = listOf(LibraryEntry(podcast, 7, null))), LibraryActions()) }
        }
        onNodeWithText("7").assertExists()
        onNodeWithContentDescription(text(Res.string.library_show_list)).assertExists()
    }

    @Test
    fun choosingASortGoesBackToTheTopOfTheNewOrder() = choosingASortGoesBackToTheTop(LibraryLayout.LIST)

    @Test
    fun choosingASortGoesBackToTheTopOfTheGridToo() = choosingASortGoesBackToTheTop(LibraryLayout.GRID)

    private fun choosingASortGoesBackToTheTop(layout: LibraryLayout) = runComposeUiTest {
        val entries = (1..30).map {
            LibraryEntry(Podcast("id-$it", "Podcast $it", "", null, null, null, emptyList(), "url$it", null, 0, true), 0, null)
        }
        // Like the view model: the chosen sort comes back with the list in the new order.
        var state by mutableStateOf(LibraryUiState(podcasts = entries, layout = layout))
        setContent {
            PodcastTheme {
                LibraryContent(
                    state = state,
                    actions = LibraryActions(onSortChange = { state = state.copy(sort = it, podcasts = entries.reversed()) }),
                )
            }
        }
        onNode(hasScrollToIndexAction()).performScrollToIndex(entries.lastIndex)

        onNodeWithContentDescription(text(Res.string.library_sort)).performClick()
        onNodeWithText(text(Res.string.library_sort_first_added)).performClick()

        onNodeWithText("Podcast 30").assertIsDisplayed()
    }

    @Test
    fun theCustomOrderIsArrangedInItsOwnScreen() = runComposeUiTest {
        var organize = 0
        val podcast = Podcast("id-1", "Hipsters", "", null, "Alura", null, emptyList(), "url", null, 0, true)
        setContent {
            PodcastTheme {
                LibraryContent(
                    state = LibraryUiState(podcasts = listOf(LibraryEntry(podcast, 0, null)), sort = LibrarySort.CUSTOM),
                    actions = LibraryActions(onOrganize = { organize++ }),
                )
            }
        }
        // The library itself stays clean: no drag handles, even in the custom order.
        onAllNodesWithContentDescription(text(Res.string.library_reorder)).assertCountEquals(0)

        onNodeWithContentDescription(text(Res.string.library_sort)).performClick()
        onNodeWithText(text(Res.string.library_organize)).performClick()

        assertEquals(1, organize)
    }

    @Test
    fun theAddDialogIsReadyToTypeAndSendsWithTheKeyboard() = runComposeUiTest {
        var confirmed = 0
        setContent {
            PodcastTheme {
                LibraryContent(
                    state = LibraryUiState(isAddDialogOpen = true, addUrl = "feeds.example.com/rss"),
                    actions = LibraryActions(onAddConfirm = { confirmed++ }),
                )
            }
        }
        val field = onNode(hasSetTextAction())

        field.assertIsFocused()
        field.performImeAction()

        assertEquals(1, confirmed)
    }

    @Test
    fun pasteFillsTheAddress() = runComposeUiTest {
        var typed: String? = null
        val clipboard = object : ClipboardManager {
            override fun getText() = AnnotatedString("https://feeds.example.com/rss")
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        setContent {
            PodcastTheme {
                CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                    LibraryContent(
                        state = LibraryUiState(isAddDialogOpen = true),
                        actions = LibraryActions(onUrlChange = { typed = it }),
                    )
                }
            }
        }

        onNodeWithContentDescription(text(Res.string.paste)).performClick()

        assertEquals("https://feeds.example.com/rss", typed)
    }

    @Test
    fun theAddButtonDoesNotCoverTheLastPodcast() = runComposeUiTest {
        val entries = (1..12).map {
            LibraryEntry(Podcast("id-$it", "Podcast $it", "", null, null, null, emptyList(), "url$it", null, 0, true), 0, null)
        }
        // With something playing, the mini player pushes the "+" up.
        setContent {
            PodcastTheme {
                CompositionLocalProvider(LocalMiniPlayerInset provides Sizes.miniPlayerHeight) {
                    LibraryContent(
                        state = LibraryUiState(podcasts = entries, layout = LibraryLayout.LIST),
                        actions = LibraryActions(),
                    )
                }
            }
        }
        onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Podcast 1"))).performScrollToIndex(entries.lastIndex)

        val last = onNodeWithText("Podcast 12").getBoundsInRoot()
        val add = onNodeWithContentDescription(text(Res.string.add_podcast)).getBoundsInRoot()
        assertTrue(last.bottom <= add.top, "${last.bottom} vs ${add.top}")
    }

    @Test
    fun aPodcastsMenuOffersToDeleteIt() = runComposeUiTest {
        var toDelete: Podcast? = null
        val podcast = Podcast("id-1", "Hipsters", "", null, "Alura", null, emptyList(), "url", null, 0, true)
        setContent {
            PodcastTheme {
                LibraryContent(
                    state = LibraryUiState(podcasts = listOf(LibraryEntry(podcast, 0, null))),
                    actions = LibraryActions(onPodcastLongClick = { toDelete = it }),
                )
            }
        }

        onNodeWithContentDescription(text(Res.string.podcast_options)).performClick()
        onNodeWithText(text(Res.string.delete_podcast)).performClick()

        assertEquals("id-1", toDelete?.id)
    }

    @Test
    fun screenTitlesAreHeadings() = runComposeUiTest {
        setContent { PodcastTheme { LibraryContent(state = LibraryUiState(), actions = LibraryActions()) } }
        onNode(isHeading() and hasText(text(Res.string.library_title))).assertExists()
    }

    @Test
    fun continueListeningShowsOnlyWhenSomethingIsStartedAndPlaysIt() = runComposeUiTest {
        var played: String? = null
        val podcast = Podcast("id-1", "Hipsters", "", null, "Alura", null, emptyList(), "url", null, 0, true)
        val started = Episode(
            id = "e1", podcastId = "id-1", title = "Started episode", description = null, audioUrl = "a",
            imageUrl = null, duration = 1_800, publishDate = 0, isPlayed = false, playbackPosition = 720_000,
            isDownloaded = false, fileSize = null,
        )
        // Enough podcasts to fill the screen, so the grid could keep the first one in place.
        val podcasts = List(PODCASTS_TO_FILL) { LibraryEntry(podcast.copy(id = "id-$it"), 0, null) }
        var state by mutableStateOf(LibraryUiState(podcasts = podcasts))
        setContent {
            PodcastTheme { LibraryContent(state = state, actions = LibraryActions(onPlay = { played = it.id })) }
        }
        onNode(isHeading() and hasText(text(Res.string.continue_listening), ignoreCase = true)).assertDoesNotExist()

        state = state.copy(inProgress = listOf(started))
        // It arrives after the podcasts: the library at its top shows it, instead of keeping the first podcast there.
        onNode(isHeading() and hasText(text(Res.string.continue_listening), ignoreCase = true)).assertIsDisplayed()
        onNodeWithText("Started episode").performClick()

        assertEquals("e1", played)
    }
}
