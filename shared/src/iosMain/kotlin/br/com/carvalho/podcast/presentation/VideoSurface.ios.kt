package br.com.carvalho.podcast.presentation

import br.com.carvalho.podcast.core.player.IosPlatformPlayer
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.feature.player.presentation.VideoSurface
import org.koin.mp.KoinPlatform
import platform.AVFoundation.AVPlayer

// The Compose player shows no video on iOS: the SwiftUI player does, in an AVPlayerLayer with picture-in-picture.
internal actual val platformVideoSurface: VideoSurface? = null

/** The AVPlayer playing the episodes, which the SwiftUI player draws for a video episode (ADR 0010). */
fun videoPlayer(): AVPlayer = (KoinPlatform.getKoin().get<PlatformPlayer>() as IosPlatformPlayer).player
