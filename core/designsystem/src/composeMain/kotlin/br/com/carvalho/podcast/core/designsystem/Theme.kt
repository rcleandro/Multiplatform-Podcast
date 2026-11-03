package br.com.carvalho.podcast.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

@Composable
fun PodcastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val family = onestFontFamily()
    val typography = remember(family) { podcastTypography(family) }
    val extraTypography = remember(family) { podcastExtraTypography(family) }

    CompositionLocalProvider(
        LocalPodcastColors provides if (darkTheme) DarkPodcastColors else LightPodcastColors,
        LocalPodcastTypography provides extraTypography,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = typography,
            shapes = Shapes,
            content = content
        )
    }
}

object PodcastTheme {
    val colors: PodcastColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPodcastColors.current

    val typography: PodcastTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalPodcastTypography.current
}
