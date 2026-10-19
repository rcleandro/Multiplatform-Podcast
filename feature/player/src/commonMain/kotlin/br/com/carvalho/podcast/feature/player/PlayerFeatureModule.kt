package br.com.carvalho.podcast.feature.player

import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.player.presentation.QueueViewModel
import br.com.carvalho.podcast.feature.player.presentation.TranscriptViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playerFeatureModule = module {
    viewModelOf(::PlayerViewModel)
    viewModelOf(::QueueViewModel)
    viewModelOf(::TranscriptViewModel)
}
