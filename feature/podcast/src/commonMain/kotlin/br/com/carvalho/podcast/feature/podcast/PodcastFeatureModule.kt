package br.com.carvalho.podcast.feature.podcast

import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val podcastFeatureModule = module {
    viewModelOf(::PodcastDetailViewModel)
}
