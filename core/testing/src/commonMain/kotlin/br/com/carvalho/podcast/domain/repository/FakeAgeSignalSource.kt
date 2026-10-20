package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.AgeGroup

class FakeAgeSignalSource(
    var countries: Set<String> = emptySet(),
    var ageGroup: AgeGroup = AgeGroup.UNKNOWN,
) : AgeSignalSource {
    /** Thrown by the next [ageGroup] instead of answering, like a store that failed. */
    var failure: Exception? = null
    var asked = 0

    override fun countries(): Set<String> = countries

    override suspend fun ageGroup(): AgeGroup {
        asked++
        failure?.let { throw it }
        return ageGroup
    }
}
