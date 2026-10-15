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
@Suppress("TooManyFunctions") // one function per way of moving around the app
class RootComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    private val restoredState = stateKeeper.consume(STATE_KEY, NavigationState.serializer())
    private val _state = MutableValue(restoredState ?: NavigationState())
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
            it.copy(
                selectedTab = tab,
                stacks = stacks,
                isPlayerOpen = false,
                isQueueOpen = false,
                isOrganizingLibrary = false,
                isAddingPodcast = false,
                isShowingSettings = false,
                isShowingStats = false,
            )
        }
    }

    /**
     * Opened without network: starts on Episodes, whose downloaded filter is what can be heard (ADR 0005). A screen
     * restored or already left behind stays where it is.
     */
    fun onOpenedOffline() {
        if (restoredState != null || _state.value != NavigationState()) return
        AppLogger.d(TAG, "Opened offline")
        _state.update { it.copy(selectedTab = Tab.Episodes) }
    }

    fun onPlayerClicked() {
        _state.update { it.copy(isPlayerOpen = true) }
    }

    /** Opens the queue (18.6) over the player or the tabs, from the player and the mini player. */
    fun onQueueClicked() {
        _state.update { it.copy(isQueueOpen = true) }
    }

    /** Opens "Organize library" over the tabs, the only place that arranges the custom order. */
    fun onOrganizeLibrary() {
        _state.update { it.copy(isOrganizingLibrary = true) }
    }

    /** Opens "Add podcast" (directory search and feed address) over the tabs, from the library's "+" (18.1). */
    fun onAddPodcast() {
        _state.update { it.copy(isAddingPodcast = true, addPodcastSession = it.addPodcastSession + 1) }
    }

    /**
     * A podcast link opened in the app (18.4): "Add podcast" shows it over the tabs, with whatever covered them closed.
     * [NavigationState.podcastLink] holds it until the screen takes it.
     */
    fun onPodcastLink(link: String) {
        AppLogger.d(TAG, "Podcast link opened")
        _state.update {
            it.copy(
                isPlayerOpen = false,
                isOrganizingLibrary = false,
                isShowingSettings = false,
                isShowingStats = false,
                isAddingPodcast = true,
                addPodcastSession = it.addPodcastSession + 1,
                podcastLink = link,
            )
        }
    }

    fun onPodcastLinkHandled() {
        _state.update { it.copy(podcastLink = null) }
    }

    /** "Add podcast" with podcasts like the library's [podcastId] above the chart (19.6), from that podcast's page. */
    fun onShowSimilar(podcastId: String) {
        _state.update {
            it.copy(isAddingPodcast = true, addPodcastSession = it.addPodcastSession + 1, similarTo = podcastId)
        }
    }

    fun onSimilarHandled() {
        _state.update { it.copy(similarTo = null) }
    }

    /** Opens Settings over the tabs, from the gear in the library's bar (18.5, ADR 0005). */
    fun onOpenSettings() {
        _state.update { it.copy(isShowingSettings = true) }
    }

    /** Opens the listening stats over Settings (19.5); going back returns to Settings. */
    fun onOpenListeningStats() {
        _state.update { it.copy(isShowingStats = true) }
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

    /**
     * Closes what covers the tabs (queue, player, "Organize", "Add podcast", Settings), else pops the tab or goes
     * home.
     */
    fun onBackClicked() {
        _state.update {
            when {
                it.isQueueOpen -> it.copy(isQueueOpen = false)
                it.isPlayerOpen -> it.copy(isPlayerOpen = false)
                it.isOrganizingLibrary -> it.copy(isOrganizingLibrary = false)
                it.isAddingPodcast -> it.copy(isAddingPodcast = false)
                it.isShowingStats -> it.copy(isShowingStats = false)
                it.isShowingSettings -> it.copy(isShowingSettings = false)
                it.currentStack.isNotEmpty() ->
                    it.copy(stacks = it.stacks + (it.selectedTab to it.currentStack.dropLast(1)))
                else -> it.copy(selectedTab = Tab.Library)
            }
        }
    }

    private fun setStack(stack: List<Detail>) {
        // From "Add podcast" too: a link to a podcast the library has opens it (18.4).
        _state.update {
            it.copy(stacks = it.stacks + (it.selectedTab to stack), isPlayerOpen = false, isAddingPodcast = false)
        }
    }
}

@Serializable
/** The two tabs of ADR 0005. */
enum class Tab { Library, Episodes }

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
    /** The queue (18.6), over the player when it was opened from there. */
    val isQueueOpen: Boolean = false,
    val isOrganizingLibrary: Boolean = false,
    val isAddingPodcast: Boolean = false,
    val isShowingSettings: Boolean = false,
    /** The listening stats (19.5), over Settings. */
    val isShowingStats: Boolean = false,
    /**
     * Counts the times "Add podcast" was opened: each opening starts clean, while a rotation or a fold, which keep
     * this number, keep what was typed.
     */
    val addPodcastSession: Int = 0,
    /** A podcast link waiting for "Add podcast" to show it (18.4). */
    val podcastLink: String? = null,
    /** A podcast whose similar ones "Add podcast" shows (19.6), held until the screen takes it. */
    val similarTo: String? = null,
) {
    val currentStack: List<Detail> get() = stacks[selectedTab].orEmpty()
    val podcast: Detail.Podcast? get() = currentStack.filterIsInstance<Detail.Podcast>().lastOrNull()
    val episode: Detail.Episode? get() = currentStack.lastOrNull() as? Detail.Episode
    val canGoBack: Boolean
        get() = isPlayerOpen || isQueueOpen || isOrganizingLibrary || isAddingPodcast || isShowingSettings ||
            isShowingStats || currentStack.isNotEmpty() || selectedTab != Tab.Library
}
