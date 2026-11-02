package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * A store for the view models of what [key] names, kept while [key] stays and cleared as it changes (23.10). The
 * podcast and episode pages took one view model per podcast and per episode opened, with the podcast's loaded pages,
 * and kept them all in the activity until the app closed. Remembered where the selection lives, not inside the pane:
 * on a phone the podcast pane leaves the screen while one of its episodes is open, and coming back must find its view
 * model. The activity handles size, orientation and fold changes itself, so the store survives those.
 */
@Composable
internal fun rememberViewModelScope(key: Any?): ViewModelStoreOwner {
    val owner = remember(key) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    return owner
}

/** [content] takes its view models from [scope] instead of the activity. */
@Composable
internal fun WithViewModels(scope: ViewModelStoreOwner, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalViewModelStoreOwner provides scope, content = content)
