package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.transcript_failed
import br.com.carvalho.podcast.core.ui.generated.resources.transcript_no_match
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import br.com.carvalho.podcast.domain.model.TranscriptLine
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TranscriptContentTest {
    private val lines = listOf(
        TranscriptLine(0, 5_000, "Welcome to the show", "Alice"),
        TranscriptLine(65_000, 69_000, "Today we talk about Kotlin"),
    )

    @Test
    fun theLineBeingSaidIsMarkedWithItsSpeakerAndATapPlaysFromIt() = runComposeUiTest {
        val intents = mutableListOf<TranscriptIntent>()
        setContent {
            PodcastTheme {
                TranscriptContent(
                    TranscriptUiState(lines = lines, shown = listOf(0, 1), current = 0),
                    onBack = {},
                    onIntent = { intents += it },
                )
            }
        }

        onNode(hasText("Welcome to the show")).assertIsSelected()
        onNodeWithText("Alice").assertExists()
        onNodeWithText("Today we talk about Kotlin").performClick()

        assertEquals(listOf<TranscriptIntent>(TranscriptIntent.PlayFrom(lines[1])), intents)
    }

    @Test
    fun aSearchShowsWhenEachFoundLineIsSaidOrThatNoneIs() = runComposeUiTest {
        var state by mutableStateOf(
            TranscriptUiState(lines = lines, shown = listOf(1), current = 0, query = "kotlin")
        )
        setContent { PodcastTheme { TranscriptContent(state, onBack = {}, onIntent = {}) } }

        onNodeWithText("1:05").assertExists()
        onNode(hasText("Welcome to the show")).assertDoesNotExist()

        state = state.copy(shown = emptyList(), query = "java")
        onNodeWithText(runBlocking { getString(Res.string.transcript_no_match, "java") }).assertExists()
    }

    @Test
    fun aTranscriptThatFailedToLoadOffersToTryAgain() = runComposeUiTest {
        val intents = mutableListOf<TranscriptIntent>()
        setContent { PodcastTheme { TranscriptContent(TranscriptUiState(failed = true), onBack = {}, onIntent = { intents += it }) } }

        onNodeWithText(runBlocking { getString(Res.string.transcript_failed) }).assertExists()
        onNodeWithText(runBlocking { getString(Res.string.try_again) }).performClick()

        assertEquals(listOf<TranscriptIntent>(TranscriptIntent.Retry), intents)
    }
}
