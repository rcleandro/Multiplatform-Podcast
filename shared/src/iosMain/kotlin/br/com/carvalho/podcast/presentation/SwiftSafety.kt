package br.com.carvalho.podcast.presentation

import br.com.carvalho.podcast.domain.model.ChildSafetyState
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import kotlinx.coroutines.flow.StateFlow
import org.koin.mp.KoinPlatform

// What the SwiftUI screens need of Brazil's ECA Digital (ADR 0012): whether the app is there, and hiding what is
// reported (26.7), as the Compose screens do through the same preferences.

fun childSafety(): StateFlow<ChildSafetyState> = KoinPlatform.getKoin().get<ChildSafety>().state

fun hidePodcast(podcastId: String) = KoinPlatform.getKoin().get<PreferencesRepository>().hidePodcast(podcastId)

fun hideEpisode(episodeId: String) = KoinPlatform.getKoin().get<PreferencesRepository>().hideEpisode(episodeId)
