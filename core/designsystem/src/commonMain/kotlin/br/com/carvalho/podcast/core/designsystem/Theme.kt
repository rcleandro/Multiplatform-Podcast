package br.com.carvalho.podcast.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun PodcastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPodcastColors provides if (darkTheme) DarkPodcastColors else LightPodcastColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
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
}
