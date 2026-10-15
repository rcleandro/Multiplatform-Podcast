package br.com.carvalho.podcast

import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.MutableStateFlow
import platform.UIKit.UIViewController

/** A link iOS opened the app with, until the navigation takes it; kept, since one may arrive before the screen. */
private val podcastLinks = MutableStateFlow<String?>(null)

/** A podcast link (`pcast://…`, `podcast://…`) the app was opened with (18.4); called by iOSApp.swift. */
fun openPodcastLink(url: String) {
    podcastLinks.value = url
}

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
        LaunchedEffect(root) {
            podcastLinks.collect { link ->
                if (link != null) {
                    root.onPodcastLink(link)
                    podcastLinks.value = null
                }
            }
        }
        PredictiveBackGestureOverlay(backDispatcher = backDispatcher, backIcon = null, endEdgeEnabled = false) {
            App(root)
        }
    }
}
