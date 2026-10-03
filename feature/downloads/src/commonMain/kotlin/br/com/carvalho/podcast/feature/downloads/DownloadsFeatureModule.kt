package br.com.carvalho.podcast.feature.downloads

import br.com.carvalho.podcast.feature.downloads.presentation.DownloadedEpisodesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val downloadsFeatureModule = module {
    viewModelOf(::DownloadedEpisodesViewModel)
}
