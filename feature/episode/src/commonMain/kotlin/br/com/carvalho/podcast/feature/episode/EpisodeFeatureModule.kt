package br.com.carvalho.podcast.feature.episode

import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val episodeFeatureModule = module {
    viewModelOf(::EpisodeDetailViewModel)
}
