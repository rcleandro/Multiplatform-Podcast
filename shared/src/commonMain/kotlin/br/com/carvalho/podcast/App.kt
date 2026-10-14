package br.com.carvalho.podcast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.image.createImageLoader
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.presentation.ProvideAppLanguage
import br.com.carvalho.podcast.presentation.ProvideLayoutDirection
import br.com.carvalho.podcast.presentation.isDark
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import br.com.carvalho.podcast.presentation.navigation.RootContent
import coil3.compose.setSingletonImageLoaderFactory
import org.koin.compose.koinInject

/** [keyboardShortcuts] on a computer, where a keyboard is the rule (21.7). */
@Composable
fun App(root: RootComponent, keyboardShortcuts: Boolean = false) {
    setSingletonImageLoaderFactory { context ->
        createImageLoader(context)
    }

    // The theme chosen in Settings (18.5), or the system's.
    val preferences = koinInject<PreferencesRepository>()
    val themeMode by preferences.themeMode.collectAsState()
    val language by preferences.appLanguage.collectAsState()
    ProvideAppLanguage(language) {
        ProvideLayoutDirection {
            PodcastTheme(darkTheme = themeMode.isDark()) {
                RootContent(root, keyboardShortcuts)
            }
        }
    }
}
