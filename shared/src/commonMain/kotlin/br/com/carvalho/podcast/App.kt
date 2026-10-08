package br.com.carvalho.podcast

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.image.createImageLoader
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import br.com.carvalho.podcast.presentation.navigation.RootContent
import coil3.compose.setSingletonImageLoaderFactory

/** [keyboardShortcuts] on a computer, where a keyboard is the rule (21.7). */
@Composable
fun App(root: RootComponent, keyboardShortcuts: Boolean = false) {
    setSingletonImageLoaderFactory { context ->
        createImageLoader(context)
    }

    PodcastTheme {
        RootContent(root, keyboardShortcuts)
    }
}
