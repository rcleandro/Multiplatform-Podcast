package br.com.carvalho.podcast.presentation

import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
import br.com.carvalho.podcast.feature.search.presentation.SearchViewModel
import br.com.carvalho.podcast.presentation.format.DateGroup
import br.com.carvalho.podcast.presentation.format.RelativeTime
import br.com.carvalho.podcast.presentation.format.dateGroup
import br.com.carvalho.podcast.presentation.format.relativeTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// What the SwiftUI lists read of the shared code (ADR 0015): pages, what the rows show of the player, and the date and
// time words the Compose screens use.

/**
 * Whether an episode is in the player, for the shell to show the mini player. It changes only when that does, while
 * the player's state moves every 500 ms as it plays (23.2), so the shell does not redraw with the position.
 */
fun PlayerViewModel.hasEpisode(): StateFlow<Boolean> = playerState.map { it.currentEpisode != null }
    .stateIn(viewModelScope, SharingStarted.Eagerly, playerState.value.currentEpisode != null)

/** How long ago [timestampMs] was, now and in the device's time zone, as the Compose screens count it. */
fun relativeTimeNow(timestampMs: Long): RelativeTime? = relativeTime(timestampMs, getCurrentTimestamp())

/** The podcast's episodes a page at a time, for the SwiftUI list; one per screen, kept while its view model lives. */
fun PodcastDetailViewModel.episodePages(): PagedItems<Episode> = PagedItems(pagedEpisodes, viewModelScope)

/**
 * What the episode rows show of the player (which episode, playing or loading), without the position that moves every
 * 500 ms, so the podcast's list does not redraw as it plays (23.3).
 */
fun PodcastDetailViewModel.rowPlayback(): StateFlow<PlayerState> = playerState.forRows(viewModelScope)

/** The same for the Episodes tab (25.6). */
fun SearchViewModel.rowPlayback(): StateFlow<PlayerState> = playerState.forRows(viewModelScope)

/** The Episodes tab's results a page at a time, for the SwiftUI list. */
fun SearchViewModel.resultPages(): PagedItems<Episode> = PagedItems(pagedResults, viewModelScope)

/** Which date header [timestampMs] goes under now, as the Compose Episodes list groups them. */
fun dateGroupNow(timestampMs: Long): DateGroup = dateGroup(timestampMs, getCurrentTimestamp())

private fun StateFlow<PlayerState>.forRows(scope: CoroutineScope): StateFlow<PlayerState> = map { it.forRows() }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, value.forRows())
