package br.com.carvalho.podcast

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.presentation.navigation.RootComponentImpl
import br.com.carvalho.podcast.presentation.navigation.RootContent

@Composable
fun App(root: RootComponentImpl) {
    PodcastTheme {
        RootContent(root)
    }
}
