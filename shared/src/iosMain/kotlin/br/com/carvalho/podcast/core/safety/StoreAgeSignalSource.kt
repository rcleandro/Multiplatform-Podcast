package br.com.carvalho.podcast.core.safety

import br.com.carvalho.podcast.core.util.deviceCountry
import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.model.ageGroupOf
import br.com.carvalho.podcast.domain.repository.AgeSignalSource
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * The App Store's country and Declared Age Range (iOS 26), both read in Swift (iOSApp.swift) and handed over here
 * (26.2, ADR 0012). Before Swift answers, the range is unknown.
 */
object StoreAgeSignalSource : AgeSignalSource {
    private var storeCountry: String? = null
    private var ageGroup = AgeGroup.UNKNOWN

    override fun countries(): Set<String> = setOfNotNull(storeCountry, deviceCountry()?.uppercase())

    override suspend fun ageGroup(): AgeGroup = ageGroup

    internal fun update(storeCountry: String?, lower: Int?, upper: Int?) {
        this.storeCountry = storeCountry
        ageGroup = ageGroupOf(lower, upper)
    }
}

/** True when the app may need the age range: Swift asks the App Store only then, so no prompt shows elsewhere. */
fun isInBrazil(storeCountry: String?): Boolean =
    BRAZIL in setOfNotNull(alpha2(storeCountry), deviceCountry()?.uppercase())

/** What the App Store answered; called by iOSApp.swift each time the app comes to the foreground. */
fun updateAgeSignal(storeCountry: String?, lower: Int?, upper: Int?) {
    StoreAgeSignalSource.update(alpha2(storeCountry), lower, upper)
    MainScope().launch { KoinPlatform.getKoin().get<ChildSafety>().refresh() }
}

private const val BRAZIL = "BR"
private const val BRAZIL_ALPHA3 = "BRA"

// StoreKit gives the storefront in alpha-3; only Brazil matters here.
private fun alpha2(alpha3: String?): String? = if (alpha3.equals(BRAZIL_ALPHA3, ignoreCase = true)) BRAZIL else alpha3
