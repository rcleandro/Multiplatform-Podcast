package br.com.carvalho.podcast

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.carvalho.podcast.presentation.navigation.RootComponentImpl
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        val lifecycle = remember { LifecycleRegistry() }
        val root = remember { RootComponentImpl(DefaultComponentContext(lifecycle = lifecycle)) }
        App(root)
    }
}
