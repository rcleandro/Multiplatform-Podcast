package br.com.carvalho.podcast.presentation.navigation

import br.com.carvalho.podcast.core.util.NetworkMonitor
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform

/**
 * The app's navigation for the SwiftUI shell (ADR 0015, decision 2): the same [RootComponent] the Compose app uses,
 * which SwiftUI only draws. [saved] is what [save] returned before the system ended the process, kept by the scene, so
 * the app comes back where it was; the Compose app on iOS never had that.
 */
class SwiftNavigation(saved: String?) {
    private val stateKeeper = StateKeeperDispatcher(saved?.let(::restore))
    private val lifecycle = LifecycleRegistry().apply { resume() }

    val root = RootComponent(DefaultComponentContext(lifecycle = lifecycle, stateKeeper = stateKeeper))

    init {
        // Opened without network: start on Episodes, whose downloaded filter is what can be heard (ADR 0005).
        MainScope().launch {
            if (!KoinPlatform.getKoin().get<NetworkMonitor>().isOnline()) root.onOpenedOffline()
        }
    }

    fun save(): String = json.encodeToString(SerializableContainer.serializer(), stateKeeper.save())

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        // A state written by an older version may no longer read; the app then starts on the library.
        fun restore(saved: String): SerializableContainer? =
            runCatching { json.decodeFromString(SerializableContainer.serializer(), saved) }.getOrNull()
    }
}
