package br.com.carvalho.podcast.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.domain.model.ThemeMode

/** Whether the app draws dark: the user's choice, or the system's when they left it to the system. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
