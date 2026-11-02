package br.com.carvalho.podcast.core.safety

import br.com.carvalho.podcast.core.util.deviceCountry
import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.repository.AgeSignalSource

/** Desktop and Web: no store shares an age, so in Brazil the user stays unknown (ADR 0012). */
object NoAgeSignalSource : AgeSignalSource {
    override fun countries(): Set<String> = setOfNotNull(deviceCountry()?.uppercase())

    override suspend fun ageGroup(): AgeGroup = AgeGroup.UNKNOWN
}
