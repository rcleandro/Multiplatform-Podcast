package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_allow_continuous
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_allow_continuous_description
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_allow_notifications
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_allow_notifications_description
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_break_every
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_break_never
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_break_reminder
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_change_pin
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_create_pin
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_create_pin_description
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_listening_time
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_lock
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_notice
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_open
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_create
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_mismatch
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_ok
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_repeat
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_unlock
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_wrong
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_remove_pin
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_remove_pin_description
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_system_tools
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_title
import br.com.carvalho.podcast.domain.model.GUARDIAN_PIN_LENGTH
import org.jetbrains.compose.resources.stringResource

/**
 * The guardians' section (26.6, ECA Digital arts. 17 and 18), in Brazil only: what is in effect for a minor, the
 * system's own tools, and, behind the PIN, what the guardians allow.
 */
internal fun LazyListScope.guardianSettings(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onOpenStats: () -> Unit,
    onPickBreak: () -> Unit,
) {
    val guardian = state.guardian
    if (!guardian.inBrazil) return
    item { Section(stringResource(Res.string.guardian_title)) }
    if (state.isMinor) item { Note(stringResource(Res.string.guardian_notice)) }
    when {
        !guardian.hasPin -> item {
            PickerRow(
                title = stringResource(Res.string.guardian_create_pin),
                value = stringResource(Res.string.guardian_create_pin_description),
                onClick = { onIntent(GuardianIntent.Open) },
            )
        }
        !guardian.unlocked -> item {
            PickerRow(stringResource(Res.string.guardian_open), value = null) { onIntent(GuardianIntent.Open) }
        }
        else -> unlockedGuardianSettings(guardian, onIntent, onOpenStats, onPickBreak)
    }
    item { Note(stringResource(Res.string.guardian_system_tools)) }
}

private fun LazyListScope.unlockedGuardianSettings(
    guardian: GuardianState,
    onIntent: (SettingsIntent) -> Unit,
    onOpenStats: () -> Unit,
    onPickBreak: () -> Unit,
) {
    val controls = guardian.controls
    item {
        SwitchRow(
            title = stringResource(Res.string.guardian_allow_continuous),
            description = stringResource(Res.string.guardian_allow_continuous_description),
            checked = controls.allowContinuousPlayback,
            onChange = { onIntent(GuardianIntent.SetControls(controls.copy(allowContinuousPlayback = it))) },
        )
    }
    item {
        SwitchRow(
            title = stringResource(Res.string.guardian_allow_notifications),
            description = stringResource(Res.string.guardian_allow_notifications_description),
            checked = controls.allowNewEpisodeNotifications,
            onChange = { onIntent(GuardianIntent.SetControls(controls.copy(allowNewEpisodeNotifications = it))) },
        )
    }
    item {
        PickerRow(
            title = stringResource(Res.string.guardian_break_reminder),
            value = breakLabel(controls.breakReminderMinutes),
            onClick = onPickBreak,
        )
    }
    item { PickerRow(stringResource(Res.string.guardian_listening_time), value = null, onClick = onOpenStats) }
    item {
        PickerRow(stringResource(Res.string.guardian_change_pin), value = null) { onIntent(GuardianIntent.ChangePin) }
    }
    item {
        PickerRow(
            title = stringResource(Res.string.guardian_remove_pin),
            value = stringResource(Res.string.guardian_remove_pin_description),
            onClick = { onIntent(GuardianIntent.RemovePin) },
        )
    }
    item { PickerRow(stringResource(Res.string.guardian_lock), value = null) { onIntent(GuardianIntent.Lock) } }
}

@Composable
private fun Note(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.readableWidth().padding(vertical = Spacing.s),
)

/** "Every 60 min", or never. */
@Composable
internal fun breakLabel(minutes: Int): String =
    if (minutes == 0) {
        stringResource(Res.string.guardian_break_never)
    } else {
        stringResource(Res.string.guardian_break_every, minutes)
    }

/** Asks for the PIN: to create it (twice) or to open the controls. Digits only, never shown. */
@Composable
internal fun GuardianPinDialog(step: PinStep, error: PinError?, onIntent: (SettingsIntent) -> Unit) {
    var pin by rememberSaveable(step) { mutableStateOf("") }
    val title = when (step) {
        PinStep.CREATE -> Res.string.guardian_pin_create
        PinStep.CONFIRM -> Res.string.guardian_pin_repeat
        PinStep.UNLOCK -> Res.string.guardian_pin_unlock
    }
    AlertDialog(
        onDismissRequest = { onIntent(GuardianIntent.DismissPin) },
        title = { Text(stringResource(title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { typed -> pin = typed.filter { it.isDigit() }.take(GUARDIAN_PIN_LENGTH) },
                    singleLine = true,
                    isError = error != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = error?.let { shown ->
                        val message = when (shown) {
                            PinError.WRONG -> Res.string.guardian_pin_wrong
                            PinError.MISMATCH -> Res.string.guardian_pin_mismatch
                        }
                        { Text(stringResource(message)) }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onIntent(GuardianIntent.EnterPin(pin))
                    pin = ""
                },
                enabled = pin.length == GUARDIAN_PIN_LENGTH,
            ) { Text(stringResource(Res.string.guardian_pin_ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(GuardianIntent.DismissPin) }) { Text(stringResource(Res.string.cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
