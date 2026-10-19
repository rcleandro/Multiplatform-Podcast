package br.com.carvalho.podcast.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The sound's switches on the watch's player (19.9f): the phone's speech effects, and headphones only. */
data class WatchEffects(
    val skipSilence: Boolean = false,
    val boostVoice: Boolean = false,
    val headphonesOnly: Boolean = false,
)

/** Reads and changes [WatchEffects] in the preferences the media service follows. */
class WatchEffectsViewModel(private val preferences: PreferencesRepository) : ViewModel() {
    val effects: StateFlow<WatchEffects> = combine(
        preferences.skipSilence,
        preferences.boostVoice,
        preferences.headphonesOnly,
        ::WatchEffects,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, WatchEffects())

    fun set(effects: WatchEffects) {
        preferences.setSkipSilence(effects.skipSilence)
        preferences.setBoostVoice(effects.boostVoice)
        preferences.setHeadphonesOnly(effects.headphonesOnly)
    }
}
