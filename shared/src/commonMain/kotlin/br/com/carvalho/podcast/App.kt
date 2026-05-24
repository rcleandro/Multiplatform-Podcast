package br.com.carvalho.podcast

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.presentation.navigation.RootComponentImpl
import br.com.carvalho.podcast.presentation.navigation.RootContent
import br.com.carvalho.podcast.core.image.createImageLoader
import coil3.compose.setSingletonImageLoaderFactory

@Composable
fun App(root: RootComponentImpl) {
    setSingletonImageLoaderFactory { context ->
        createImageLoader(context)
    }

    PodcastTheme {
        RootContent(root)
    }
}
