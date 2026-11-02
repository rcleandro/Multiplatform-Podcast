package br.com.carvalho.podcast.presentation

import br.com.carvalho.podcast.feature.player.presentation.VideoSurface

/** The player's video on this platform (19.10); `null` where episodes play only their sound. */
internal expect val platformVideoSurface: VideoSurface?
