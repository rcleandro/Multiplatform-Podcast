package br.com.carvalho.podcast.feature.library

import br.com.carvalho.podcast.feature.library.presentation.LibraryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val libraryFeatureModule = module {
    viewModelOf(::LibraryViewModel)
}
