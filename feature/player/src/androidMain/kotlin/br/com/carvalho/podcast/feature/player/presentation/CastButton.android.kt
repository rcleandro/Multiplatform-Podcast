package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.media3.cast.MediaRouteButton
import androidx.media3.common.util.UnstableApi

/** Media3's button: the system's output picker where there is one, Cast's own dialog otherwise. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal actual fun CastButton() = MediaRouteButton()
