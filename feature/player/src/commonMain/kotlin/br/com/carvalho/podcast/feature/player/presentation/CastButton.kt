package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.runtime.Composable

/** Sends the episode to a Chromecast (18.17); only Android has one, AirPlay comes with phase 25. */
@Composable
internal expect fun CastButton()
