package br.com.carvalho.podcast.presentation.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PodcastCardTest {

    @Test
    fun podcastCardDisplaysTitleAndAuthor() = runComposeUiTest {
        val podcast = Podcast(
            id = "1",
            title = "Teste Podcast",
            description = "Desc",
            imageUrl = null,
            author = "Teste Author",
            language = "pt",
            categories = listOf("Tech"),
            feedUrl = "url",
            siteUrl = null,
            lastUpdated = 0,
            isSubscribed = true
        )

        var clicked = false

        setContent {
            PodcastCard(
                podcast = podcast,
                onClick = { clicked = true },
                onLongClick = {}
            )
        }

        // Check if title is displayed
        onNodeWithText("Teste Podcast").assertExists()

        // Check if author is displayed
        onNodeWithText("Teste Author").assertExists()

        // Test click
        onNodeWithText("Teste Podcast").performClick()
        assertEquals(true, clicked)
    }
}
