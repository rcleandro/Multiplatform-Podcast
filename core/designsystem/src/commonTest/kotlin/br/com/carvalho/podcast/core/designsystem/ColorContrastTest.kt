package br.com.carvalho.podcast.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

private const val TEXT = 4.5f
private const val GRAPHIC = 3f

/** WCAG 2.x contrast ratio between two opaque colors. */
internal fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

private class Pair(val name: String, val min: Float, val fg: Pick, val bg: Pick)
private typealias Pick = (ColorScheme, PodcastColors) -> Color

// The usage pairs listed in ADR 0001: text needs 4.5:1, icons, borders and shapes need 3:1.
private val pairs = listOf(
    Pair("onSurface / background", TEXT, { s, _ -> s.onSurface }, { s, _ -> s.background }),
    Pair("onSurface / surfaceContainerHighest", TEXT, { s, _ -> s.onSurface }, { s, _ -> s.surfaceContainerHighest }),
    Pair("onSurfaceVariant / background", TEXT, { s, _ -> s.onSurfaceVariant }, { s, _ -> s.background }),
    Pair("onSurfaceVariant / surfaceContainerHighest", TEXT, { s, _ -> s.onSurfaceVariant }, { s, _ -> s.surfaceContainerHighest }),
    Pair("onPrimary / primary", TEXT, { s, _ -> s.onPrimary }, { s, _ -> s.primary }),
    Pair("primary / background", GRAPHIC, { s, _ -> s.primary }, { s, _ -> s.background }),
    Pair("primary / surfaceContainerLowest", GRAPHIC, { s, _ -> s.primary }, { s, _ -> s.surfaceContainerLowest }),
    Pair("primary / surfaceContainer", GRAPHIC, { s, _ -> s.primary }, { s, _ -> s.surfaceContainer }),
    Pair("onPrimaryContainer / primaryContainer", TEXT, { s, _ -> s.onPrimaryContainer }, { s, _ -> s.primaryContainer }),
    Pair("onSecondary / secondary", TEXT, { s, _ -> s.onSecondary }, { s, _ -> s.secondary }),
    Pair("onSecondaryContainer / secondaryContainer", TEXT, { s, _ -> s.onSecondaryContainer }, { s, _ -> s.secondaryContainer }),
    Pair("onTertiary / tertiary", TEXT, { s, _ -> s.onTertiary }, { s, _ -> s.tertiary }),
    Pair("onTertiaryContainer / tertiaryContainer", TEXT, { s, _ -> s.onTertiaryContainer }, { s, _ -> s.tertiaryContainer }),
    Pair("onError / error", TEXT, { s, _ -> s.onError }, { s, _ -> s.error }),
    Pair("onErrorContainer / errorContainer", TEXT, { s, _ -> s.onErrorContainer }, { s, _ -> s.errorContainer }),
    Pair("error / background", TEXT, { s, _ -> s.error }, { s, _ -> s.background }),
    Pair("inverseOnSurface / inverseSurface", TEXT, { s, _ -> s.inverseOnSurface }, { s, _ -> s.inverseSurface }),
    Pair("accentText / background", TEXT, { _, p -> p.accentText }, { s, _ -> s.background }),
    Pair("accentText / surfaceContainerLowest", TEXT, { _, p -> p.accentText }, { s, _ -> s.surfaceContainerLowest }),
    Pair("accentText / surfaceContainer", TEXT, { _, p -> p.accentText }, { s, _ -> s.surfaceContainer }),
    Pair("onBrand / brand", TEXT, { _, p -> p.onBrand }, { _, p -> p.brand }),
    Pair("outline / background", GRAPHIC, { s, _ -> s.outline }, { s, _ -> s.background }),
    Pair("downloaded / background", GRAPHIC, { _, p -> p.downloaded }, { s, _ -> s.background }),
    Pair("downloaded / surfaceContainerLowest", GRAPHIC, { _, p -> p.downloaded }, { s, _ -> s.surfaceContainerLowest }),
    Pair("played / background", GRAPHIC, { _, p -> p.played }, { s, _ -> s.background }),
    Pair("tertiary / background", TEXT, { s, _ -> s.tertiary }, { s, _ -> s.background }),
)

class ColorContrastTest {

    @Test
    fun lightThemePairsMeetWcagAa() = assertPairs(LightColorScheme, LightPodcastColors)

    @Test
    fun darkThemePairsMeetWcagAa() = assertPairs(DarkColorScheme, DarkPodcastColors)

    @Test
    fun contrastMatchesWcagReferenceValues() {
        assertTrue(contrast(Color.Black, Color.White) in 20.9f..21.1f)
        assertTrue(contrast(Color.White, Color.White) in 0.99f..1.01f)
    }

    private fun assertPairs(scheme: ColorScheme, colors: PodcastColors) {
        val failures = pairs.mapNotNull { pair ->
            val ratio = contrast(pair.fg(scheme, colors), pair.bg(scheme, colors))
            if (ratio < pair.min) "${pair.name}: $ratio < ${pair.min}" else null
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
