package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.settings_refresh
import br.com.carvalho.podcast.core.ui.generated.resources.settings_refresh_every
import br.com.carvalho.podcast.core.ui.generated.resources.settings_refresh_never
import br.com.carvalho.podcast.core.ui.generated.resources.settings_updates
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** How often new episodes are looked for while the app is closed (18.18). */
internal fun LazyListScope.updateSettings(hours: Int, onPick: () -> Unit) {
    item { Section(stringResource(Res.string.settings_updates)) }
    item {
        PickerRow(
            title = stringResource(Res.string.settings_refresh),
            value = refreshLabel(hours),
            onClick = onPick,
        )
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
