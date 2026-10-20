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
