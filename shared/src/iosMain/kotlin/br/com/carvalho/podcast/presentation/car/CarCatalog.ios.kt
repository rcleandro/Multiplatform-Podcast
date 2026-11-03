package br.com.carvalho.podcast.presentation.car

import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import org.koin.mp.KoinPlatform

/** The [CarCatalog] CarPlaySceneDelegate.swift (25.13) reads, over the app's library and player. */
fun carCatalog(): CarCatalog {
    val koin = KoinPlatform.getKoin()
    return CarCatalog(koin.get<PodcastRepository>(), koin.get<AudioPlayer>(), koin.get<ExplicitContent>())
}
