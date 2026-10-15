package br.com.carvalho.podcast.feature.discover

import br.com.carvalho.podcast.domain.usecase.OpenPodcastLinkUseCase
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
import br.com.carvalho.podcast.feature.discover.presentation.DiscoverViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val discoverFeatureModule = module {
    factoryOf(::SearchPodcastsUseCase)
    factoryOf(::OpenPodcastLinkUseCase)
    viewModelOf(::DiscoverViewModel)
}
