package br.com.carvalho.podcast.feature.search

import br.com.carvalho.podcast.feature.search.presentation.SearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchFeatureModule = module {
    viewModelOf(::SearchViewModel)
}
