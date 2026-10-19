package br.com.carvalho.podcast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.TranscriptLine
import br.com.carvalho.podcast.domain.model.lineAt
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.TranscriptSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The transcript of the episode playing (19.2): loaded when the episode changes, the line being said as it plays, a
 * tap that plays from a line, and a search that keeps only the lines with the words.
 */
class TranscriptViewModel(
    private val audioPlayer: AudioPlayer,
    private val transcriptSource: TranscriptSource,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics,
) : ViewModel() {
    private val loaded = MutableStateFlow(Loaded())
    private val query = MutableStateFlow("")

    val uiState: StateFlow<TranscriptUiState> = combine(
        loaded,
        query,
        audioPlayer.playerState,
    ) { loaded, query, player ->
        val found = if (query.isBlank()) {
            loaded.lines.indices.toList()
        } else {
            loaded.lines.indices.filter { loaded.lines[it].text.contains(query.trim(), ignoreCase = true) }
        }
        TranscriptUiState(
            lines = loaded.lines,
            shown = found,
            current = loaded.lines.lineAt(player.position),
            query = query,
            isLoading = loaded.isLoading,
            failed = loaded.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TranscriptUiState())

    init {
        audioPlayer.playerState.map { it.currentEpisode }
            .distinctUntilChanged { old, new -> old?.id == new?.id }
            .onEach { load(it) }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TranscriptIntent) {
        when (intent) {
            is TranscriptIntent.ChangeQuery -> query.value = intent.query
            is TranscriptIntent.PlayFrom -> {
                analytics.logEvent(AnalyticsEvent.PlayFromTranscript(searching = query.value.isNotBlank()))
                audioPlayer.seekTo(intent.line.startMs)
                if (!audioPlayer.playerState.value.isPlaying) audioPlayer.resume()
            }
            TranscriptIntent.Retry -> load(audioPlayer.playerState.value.currentEpisode)
        }
    }

    private fun load(episode: Episode?) {
        query.value = ""
        if (episode?.transcriptUrl == null) {
            loaded.value = Loaded()
            return
        }
        loaded.value = Loaded(isLoading = true)
        viewModelScope.launch(dispatchers.io) {
            val result = transcriptSource.transcript(episode)
            // Another episode may have started while this one loaded.
            if (audioPlayer.playerState.value.currentEpisode?.id != episode.id) return@launch
            loaded.update { Loaded(lines = result.getOrDefault(emptyList()), failed = result.isFailure) }
        }
    }

    private data class Loaded(
        val lines: List<TranscriptLine> = emptyList(),
        val isLoading: Boolean = false,
        val failed: Boolean = false,
    )
}

data class TranscriptUiState(
    val lines: List<TranscriptLine> = emptyList(),
    /** The indices of [lines] to show: all of them, or those with the searched words. */
    val shown: List<Int> = emptyList(),
    /** The line being said, or -1 before the first. */
    val current: Int = -1,
    val query: String = "",
    val isLoading: Boolean = false,
    val failed: Boolean = false,
)

sealed interface TranscriptIntent {
    data class ChangeQuery(val query: String) : TranscriptIntent
    data class PlayFrom(val line: TranscriptLine) : TranscriptIntent
    data object Retry : TranscriptIntent
}
