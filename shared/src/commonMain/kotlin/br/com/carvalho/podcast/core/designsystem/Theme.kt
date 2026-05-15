package br.com.carvalho.podcast.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray800,
    onPrimaryContainer = White,
    secondary = Gray400,
    onSecondary = Black,
    secondaryContainer = Gray600,
    onSecondaryContainer = White,
    tertiary = Gray300,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,
    outline = Gray600,
    inverseOnSurface = Black,
    inverseSurface = White,
    inversePrimary = Black,
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray200,
    onPrimaryContainer = Black,
    secondary = Gray600,
    onSecondary = White,
    secondaryContainer = Gray300,
    onSecondaryContainer = Black,
    tertiary = Gray400,
    onTertiary = White,
    background = White,
    onBackground = Black,
    surface = Gray200,
    onSurface = Black,
    surfaceVariant = Gray300,
    onSurfaceVariant = Gray600,
    outline = Gray400,
    inverseOnSurface = White,
    inverseSurface = Black,
    inversePrimary = White,
)

@Composable
fun PodcastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
