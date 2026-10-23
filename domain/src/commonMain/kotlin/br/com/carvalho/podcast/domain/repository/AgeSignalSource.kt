package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.AgeGroup

/** Where the platform says the user is and how old (26.2): Play Age Signals, Declared Age Range, or nothing. */
interface AgeSignalSource {
    /** ISO 3166-1 alpha-2 countries the device points to: network, SIM, store account, system language. */
    fun countries(): Set<String>

    /** The shared age range; [AgeGroup.UNKNOWN] when not shared, declined or unavailable. Asked only in Brazil. */
    suspend fun ageGroup(): AgeGroup

    /** Nowhere and no age: outside Brazil, so nothing changes. What tests and previews start with. */
    object None : AgeSignalSource {
        override fun countries(): Set<String> = emptySet()

        override suspend fun ageGroup(): AgeGroup = AgeGroup.UNKNOWN
    }
}
