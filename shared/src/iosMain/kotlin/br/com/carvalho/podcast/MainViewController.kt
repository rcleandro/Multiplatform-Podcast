package br.com.carvalho.podcast

import androidx.compose.ui.window.ComposeUIViewController
import br.com.carvalho.podcast.presentation.navigation.RootComponentImpl
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry

import androidx.compose.runtime.remember
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
fun MainViewController() = ComposeUIViewController {
    val lifecycle = remember { LifecycleRegistry() }
    val root = remember { RootComponentImpl(DefaultComponentContext(lifecycle = lifecycle)) }
    App(root)
}
