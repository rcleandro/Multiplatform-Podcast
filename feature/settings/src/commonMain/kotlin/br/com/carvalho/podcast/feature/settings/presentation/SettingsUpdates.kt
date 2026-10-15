package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_after
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_after_days
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_after_never
import br.com.carvalho.podcast.core.ui.generated.resources.settings_notify_new
import br.com.carvalho.podcast.core.ui.generated.resources.settings_notify_new_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_refresh
import br.com.carvalho.podcast.core.ui.generated.resources.settings_refresh_every
import br.com.carvalho.podcast.core.ui.generated.resources.settings_refresh_never
import br.com.carvalho.podcast.core.ui.generated.resources.settings_updates
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** How often new episodes are looked for while the app is closed (18.18). */
internal fun LazyListScope.updateSettings(
    state: SettingsUiState,
    onPick: () -> Unit,
    onIntent: (SettingsIntent) -> Unit,
) {
    item { Section(stringResource(Res.string.settings_updates)) }
    item {
        PickerRow(
            title = stringResource(Res.string.settings_refresh),
            value = refreshLabel(state.refreshIntervalHours),
            onClick = onPick,
        )
    }
    // Nothing is looked for while closed, so nothing would notify.
    if (state.refreshIntervalHours > 0) {
        item {
            SwitchRow(
                title = stringResource(Res.string.settings_notify_new),
                description = stringResource(Res.string.settings_notify_new_description),
                checked = state.notifyNewEpisodes,
                onChange = { onIntent(SettingsIntent.SetNotifyNewEpisodes(it)) },
            )
        }
    }
}

/** "Every 6 hours", or never. */
@Composable
internal fun refreshLabel(hours: Int): String =
    if (hours == 0) {
        stringResource(Res.string.settings_refresh_never)
    } else {
        pluralStringResource(Res.plurals.settings_refresh_every, hours, hours)
    }

/** "30 days", or kept. */
@Composable
internal fun deleteAfterLabel(days: Int): String =
    if (days == 0) {
        stringResource(Res.string.settings_delete_after_never)
    } else {
        pluralStringResource(Res.plurals.settings_delete_after_days, days, days)
    }

/** Opens the choice of how long downloads stay (18.19). */
internal fun LazyListScope.deleteAfterRow(days: Int, onPick: () -> Unit) {
    item {
        PickerRow(
            title = stringResource(Res.string.settings_delete_after),
            value = deleteAfterLabel(days),
            onClick = onPick,
        )
    }
}
