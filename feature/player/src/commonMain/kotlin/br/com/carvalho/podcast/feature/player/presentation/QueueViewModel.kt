package br.com.carvalho.podcast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * The queue screen (18.6): the episode playing and what comes next. The player keeps the episodes as they were when
 * queued, so each one is read again from the library to show where it stopped and whether it is downloaded.
 */
class QueueViewModel(
    private val audioPlayer: AudioPlayer,
    private val playEpisode: PlayEpisodeUseCase,
    private val podcastRepository: PodcastRepository,
    private val downloader: EpisodeDownloader,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val isConfirmingClear = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<QueueUiState> = combine(
        audioPlayer.playerState,
        downloader.activeDownloads,
        isConfirmingClear,
    ) { player, downloads, confirming -> Triple(player, downloads, confirming) }
        .mapLatest { (player, downloads, confirming) ->
            QueueUiState(
                current = player.currentEpisode?.let { fresh(it) },
                upNext = player.queue.map { fresh(it) },
                isPlaying = player.isPlaying,
                isBuffering = player.isBuffering,
                downloads = downloads,
                isConfirmingClear = confirming,
            )
        }
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), QueueUiState())

    fun onIntent(intent: QueueIntent) {
        when (intent) {
            is QueueIntent.Play -> onIo { playEpisode(intent.episode) }
            is QueueIntent.Remove -> setQueue { it.filter { episode -> episode.id != intent.episode.id } }
            is QueueIntent.Restore -> setQueue {
                it.toMutableList().apply { add(intent.index.coerceIn(0, size), intent.episode) }
            }
            QueueIntent.RequestClear -> isConfirmingClear.value = true
            QueueIntent.DismissClear -> isConfirmingClear.value = false
            QueueIntent.ConfirmClear -> {
                isConfirmingClear.value = false
                audioPlayer.setQueue(emptyList())
            }
            is QueueIntent.Download -> onIo { downloader.download(intent.episode) }
            is QueueIntent.CancelDownload -> onIo { downloader.cancel(intent.episode.id) }
            is QueueIntent.DeleteDownload -> onIo { downloader.delete(intent.episode.id) }
        }
    }

    private fun onIo(block: suspend () -> Unit) {
        viewModelScope.launch(dispatchers.io) { block() }
    }

    private fun setQueue(change: (List<Episode>) -> List<Episode>) =
        audioPlayer.setQueue(change(audioPlayer.playerState.value.queue))

    private suspend fun fresh(episode: Episode): Episode =
        runCatching { podcastRepository.getEpisodeById(episode.id) }.getOrNull() ?: episode
}

data class QueueUiState(
    val current: Episode? = null,
    val upNext: List<Episode> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val downloads: Map<String, DownloadStatus> = emptyMap(),
    val isConfirmingClear: Boolean = false,
)

sealed interface QueueIntent {
    /** Plays an episode of the queue, or pauses and resumes the current one. */
    data class Play(val episode: Episode) : QueueIntent
    data class Remove(val episode: Episode) : QueueIntent

    /** "Undo" after a removal: the episode goes back where it was. */
    data class Restore(val episode: Episode, val index: Int) : QueueIntent
    data object RequestClear : QueueIntent
    data object DismissClear : QueueIntent
    data object ConfirmClear : QueueIntent
    data class Download(val episode: Episode) : QueueIntent
    data class CancelDownload(val episode: Episode) : QueueIntent
    data class DeleteDownload(val episode: Episode) : QueueIntent
}
