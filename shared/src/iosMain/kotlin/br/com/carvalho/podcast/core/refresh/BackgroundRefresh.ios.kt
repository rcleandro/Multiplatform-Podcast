package br.com.carvalho.podcast.core.refresh

import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.BackgroundRefresh
import br.com.carvalho.podcast.domain.usecase.RefreshInBackgroundUseCase
import org.koin.mp.KoinPlatform

// What the BGAppRefreshTask in BackgroundRefresh.swift (25.11) asks of the shared code, as the Android RefreshWorker
// does (18.18): one look at every feed, and how often Settings want it.

/** One look at every feed, the same as pulling the library down; says what to notify about. */
suspend fun refreshInBackground(): BackgroundRefresh = KoinPlatform.getKoin().get<RefreshInBackgroundUseCase>()()

/** Hours between looks as Settings say; 0 is never. */
fun refreshIntervalHours(): Int = KoinPlatform.getKoin().get<PreferencesRepository>().refreshIntervalHours.value
