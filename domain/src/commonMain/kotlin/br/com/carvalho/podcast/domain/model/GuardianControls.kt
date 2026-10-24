package br.com.carvalho.podcast.domain.model

import okio.ByteString.Companion.encodeUtf8
import kotlin.random.Random

/**
 * What a minor's guardians allow in Brazil (26.6, ECA Digital arts. 17 and 18), behind their PIN. Explicit content is
 * not among them: the law keeps it from minors whatever the guardians say (art. 9).
 */
data class GuardianControls(
    val allowContinuousPlayback: Boolean = false,
    val allowNewEpisodeNotifications: Boolean = false,
    /** Minutes of listening before a break is suggested (26.5); 0 never suggests one. */
    val breakReminderMinutes: Int = DEFAULT_BREAK_MINUTES,
) {
    companion object {
        const val DEFAULT_BREAK_MINUTES = 60
        val BREAK_CHOICES = listOf(30, 60, 90, 120, 0)
    }
}

const val GUARDIAN_PIN_LENGTH = 4
private const val SALT_BYTES = 16

/** A PIN kept only as "salt:sha256(salt:pin)", so the preferences never hold the digits themselves. */
object GuardianPin {
    fun isValid(pin: String): Boolean = pin.length == GUARDIAN_PIN_LENGTH && pin.all { it.isDigit() }

    fun hash(pin: String, salt: String = newSalt()): String = "$salt:${digest(salt, pin)}"

    fun matches(pin: String, stored: String): Boolean {
        val salt = stored.substringBefore(':')
        return stored == "$salt:${digest(salt, pin)}"
    }

    private fun digest(salt: String, pin: String) = "$salt:$pin".encodeUtf8().sha256().hex()

    @OptIn(ExperimentalStdlibApi::class)
    private fun newSalt() = Random.nextBytes(SALT_BYTES).toHexString()
}

/**
 * Wrong PINs in a row and when the next try may come (22.11, T11 of ADR 0014). Kept in the preferences, so closing the
 * app does not start the count over.
 */
data class PinAttempts(val failures: Int = 0, val lockedUntil: Long = 0)

/**
 * Four digits are what a guardian remembers, and 10,000 tries fit in an afternoon. The first [FREE_FAILURES] wrong
 * PINs cost nothing, a typo is common; after that each one waits 30 s, doubling up to an hour, so the whole range
 * would take years.
 */
object PinLockout {
    const val FREE_FAILURES = 5
    private const val FIRST_WAIT_MS = 30_000L
    private const val MAX_WAIT_MS = 3_600_000L

    // 30 s shifted 7 times is already past the hour.
    private const val MAX_DOUBLINGS = 7

    /** How long the next try waits after [failures] wrong PINs in a row. */
    fun waitAfter(failures: Int): Long = if (failures < FREE_FAILURES) {
        0
    } else {
        (FIRST_WAIT_MS shl (failures - FREE_FAILURES).coerceAtMost(MAX_DOUBLINGS)).coerceAtMost(MAX_WAIT_MS)
    }

    fun failed(attempts: PinAttempts, now: Long): PinAttempts =
        (attempts.failures + 1).let { failures -> PinAttempts(failures, now + waitAfter(failures)) }

    // ponytail: the wall clock, so a wait outlives a restart; a minor who sets the clock forward skips it. The
    // system's parental controls are the boundary that holds (ADR 0014).
    fun remainingMs(attempts: PinAttempts, now: Long): Long = (attempts.lockedUntil - now).coerceAtLeast(0)
}
