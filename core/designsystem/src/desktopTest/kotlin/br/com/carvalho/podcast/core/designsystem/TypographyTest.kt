package br.com.carvalho.podcast.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class TypographyTest {

    @Test
    fun everyStyleUsesTheBundledFontFamily() = runComposeUiTest {
        var styles: List<TextStyle> = emptyList()
        setContent {
            PodcastTheme {
                styles = MaterialTheme.typography.all() +
                    listOf(PodcastTheme.typography.timer, PodcastTheme.typography.section)
            }
        }
        waitForIdle()

        val families = styles.map { it.fontFamily }.distinct()
        assertEquals(1, families.size, "Styles use different families: $families")
        assertNotEquals<FontFamily?>(FontFamily.Default, families.single())
        assertNotEquals(null, families.single())
    }

    @Test
    fun timerUsesTabularFigures() {
        assertEquals("tnum", podcastExtraTypography(FontFamily.Default).timer.fontFeatureSettings)
    }

    private fun Typography.all() = listOf(
        displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
        titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall,
    )
}
