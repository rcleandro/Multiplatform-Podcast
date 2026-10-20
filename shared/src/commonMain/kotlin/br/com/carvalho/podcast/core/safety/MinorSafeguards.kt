package br.com.carvalho.podcast.core.safety

import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Telemetry goes out only if the user left it on and is not a minor in Brazil (16.4, 26.4). */
fun telemetryAllowed(preferences: PreferencesRepository, childSafety: ChildSafety): Boolean =
    preferences.telemetryEnabled.value && !childSafety.state.value.isMinor

/** The same, as it changes: for Firebase's own collection, which also covers its automatic events and crashes. */
fun telemetryAllowedFlow(preferences: PreferencesRepository, childSafety: ChildSafety): Flow<Boolean> =
    combine(preferences.telemetryEnabled, childSafety.state) { enabled, safety -> enabled && !safety.isMinor }

/** The queue moves on by itself only if the user left it on and is not a minor in Brazil (26.5). */
fun continuousPlaybackAllowed(
    preferences: PreferencesRepository,
    childSafety: ChildSafety,
    scope: CoroutineScope,
): StateFlow<Boolean> =
    combine(preferences.continuousPlayback, childSafety.state) { enabled, safety -> enabled && !safety.isMinor }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = preferences.continuousPlayback.value && !childSafety.state.value.isMinor,
        )
