package br.com.carvalho.podcast.presentation.widget

import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.flow.Flow
import org.koin.mp.KoinPlatform

// What WidgetSync.swift and the widget's App Intents (25.12) ask of the shared code, as the Android PlayerWidget's
// actions do (21.9): the state to show, and the buttons, run in the app's process on the one player.

/** What the widget shows, from the app's player and library. */
fun playerWidgetState(): Flow<PlayerWidgetState> {
    val koin = KoinPlatform.getKoin()
    return playerWidgetState(
        koin.get<AudioPlayer>().playerState,
        koin.get<PodcastRepository>().getInProgressEpisodes(),
        koin.get<ExplicitContent>().rule,
    )
}

fun widgetPlayPause() {
    val player = KoinPlatform.getKoin().get<AudioPlayer>()
    if (player.playerState.value.isPlaying) player.pause() else player.resume()
}

fun widgetSkipBackward() {
    val player = KoinPlatform.getKoin().get<AudioPlayer>()
    player.skipBackward(player.playerState.value.skipIntervals.backwardSeconds)
}

fun widgetSkipForward() {
    val player = KoinPlatform.getKoin().get<AudioPlayer>()
    player.skipForward(player.playerState.value.skipIntervals.forwardSeconds)
}

/** Plays a started episode from the widget's list. */
suspend fun widgetPlayEpisode(id: String) {
    val koin = KoinPlatform.getKoin()
    koin.get<PodcastRepository>().getEpisodeById(id)?.let { koin.get<PlayEpisodeUseCase>()(it) }
}
