package br.com.carvalho.podcast

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.FlowPreview
import platform.UIKit.UIViewController

// The Swift app calls it by this name (ContentView.swift).
@Suppress("FunctionNaming")
@OptIn(FlowPreview::class, ExperimentalDecomposeApi::class)
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController {
        val lifecycle = remember { LifecycleRegistry() }
        // iOS has no back button: a swipe from the left edge drives the same back handler the navigation registers.
        val backDispatcher = remember { BackDispatcher() }
        val root = remember {
            RootComponent(DefaultComponentContext(lifecycle = lifecycle, backHandler = backDispatcher))
        }
        PredictiveBackGestureOverlay(backDispatcher = backDispatcher, backIcon = null, endEdgeEnabled = false) {
            App(root)
        }
    }
}
