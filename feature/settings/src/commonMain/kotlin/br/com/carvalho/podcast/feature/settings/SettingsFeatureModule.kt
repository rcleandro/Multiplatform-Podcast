package br.com.carvalho.podcast.feature.settings

import br.com.carvalho.podcast.feature.settings.presentation.ListeningStatsViewModel
import br.com.carvalho.podcast.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsFeatureModule = module {
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ListeningStatsViewModel)
}
