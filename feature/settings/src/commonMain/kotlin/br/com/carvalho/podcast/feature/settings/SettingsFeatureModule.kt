package br.com.carvalho.podcast.feature.settings

import br.com.carvalho.podcast.feature.settings.presentation.ListeningStatsViewModel
import br.com.carvalho.podcast.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsFeatureModule = module {
    // By hand: viewModelOf asks Koin for every constructor parameter, the defaulted `now` too, which has no definition.
    viewModel {
        SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModelOf(::ListeningStatsViewModel)
}
