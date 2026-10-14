package br.com.carvalho.podcast.feature.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.core.observability.urlHost
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.error_open_link
import br.com.carvalho.podcast.core.ui.generated.resources.opml_imported
import br.com.carvalho.podcast.core.ui.generated.resources.opml_nothing_to_import
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.domain.usecase.ImportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.OpenPodcastLinkUseCase
import br.com.carvalho.podcast.domain.usecase.OpmlImport
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
import br.com.carvalho.podcast.domain.usecase.validFeedUrl
import br.com.carvalho.podcast.presentation.UiMessage
import br.com.carvalho.podcast.presentation.toMessage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DiscoverViewModel"

/**
 * "Add podcast": search the directory (18.1), or browse the most popular podcasts (18.3) and those of a category
 * (18.2), and subscribe with a tap; or add a podcast by its feed address, or by a link opened in the app (18.4). Also
 * the suggestions of an empty library.
 */
class DiscoverViewModel(
    private val searchPodcasts: SearchPodcastsUseCase,
    private val addPodcast: AddPodcastFromUrlUseCase,
    private val openLink: OpenPodcastLinkUseCase,
    private val importOpml: ImportOpmlUseCase,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    /** A podcast added by its address: the screen closes and the library shows it. */
    private val _addedByUrl = Channel<Unit>(Channel.CONFLATED)
    val addedByUrl: Flow<Unit> = _addedByUrl.receiveAsFlow()

    /** A link to a podcast the library already has: the app opens it there. */
    private val _openPodcast = Channel<String>(Channel.CONFLATED)
    val openPodcast: Flow<String> = _openPodcast.receiveAsFlow()

    private val searchRequests = MutableStateFlow(SearchRequest(term = "", category = null))

    /** The opening of the screen this state belongs to; null until the screen first opens. */
    private var session: Int? = null

    init {
        viewModelScope.launch(dispatchers.io) { searchAsTheUserTypes() }
    }

    /** Subscribes to an OPML file's feeds (18.13) and says how it went; the screen stays, as with "Subscribe". */
    private fun import(text: String?) {
        if (_uiState.value.importProgress != null) return
        viewModelScope.launch(dispatchers.io) {
            val result = text?.let {
                importOpml(it) { done, total -> _uiState.update { s -> s.copy(importProgress = done to total) } }
            } ?: OpmlImport(0, 0, 0)
            _uiState.update { it.copy(importProgress = null) }
            _messages.send(
                if (result == OpmlImport(0, 0, 0)) {
                    UiMessage(Res.string.opml_nothing_to_import)
                } else {
                    UiMessage(Res.string.opml_imported, listOf(result.added, result.known, result.failed))
                }
            )
        }
    }

    fun onIntent(intent: DiscoverIntent) {
        when (intent) {
            is DiscoverIntent.ChangeQuery -> {
                _uiState.update { it.copy(query = intent.query) }
                searchRequests.value = SearchRequest(intent.query, _uiState.value.category)
            }
            is DiscoverIntent.SelectCategory -> {
                _uiState.update { it.copy(category = intent.category) }
                intent.category?.let { analytics.logEvent(AnalyticsEvent.BrowseCategory(it.name.lowercase())) }
                searchRequests.value = SearchRequest(_uiState.value.query, intent.category)
            }
            DiscoverIntent.Retry -> searchRequests.value = SearchRequest(_uiState.value.query, _uiState.value.category)
            is DiscoverIntent.Subscribe -> subscribe(intent.result)
            DiscoverIntent.OpenUrlDialog -> _uiState.update { it.copy(isUrlDialogOpen = true) }
            DiscoverIntent.DismissUrlDialog -> _uiState.update { it.copy(isUrlDialogOpen = false, url = "") }
            is DiscoverIntent.ChangeUrl -> _uiState.update { it.copy(url = intent.url) }
            DiscoverIntent.ConfirmUrl -> addByUrl()
            is DiscoverIntent.ImportOpml -> import(intent.text)
            is DiscoverIntent.OpenLink -> openPodcastLink(intent.link)
            is DiscoverIntent.Open -> open(intent.session)
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun searchAsTheUserTypes() {
        searchRequests
            // Typing waits for a pause; a tapped category opens at once.
            .debounce { if (it.term.isBlank()) 0 else AppConfig.SEARCH_DEBOUNCE_MS }
            .collectLatest { request ->
                val term = request.term.trim()
                _uiState.update { it.copy(results = emptyList(), isSearching = true, searchFailed = false) }
                // A typed term wins over the category, whose chips only show while the field is empty; with neither,
                // the most popular podcasts of all categories (18.3).
                val found = if (term.isNotEmpty()) searchPodcasts(term) else searchPodcasts.top(request.category)
                found
                    .onSuccess { results ->
                        if (term.isNotEmpty()) analytics.logEvent(AnalyticsEvent.SearchDirectory(results.size))
                        _uiState.update { it.copy(results = results, isSearching = false) }
                    }
                    .onFailure { e ->
                        AppLogger.e(TAG, "Directory search failed", e)
                        _uiState.update { it.copy(results = emptyList(), isSearching = false, searchFailed = true) }
                    }
            }
    }

    private fun subscribe(result: DirectoryResult) {
        val feedUrl = result.podcast.feedUrl
        if (result.isSubscribed || feedUrl in _uiState.value.subscribing) return
        _uiState.update { it.copy(subscribing = it.subscribing + feedUrl) }
        viewModelScope.launch(dispatchers.io) {
            val added = add(feedUrl)
            _uiState.update { state ->
                state.copy(
                    subscribing = state.subscribing - feedUrl,
                    results = state.results.map { if (added) it.subscribedIf(feedUrl) else it },
                    linked = state.linked?.let { if (added) it.subscribedIf(feedUrl) else it },
                )
            }
        }
    }

    /**
     * The view model outlives the screen, which slides away over the tabs: a new opening starts with nothing typed,
     * no category and no link, as the first one did. The same opening again (a rotation, a fold) changes nothing.
     */
    private fun open(opening: Int) {
        val previous = session
        session = opening
        if (previous == null || previous == opening) return
        _uiState.value = DiscoverUiState()
        searchRequests.value = SearchRequest(term = "", category = null)
    }

    private fun DirectoryResult.subscribedIf(feedUrl: String) =
        if (podcast.feedUrl == feedUrl) copy(isSubscribed = true) else this

    /** Shows the podcast a link leads to, with "Subscribe"; one the library has opens there instead (18.4). */
    private fun openPodcastLink(link: String) {
        _uiState.update { it.copy(linked = null, isOpeningLink = true) }
        viewModelScope.launch(dispatchers.io) {
            openLink(link)
                .onSuccess { found ->
                    analytics.logEvent(AnalyticsEvent.OpenPodcastLink(urlHost(found.podcast.feedUrl)))
                    _uiState.update { it.copy(isOpeningLink = false) }
                    val libraryId = found.libraryId
                    if (libraryId != null) {
                        _openPodcast.send(libraryId)
                    } else {
                        _uiState.update { it.copy(linked = DirectoryResult(found.podcast, isSubscribed = false)) }
                    }
                }
                .onFailure { e ->
                    // Never the link itself: a private feed's address carries its access token.
                    AppLogger.e(TAG, "Failed to open a podcast link", e)
                    _uiState.update { it.copy(isOpeningLink = false) }
                    _messages.send(UiMessage(e.toMessage(fallback = Res.string.error_open_link)))
                }
        }
    }

    private fun addByUrl() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) return
        _uiState.update { it.copy(isUrlDialogOpen = false, isAddingUrl = true) }
        viewModelScope.launch(dispatchers.io) {
            val added = add(url)
            _uiState.update { it.copy(isAddingUrl = false, url = "") }
            if (added) _addedByUrl.send(Unit)
        }
    }

    /** Adds the feed and reports it; true when the library has it afterwards (a podcast already there counts). */
    private suspend fun add(input: String): Boolean {
        // Only the host: the typed text may lack the scheme, which the logger needs to spot (and cut) a URL.
        val host = urlHost(validFeedUrl(input) ?: input)
        analytics.logEvent(AnalyticsEvent.AddPodcastAttempt(host))
        AppLogger.i(TAG, "Adding podcast from host: $host")
        return addPodcast(input).fold(
            onSuccess = {
                analytics.logEvent(AnalyticsEvent.AddPodcastSuccess(host))
                true
            },
            onFailure = { e ->
                AppLogger.e(TAG, "Failed to add podcast from host: $host", e)
                analytics.logEvent(AnalyticsEvent.AddPodcastFailure(host, e))
                _messages.send(UiMessage(e.toMessage(fallback = Res.string.error_add_podcast)))
                e == AppError.AlreadyExists
            },
        )
    }

    /** Compared by identity, so "Try again" searches the same term again. */
    private class SearchRequest(val term: String, val category: PodcastCategory?)
}

