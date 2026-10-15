package br.com.carvalho.podcast.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.ListeningStats
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The listening stats (19.5), as they grow while something plays. */
class ListeningStatsViewModel(playerRepository: PlayerRepository) : ViewModel() {
    val stats: StateFlow<ListeningStats> = playerRepository.listeningStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ListeningStats())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
