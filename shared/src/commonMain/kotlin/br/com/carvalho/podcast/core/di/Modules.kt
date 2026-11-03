package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.feature.discover.discoverFeatureModule
import br.com.carvalho.podcast.feature.episode.episodeFeatureModule
import br.com.carvalho.podcast.feature.library.libraryFeatureModule
import br.com.carvalho.podcast.feature.player.playerFeatureModule
import br.com.carvalho.podcast.feature.podcast.podcastFeatureModule
import br.com.carvalho.podcast.feature.search.searchFeatureModule
import br.com.carvalho.podcast.feature.settings.settingsFeatureModule

/** The app's graph: the shared core (`:core:di`) and the screens' view models. */
val commonModules = coreModules + listOf(
    libraryFeatureModule,
    podcastFeatureModule,
    episodeFeatureModule,
    searchFeatureModule,
    playerFeatureModule,
    discoverFeatureModule,
    settingsFeatureModule,
)
