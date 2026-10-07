package br.com.carvalho.podcast.presentation.navigation

import br.com.carvalho.podcast.core.util.AppLogger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackCallback
import kotlinx.serialization.Serializable

private const val TAG = "RootComponent"
private const val STATE_KEY = "navigation"

/**
 * Navigation of the whole app: one tab selected at a time, a stack of detail screens per tab, and the player
 * opened over everything. The state is saved across process death and drives the system back button.
 */
class RootComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    private val _state = MutableValue(stateKeeper.consume(STATE_KEY, NavigationState.serializer()) ?: NavigationState())
    val state: Value<NavigationState> = _state

    private val backCallback = BackCallback(isEnabled = _state.value.canGoBack) { onBackClicked() }

    init {
        stateKeeper.register(STATE_KEY, NavigationState.serializer()) { _state.value }
        backHandler.register(backCallback)
        _state.subscribe { backCallback.isEnabled = it.canGoBack }
    }

    /** Selects [tab]; selecting the tab already shown goes back to its root. The player closes either way. */
    fun onTabClicked(tab: Tab) {
        AppLogger.d(TAG, "Tab clicked: $tab")
        _state.update {
            val stacks = if (tab == it.selectedTab && !it.isPlayerOpen) it.stacks - tab else it.stacks
            it.copy(selectedTab = tab, stacks = stacks, isPlayerOpen = false, isOrganizingLibrary = false)
        }
    }

    fun onPlayerClicked() {
        _state.update { it.copy(isPlayerOpen = true) }
    }

    /** Opens "Organize library" over the tabs, the only place that arranges the custom order. */
    fun onOrganizeLibrary() {
        _state.update { it.copy(isOrganizingLibrary = true) }
    }

    fun onPodcastSelected(podcastId: String) {
        AppLogger.d(TAG, "Podcast selected")
        setStack(listOf(Detail.Podcast(podcastId)))
    }

    /** Opens the episode over its podcast, so going back shows the podcast. */
    fun onEpisodeSelected(episodeId: String, podcastId: String) {
        AppLogger.d(TAG, "Episode selected")
        setStack(listOf(Detail.Podcast(podcastId), Detail.Episode(episodeId)))
    }

    /** Closes the player or "Organize library", else pops the current tab, else returns to the library. */
    fun onBackClicked() {
        _state.update {
            when {
                it.isPlayerOpen -> it.copy(isPlayerOpen = false)
                it.isOrganizingLibrary -> it.copy(isOrganizingLibrary = false)
                it.currentStack.isNotEmpty() ->
                    it.copy(stacks = it.stacks + (it.selectedTab to it.currentStack.dropLast(1)))
                else -> it.copy(selectedTab = Tab.Library)
            }
        }
    }

    private fun setStack(stack: List<Detail>) {
        _state.update { it.copy(stacks = it.stacks + (it.selectedTab to stack), isPlayerOpen = false) }
    }
}

@Serializable
enum class Tab { Library, Search, Downloads }

@Serializable
sealed interface Detail {
    @Serializable
    data class Podcast(val podcastId: String) : Detail

    @Serializable
    data class Episode(val episodeId: String) : Detail
}

@Serializable
data class NavigationState(
    val selectedTab: Tab = Tab.Library,
    val stacks: Map<Tab, List<Detail>> = emptyMap(),
    val isPlayerOpen: Boolean = false,
    val isOrganizingLibrary: Boolean = false,
) {
    val currentStack: List<Detail> get() = stacks[selectedTab].orEmpty()
    val podcast: Detail.Podcast? get() = currentStack.filterIsInstance<Detail.Podcast>().lastOrNull()
    val episode: Detail.Episode? get() = currentStack.lastOrNull() as? Detail.Episode
    val canGoBack: Boolean
        get() = isPlayerOpen || isOrganizingLibrary || currentStack.isNotEmpty() || selectedTab != Tab.Library
}