data class DiscoverUiState(
    val query: String = "",
    /** The category whose chart shows while nothing is typed (18.2). */
    val category: PodcastCategory? = null,
    val results: List<DirectoryResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchFailed: Boolean = false,
    /** Feed addresses being subscribed to, each showing progress on its row. */
    val subscribing: Set<String> = emptySet(),
    val isUrlDialogOpen: Boolean = false,
    val url: String = "",
    val isAddingUrl: Boolean = false,
    /** The podcast of a link opened in the app (18.4), shown above the rest while nothing is typed. */
    val linked: DirectoryResult? = null,
    val isOpeningLink: Boolean = false,
    /** Feeds of an OPML file done out of how many, while it imports (18.13). */
    val importProgress: Pair<Int, Int>? = null,
)

sealed interface DiscoverIntent {
    data class ChangeQuery(val query: String) : DiscoverIntent
    data object Retry : DiscoverIntent
    data class SelectCategory(val category: PodcastCategory?) : DiscoverIntent
    data class Subscribe(val result: DirectoryResult) : DiscoverIntent
    data object OpenUrlDialog : DiscoverIntent
    data object DismissUrlDialog : DiscoverIntent
    data class ChangeUrl(val url: String) : DiscoverIntent
    data object ConfirmUrl : DiscoverIntent

    /** The text of the OPML file the user picked, or null when it could not be read (18.13). */
    data class ImportOpml(val text: String?) : DiscoverIntent
    data class OpenLink(val link: String) : DiscoverIntent

    /** The screen opened ([session] counts the openings): a new one starts clean. */
    data class Open(val session: Int) : DiscoverIntent
}
