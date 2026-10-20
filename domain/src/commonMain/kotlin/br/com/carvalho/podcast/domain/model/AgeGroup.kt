package br.com.carvalho.podcast.domain.model

/** The age range the store or system shares (26.2, ADR 0012): never stored, sent or synced. */
enum class AgeGroup { ADULT, MINOR, UNKNOWN }

private const val ADULT_AGE = 18

/** An age range as both stores give it: bounds in years, either one missing when not shared. */
fun ageGroupOf(lower: Int?, upper: Int?): AgeGroup = when {
    lower != null && lower >= ADULT_AGE -> AgeGroup.ADULT
    upper != null && upper < ADULT_AGE -> AgeGroup.MINOR
    else -> AgeGroup.UNKNOWN
}

/**
 * What Brazil's ECA Digital changes for this user (ADR 0012). Outside Brazil, or with an adult signal, nothing does.
 */
data class ChildSafetyState(val inBrazil: Boolean = false, val ageGroup: AgeGroup = AgeGroup.UNKNOWN) {
    /** Explicit podcasts and episodes are hidden and do not play: for minors and without a signal alike. */
    val hidesExplicit: Boolean get() = inBrazil && ageGroup != AgeGroup.ADULT

    /** Safeguards a minor cannot turn off: telemetry, the queue moving on by itself, notifications. */
    val isMinor: Boolean get() = inBrazil && ageGroup == AgeGroup.MINOR
}
