package br.com.carvalho.podcast.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.feature.discover.presentation.DiscoverViewModel
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailViewModel
import br.com.carvalho.podcast.feature.library.presentation.LibraryOrderViewModel
import br.com.carvalho.podcast.feature.library.presentation.LibraryViewModel
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
import br.com.carvalho.podcast.feature.search.presentation.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// What the SwiftUI screens use of the shared code (ADR 0015, decision 1): hand-written, with no SKIE or
// KMP-NativeCoroutines, since every view model has the same shape (a state, messages and onIntent).

/** Starts the shared code; iOSApp.swift calls it once, before the first screen. */
fun startShared() = initKoin()

/** What Swift holds to stop watching a flow; not `Cancellable`, which SwiftUI already has from Combine. */
fun interface Watching {
    fun cancel()
}

/** Calls [onEach] on the main thread with every value of this flow until the handle is cancelled. */
fun <T> Flow<T>.watch(onEach: (T) -> Unit): Watching = watch(MainScope(), onEach)

internal fun <T> Flow<T>.watch(scope: CoroutineScope, onEach: (T) -> Unit): Watching {
    val job = scope.launch { collect { onEach(it) } }
    return Watching { job.cancel() }
}

/**
 * The view models of one SwiftUI screen, in a store of their own that Swift clears when the screen goes: the same
 * lifetime `rememberViewModelScope` gives the Compose panes (23.10). Each screen adds its view model here as it moves.
 */
class ScreenViewModels {
    private val store = ViewModelStore()

    fun library(): LibraryViewModel = get()

    fun libraryOrder(): LibraryOrderViewModel = get()

    fun discover(): DiscoverViewModel = get()

    fun search(): SearchViewModel = get()

    fun podcast(podcastId: String): PodcastDetailViewModel = get { parametersOf(podcastId) }

    fun episode(episodeId: String): EpisodeDetailViewModel = get { parametersOf(episodeId) }

    fun player(): PlayerViewModel = get()

    fun clear() = store.clear()

    private inline fun <reified VM : ViewModel> get(noinline parameters: (() -> ParametersHolder)? = null): VM {
        val factory = viewModelFactory { initializer { KoinPlatform.getKoin().get<VM>(parameters = parameters) } }
        return ViewModelProvider.create(store, factory)[VM::class]
    }
}
