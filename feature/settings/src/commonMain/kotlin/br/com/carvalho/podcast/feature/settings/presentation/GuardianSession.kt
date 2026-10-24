package br.com.carvalho.podcast.feature.settings.presentation

import br.com.carvalho.podcast.domain.model.ChildSafetyState
import br.com.carvalho.podcast.domain.model.GuardianControls
import br.com.carvalho.podcast.domain.model.GuardianPin
import br.com.carvalho.podcast.domain.model.PinAttempts
import br.com.carvalho.podcast.domain.model.PinLockout
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/** The guardians' section of Settings (26.6): shown in Brazil only, its controls open with their PIN. */
data class GuardianState(
    val inBrazil: Boolean = false,
    val hasPin: Boolean = false,
    val unlocked: Boolean = false,
    val controls: GuardianControls = GuardianControls(),
    /** The PIN dialog open, if any. */
    val pinStep: PinStep? = null,
    val pinError: PinError? = null,
    /** With [PinError.WAIT]: the seconds left before the PIN can be tried again (22.11). */
    val pinWaitSeconds: Long = 0,
)

enum class PinStep { CREATE, CONFIRM, UNLOCK }

enum class PinError { WRONG, MISMATCH, WAIT }

sealed interface GuardianIntent : SettingsIntent {
    /** Creates the PIN when there is none, asks for it otherwise. */
    data object Open : GuardianIntent
    data class EnterPin(val pin: String) : GuardianIntent
    data object DismissPin : GuardianIntent
    data object Lock : GuardianIntent
    data object ChangePin : GuardianIntent
    data object RemovePin : GuardianIntent
    data class SetControls(val controls: GuardianControls) : GuardianIntent
}

/**
 * The PIN's steps and the unlocked controls. Unlocking lasts while Settings is open; removing the PIN puts the
 * controls back to their protective defaults, so no one is left with what the guardians had allowed.
 */
internal class GuardianSession(private val preferences: PreferencesRepository, private val now: () -> Long) {
    private val dialog = MutableStateFlow(Dialog())
    private val unlocked = MutableStateFlow(false)

    private data class Dialog(
        val step: PinStep? = null,
        val error: PinError? = null,
        val firstPin: String? = null,
        val waitSeconds: Long = 0,
    )

    fun state(childSafety: Flow<ChildSafetyState>): Flow<GuardianState> = combine(
        childSafety,
        preferences.guardianPin,
        preferences.guardianControls,
        unlocked,
        dialog,
    ) { safety, pin, controls, open, shown ->
        GuardianState(
            inBrazil = safety.inBrazil,
            hasPin = pin != null,
            unlocked = open && pin != null,
            controls = controls,
            pinStep = shown.step,
            pinError = shown.error,
            pinWaitSeconds = shown.waitSeconds,
        )
    }

    fun onIntent(intent: GuardianIntent) {
        when (intent) {
            GuardianIntent.Open ->
                dialog.value = Dialog(if (preferences.guardianPin.value == null) PinStep.CREATE else PinStep.UNLOCK)
            is GuardianIntent.EnterPin -> enter(intent.pin)
            GuardianIntent.DismissPin -> dialog.value = Dialog()
            GuardianIntent.Lock -> unlocked.value = false
            GuardianIntent.ChangePin -> if (unlocked.value) dialog.value = Dialog(PinStep.CREATE)
            GuardianIntent.RemovePin -> if (unlocked.value) {
                preferences.setGuardianPin(null)
                preferences.setGuardianControls(GuardianControls())
                unlocked.value = false
            }
            is GuardianIntent.SetControls -> if (unlocked.value) preferences.setGuardianControls(intent.controls)
        }
    }

    private fun enter(pin: String) {
        val current = dialog.value
        when (current.step) {
            PinStep.CREATE -> if (GuardianPin.isValid(pin)) dialog.value = Dialog(PinStep.CONFIRM, firstPin = pin)
            PinStep.CONFIRM -> if (pin == current.firstPin) {
                preferences.setGuardianPin(GuardianPin.hash(pin))
                unlocked.value = true
                dialog.value = Dialog()
            } else {
                dialog.value = Dialog(PinStep.CREATE, PinError.MISMATCH)
            }
            PinStep.UNLOCK -> unlock(pin)
            null -> Unit
        }
    }

    /** Checks [pin] unless a wait from earlier wrong ones is still running (22.11); the wait is checked first. */
    private fun unlock(pin: String) {
        val attempts = preferences.guardianPinAttempts.value
        if (PinLockout.remainingMs(attempts, now()) > 0) return showWait(attempts)
        if (preferences.guardianPin.value?.let { GuardianPin.matches(pin, it) } == true) {
            preferences.setGuardianPinAttempts(PinAttempts())
            unlocked.value = true
            dialog.value = Dialog()
        } else {
            val failed = PinLockout.failed(attempts, now())
            preferences.setGuardianPinAttempts(failed)
            if (PinLockout.remainingMs(failed, now()) > 0) {
                showWait(failed)
            } else {
                dialog.update { it.copy(error = PinError.WRONG) }
            }
        }
    }

    private fun showWait(attempts: PinAttempts) {
        val seconds = (PinLockout.remainingMs(attempts, now()) + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
        dialog.update { it.copy(error = PinError.WAIT, waitSeconds = seconds) }
    }
}

private const val MILLIS_PER_SECOND = 1000L
