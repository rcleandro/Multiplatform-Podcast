package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.component.EXPLICIT_BADGE_TAG
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_by_url
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.category_news
import br.com.carvalho.podcast.core.ui.generated.resources.category_technology
import br.com.carvalho.podcast.core.ui.generated.resources.error_search_directory
import br.com.carvalho.podcast.core.ui.generated.resources.for_you
import br.com.carvalho.podcast.core.ui.generated.resources.more_options
import br.com.carvalho.podcast.core.ui.generated.resources.opened_link
import br.com.carvalho.podcast.core.ui.generated.resources.paste
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_episode_count
import br.com.carvalho.podcast.core.ui.generated.resources.popular
import br.com.carvalho.podcast.core.ui.generated.resources.settings_import_opml
import br.com.carvalho.podcast.core.ui.generated.resources.similar_none
import br.com.carvalho.podcast.core.ui.generated.resources.similar_to
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe_to
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DiscoverContentTest {
    private fun text(res: StringResource, vararg args: Any) = runBlocking { getString(res, *args) }

    private val hipsters = DirectoryResult(
        DirectoryPodcast(
            title = "Hipsters Ponto Tech", author = "Alura", artworkUrl = null,
            feedUrl = "https://hipsters.tech/feed/podcast/", genre = "Tecnologia", episodeCount = 629,
        ),
        isSubscribed = false,
    )

    @Test
    fun beforeSearchingTheScreenShowsThePopularPodcasts() = runComposeUiTest {
        setContent { PodcastTheme { DiscoverContent(DiscoverUiState(results = listOf(hipsters)), DiscoverActions()) } }

        onNode(isHeading() and hasText(text(Res.string.add_podcast))).assertExists()
        onNode(isHeading() and hasText(text(Res.string.popular), ignoreCase = true)).assertExists()
        onNodeWithText(hipsters.podcast.title).assertExists()
        // The search field takes the keyboard as soon as the screen opens.
        onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun aCategoryChartIsTitledWithTheCategoryAndSearchResultsAreNot() = runComposeUiTest {
        var state by mutableStateOf(DiscoverUiState(category = PodcastCategory.NEWS, results = listOf(hipsters)))
        setContent { PodcastTheme { DiscoverContent(state, DiscoverActions()) } }

        onNode(isHeading() and hasText(text(Res.string.category_news), ignoreCase = true)).assertExists()

        state = DiscoverUiState(query = "hipsters", results = listOf(hipsters))
        onNode(isHeading() and hasText(text(Res.string.popular), ignoreCase = true)).assertDoesNotExist()
    }

    @Test
    fun aLinkedPodcastThatArrivesAfterTheChartShowsAtTheTop() = runComposeUiTest {
        val other = hipsters.copy(podcast = hipsters.podcast.copy(title = "NerdCast", feedUrl = "https://nerd.cast/rss"))
        var state by mutableStateOf(DiscoverUiState(results = listOf(hipsters), isOpeningLink = true))
        setContent { PodcastTheme { DiscoverContent(state, DiscoverActions()) } }
        waitForIdle()

        state = state.copy(linked = other, isOpeningLink = false)

        onNodeWithText(text(Res.string.opened_link), ignoreCase = true).assertIsDisplayed()
        onNodeWithText("NerdCast").assertIsDisplayed()
        onNodeWithText(text(Res.string.popular), ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun scrollingTheResultsLetsGoOfTheSearchField() = runComposeUiTest {
        val many = (1..30).map { hipsters.copy(podcast = hipsters.podcast.copy(title = "Podcast $it", feedUrl = "https://p$it.fm/rss")) }
        setContent { PodcastTheme { DiscoverContent(DiscoverUiState(results = many), DiscoverActions()) } }
        onNode(hasSetTextAction()).assertIsFocused()

        onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Podcast 1"))).performTouchInput { swipeUp() }

        onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun aLinkLeavesTheKeyboardClosed() = runComposeUiTest {
        setContent { PodcastTheme { DiscoverContent(DiscoverUiState(isOpeningLink = true), DiscoverActions()) } }

        onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun openedForALinkNotYetTakenTheSearchStaysUnfocused() = runComposeUiTest {
        setContent { PodcastTheme { DiscoverContent(DiscoverUiState(), DiscoverActions(), focusSearch = false) } }

        onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun withNothingFoundTheAddressIsOffered() = runComposeUiTest {
        var opened = 0
        setContent {
            PodcastTheme { DiscoverContent(DiscoverUiState(query = "xyz"), DiscoverActions(onOpenUrlDialog = { opened++ })) }
        }

        onNodeWithText(text(Res.string.add_by_url)).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun categoriesShowWhileNothingIsTypedAndOpenTheirChart() = runComposeUiTest {
        var chosen: PodcastCategory? = null
        var state by mutableStateOf(DiscoverUiState())
        setContent {
            PodcastTheme { DiscoverContent(state, DiscoverActions(onCategoryChange = { chosen = it })) }
        }

        onNodeWithText(text(Res.string.category_technology)).performScrollTo().performClick()
        assertEquals(PodcastCategory.TECHNOLOGY, chosen)

        // The open category closes with another tap.
        state = DiscoverUiState(category = PodcastCategory.TECHNOLOGY, results = listOf(hipsters))
        onNodeWithText(hipsters.podcast.title).assertExists()
        onNodeWithText(text(Res.string.category_technology)).performScrollTo().performClick()
        assertEquals(null, chosen)

        state = DiscoverUiState(query = "hipsters", results = listOf(hipsters))
        onAllNodesWithText(text(Res.string.category_technology)).assertCountEquals(0)
    }

    @Test
    fun aResultShowsItsDetailsAndSubscribes() = runComposeUiTest {
        var subscribed: DirectoryResult? = null
        setContent {
            PodcastTheme {
                DiscoverContent(
                    DiscoverUiState(query = "hipsters", results = listOf(hipsters)),
                    DiscoverActions(onSubscribe = { subscribed = it }),
                )
            }
        }

        val episodes = runBlocking { getPluralString(Res.plurals.podcast_episode_count, 629, 629) }
        onNodeWithText("Tecnologia · $episodes").assertExists()
        onNodeWithContentDescription(text(Res.string.subscribe_to, hipsters.podcast.title)).performClick()

        assertEquals(hipsters, subscribed)
    }

    @Test
    fun anExplicitPodcastSaysSo() = runComposeUiTest {
        val explicit = hipsters.copy(podcast = hipsters.podcast.copy(explicit = true))
        setContent { PodcastTheme { DiscoverContent(DiscoverUiState(query = "x", results = listOf(explicit)), DiscoverActions()) } }

        // The "E" badge, read aloud as the word; the line under the title keeps only the category.
        onNodeWithTag(EXPLICIT_BADGE_TAG, useUnmergedTree = true).assertExists()
        onNodeWithText("Tecnologia", substring = true).assertExists()
    }

    @Test
    fun aPodcastInTheLibraryShowsAsSubscribed() = runComposeUiTest {
        setContent {
            PodcastTheme {
                DiscoverContent(
                    DiscoverUiState(query = "hipsters", results = listOf(hipsters.copy(isSubscribed = true))),
                    DiscoverActions(),
                )
            }
        }

        onNodeWithContentDescription(text(Res.string.subscribed)).assertExists()
    }

    @Test
    fun aFailedSearchOffersToTryAgain() = runComposeUiTest {
        var retried = 0
        setContent {
            PodcastTheme {
                DiscoverContent(DiscoverUiState(query = "news", searchFailed = true), DiscoverActions(onRetry = { retried++ }))
            }
        }

        onNodeWithText(text(Res.string.error_search_directory)).assertExists()
        onNodeWithText(text(Res.string.try_again)).performClick()

        assertEquals(1, retried)
    }

    @Test
    fun theMenuOffersToAddByAddress() = runComposeUiTest {
        var opened = 0
        setContent {
            PodcastTheme {
                DiscoverContent(DiscoverUiState(query = "hipsters", results = listOf(hipsters)), DiscoverActions(onOpenUrlDialog = { opened++ }))
            }
        }

        onNodeWithContentDescription(text(Res.string.more_options)).performClick()
        onNodeWithText(text(Res.string.add_by_url)).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun theMenuOffersToImportAnOpmlFile() = runComposeUiTest {
        var imported = 0
        setContent { PodcastTheme { DiscoverContent(DiscoverUiState(), DiscoverActions(onImportOpml = { imported++ })) } }

        onNodeWithContentDescription(text(Res.string.more_options)).performClick()
        onNodeWithText(text(Res.string.settings_import_opml)).performClick()

        assertEquals(1, imported)
    }

    @Test
    fun anImportShowsHowFarItWent() = runComposeUiTest {
        setContent { PodcastTheme { DiscoverContent(DiscoverUiState(importProgress = 1 to 4), DiscoverActions()) } }

        onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.25f, 0f..1f)))
            .assertExists()
    }

    @Test
    fun theAddressDialogIsReadyToTypeAndSendsWithTheKeyboard() = runComposeUiTest {
        var confirmed = 0
        setContent {
            PodcastTheme {
                DiscoverContent(
                    DiscoverUiState(isUrlDialogOpen = true, url = "feeds.example.com/rss"),
                    DiscoverActions(onConfirmUrl = { confirmed++ }),
                )
            }
        }
        val field = onNode(hasSetTextAction() and hasText("feeds.example.com/rss"))

        field.assertIsFocused()
        field.performImeAction()

        assertEquals(1, confirmed)
    }

    @Test
    fun pasteFillsTheAddress() = runComposeUiTest {
        var typed: String? = null
        @Suppress("DEPRECATION")
        val clipboard = object : ClipboardManager {
            override fun getText() = AnnotatedString("https://feeds.example.com/rss")
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        setContent {
            PodcastTheme {
                @Suppress("DEPRECATION")
                CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                    DiscoverContent(DiscoverUiState(isUrlDialogOpen = true), DiscoverActions(onUrlChange = { typed = it }))
                }
            }
        }

        onNodeWithContentDescription(text(Res.string.paste)).performClick()

        assertEquals("https://feeds.example.com/rss", typed)
    }

    @Test
    fun onAWideWindowTheResultsKeepAReadableWidth() = runDesktopComposeUiTest(width = WIDE, height = TALL) {
        setContent {
            PodcastTheme { DiscoverContent(DiscoverUiState(query = "hipsters", results = listOf(hipsters)), DiscoverActions()) }
        }

        val row = onNodeWithText(hipsters.podcast.title).fetchSemanticsNode().boundsInRoot
        assertTrue(row.right - row.left <= with(density) { Sizes.readableWidth.toPx() }, "$row")
    }

    private companion object {
        const val WIDE = 1200
        const val TALL = 800
    }

    @Test
    fun forYouShowsAboveThePopularOnesAndSimilarPodcastsTakeItsPlace() = runComposeUiTest {
        val suggested = hipsters.copy(podcast = hipsters.podcast.copy(title = "NerdCast", feedUrl = "https://nerd.cast/rss"))
        var state by mutableStateOf(DiscoverUiState(results = listOf(hipsters), forYou = listOf(suggested)))
        setContent { PodcastTheme { DiscoverContent(state, DiscoverActions()) } }

        val forYou = onNode(isHeading() and hasText(text(Res.string.for_you), ignoreCase = true)).getBoundsInRoot()
        val popular = onNode(isHeading() and hasText(text(Res.string.popular), ignoreCase = true)).getBoundsInRoot()
        assertTrue(forYou.bottom <= popular.top, "for you at $forYou should be above the popular ones at $popular")
        onNodeWithText("NerdCast").assertExists()

        state = state.copy(similar = SimilarPodcasts("Mine", emptyList()))
        onNode(isHeading() and hasText(runBlocking { getString(Res.string.similar_to, "Mine") }, ignoreCase = true))
            .assertExists()
        onNodeWithText(text(Res.string.similar_none)).assertExists()
        onNode(isHeading() and hasText(text(Res.string.for_you), ignoreCase = true)).assertDoesNotExist()
    }
}
