package br.com.carvalho.podcast.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App colors that Material 3 has no role for. Read them through [PodcastTheme.colors]. */
@Immutable
data class PodcastColors(
    /** Amber used as text or small icons; `primary` is too light for text on the light theme. */
    val accentText: Color,
    /** Bright brand amber for badges and the logo, always with [onBrand] on top. */
    val brand: Color,
    val onBrand: Color,
    val downloaded: Color,
    val played: Color,
)

internal val LightPodcastColors = PodcastColors(
    accentText = Color(Palette.AMBER_40),
    brand = Color(Palette.AMBER_70),
    onBrand = Color(Palette.NEUTRAL_10),
    downloaded = Color(Palette.GREEN_40),
    played = Color(Palette.NEUTRAL_55),
)

internal val DarkPodcastColors = PodcastColors(
    accentText = Color(Palette.AMBER_70),
    brand = Color(Palette.AMBER_70),
    onBrand = Color(Palette.NEUTRAL_10),
    downloaded = Color(Palette.GREEN_80),
    played = Color(Palette.NEUTRAL_50),
)

internal val LocalPodcastColors = staticCompositionLocalOf { LightPodcastColors }
