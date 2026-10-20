package br.com.carvalho.podcast.core.safety

import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Telemetry goes out only if the user left it on and is not a minor in Brazil (16.4, 26.4). */
fun telemetryAllowed(preferences: PreferencesRepository, childSafety: ChildSafety): Boolean =
    preferences.telemetryEnabled.value && !childSafety.state.value.isMinor

/** The same, as it changes: for Firebase's own collection, which also covers its automatic events and crashes. */
fun telemetryAllowedFlow(preferences: PreferencesRepository, childSafety: ChildSafety): Flow<Boolean> =
    combine(preferences.telemetryEnabled, childSafety.state) { enabled, safety -> enabled && !safety.isMinor }
