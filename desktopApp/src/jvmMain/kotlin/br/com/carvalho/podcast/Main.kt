package br.com.carvalho.podcast

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.presentation.navigation.RootComponentImpl
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import androidx.compose.runtime.remember

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Podcast",
        ) {
            val lifecycle = remember { LifecycleRegistry() }
            val root = remember { RootComponentImpl(DefaultComponentContext(lifecycle = lifecycle)) }
            App(root)
        }
    }
}
