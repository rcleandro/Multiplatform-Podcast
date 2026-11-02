package br.com.carvalho.podcast.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertTrue

private val roles: Map<String, (ColorScheme) -> Color> = mapOf(
    "primary" to { it.primary }, "onPrimary" to { it.onPrimary },
    "primaryContainer" to { it.primaryContainer }, "onPrimaryContainer" to { it.onPrimaryContainer },
    "inversePrimary" to { it.inversePrimary },
    "secondary" to { it.secondary }, "onSecondary" to { it.onSecondary },
    "secondaryContainer" to { it.secondaryContainer }, "onSecondaryContainer" to { it.onSecondaryContainer },
    "tertiary" to { it.tertiary }, "onTertiary" to { it.onTertiary },
    "tertiaryContainer" to { it.tertiaryContainer }, "onTertiaryContainer" to { it.onTertiaryContainer },
    "background" to { it.background }, "onBackground" to { it.onBackground },
    "surface" to { it.surface }, "onSurface" to { it.onSurface },
    "surfaceVariant" to { it.surfaceVariant }, "onSurfaceVariant" to { it.onSurfaceVariant },
    "surfaceTint" to { it.surfaceTint },
    "inverseSurface" to { it.inverseSurface }, "inverseOnSurface" to { it.inverseOnSurface },
    "error" to { it.error }, "onError" to { it.onError },
    "errorContainer" to { it.errorContainer }, "onErrorContainer" to { it.onErrorContainer },
    "outline" to { it.outline }, "outlineVariant" to { it.outlineVariant }, "scrim" to { it.scrim },
    "surfaceBright" to { it.surfaceBright }, "surfaceDim" to { it.surfaceDim },
    "surfaceContainer" to { it.surfaceContainer }, "surfaceContainerHigh" to { it.surfaceContainerHigh },
    "surfaceContainerHighest" to { it.surfaceContainerHighest }, "surfaceContainerLow" to { it.surfaceContainerLow },
    "surfaceContainerLowest" to { it.surfaceContainerLowest },
    "primaryFixed" to { it.primaryFixed }, "primaryFixedDim" to { it.primaryFixedDim },
    "onPrimaryFixed" to { it.onPrimaryFixed }, "onPrimaryFixedVariant" to { it.onPrimaryFixedVariant },
    "secondaryFixed" to { it.secondaryFixed }, "secondaryFixedDim" to { it.secondaryFixedDim },
    "onSecondaryFixed" to { it.onSecondaryFixed }, "onSecondaryFixedVariant" to { it.onSecondaryFixedVariant },
    "tertiaryFixed" to { it.tertiaryFixed }, "tertiaryFixedDim" to { it.tertiaryFixedDim },
    "onTertiaryFixed" to { it.onTertiaryFixed }, "onTertiaryFixedVariant" to { it.onTertiaryFixedVariant },
)

// Roles whose value happens to equal the Material 3 baseline on purpose: pure white or black,
// and the standard M3 error reds that ADR 0001 kept for the light theme.
private val lightSameAsBaseline = setOf(
    "onSecondary", "onTertiary", "surfaceContainerLowest", "scrim",
    "error", "onError", "errorContainer", "onErrorContainer",
)
private val darkSameAsBaseline = setOf("scrim")

/** Guards against roles silently falling back to the baseline purple of Material 3. */
class ColorSchemeCompletenessTest {

    @Test
    fun lightSchemeDefinesEveryRole() =
        assertNoBaselineRole(LightColorScheme, lightColorScheme(), lightSameAsBaseline)

    @Test
    fun darkSchemeDefinesEveryRole() =
        assertNoBaselineRole(DarkColorScheme, darkColorScheme(), darkSameAsBaseline)

    private fun assertNoBaselineRole(scheme: ColorScheme, baseline: ColorScheme, allowed: Set<String>) {
        val fallbacks = roles.filter { (name, pick) -> name !in allowed && pick(scheme) == pick(baseline) }.keys
        assertTrue(fallbacks.isEmpty(), "Roles still using the Material 3 baseline: $fallbacks")
    }
}
