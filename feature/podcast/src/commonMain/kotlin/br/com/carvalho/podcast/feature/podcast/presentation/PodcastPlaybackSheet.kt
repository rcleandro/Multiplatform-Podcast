package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.icons.Tune
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.auto_download
import br.com.carvalho.podcast.core.ui.generated.resources.auto_download_description
import br.com.carvalho.podcast.core.ui.generated.resources.auto_download_newest
import br.com.carvalho.podcast.core.ui.generated.resources.playback_speed
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_notify_new
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_playback
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_settings_title
import br.com.carvalho.podcast.core.ui.generated.resources.seconds_short
import br.com.carvalho.podcast.core.ui.generated.resources.skip_intro
import br.com.carvalho.podcast.core.ui.generated.resources.skip_intro_description
import br.com.carvalho.podcast.core.ui.generated.resources.skip_off
import br.com.carvalho.podcast.core.ui.generated.resources.skip_outro
import br.com.carvalho.podcast.core.ui.generated.resources.skip_outro_description
import br.com.carvalho.podcast.core.ui.generated.resources.speed_general
import br.com.carvalho.podcast.core.util.canRefreshInBackground
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.usecase.AutoDownload
import br.com.carvalho.podcast.presentation.format.formatSpeed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** How long a jingle or the credits can be skipped; 0 skips nothing. */
@Suppress("MagicNumber") // the choices themselves
private val SKIP_SECONDS = listOf(0, 10, 15, 30, 45, 60, 90, 120)

/** This podcast's own speed and the seconds skipped at the start and end of its episodes (18.14). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastPlaybackSheet(
    podcast: Podcast,
    onChange: (PodcastPlayback) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onAutoDownloadChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val playback = podcast.playback
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier.readableWidth().verticalScroll(rememberScrollState()).padding(bottom = Spacing.xl),
        ) {
            Text(
                stringResource(Res.string.podcast_settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.l).semantics { heading() },
            )
            val speeds = listOf(null) + AppConfig.PLAYBACK_SPEEDS
            Choice(
                title = Res.string.playback_speed,
                options = speeds.map { it?.let(::formatSpeed) ?: stringResource(Res.string.speed_general) },
                selected = speeds.indexOf(playback.speed).coerceAtLeast(0),
                onSelected = { onChange(playback.copy(speed = speeds[it])) },
            )
            Choice(
                title = Res.string.skip_intro,
                description = Res.string.skip_intro_description,
                options = SKIP_SECONDS.map { secondsLabel(it) },
                selected = SKIP_SECONDS.indexOf(playback.skipIntroSeconds).coerceAtLeast(0),
                onSelected = { onChange(playback.copy(skipIntroSeconds = SKIP_SECONDS[it])) },
            )
            Choice(
                title = Res.string.skip_outro,
                description = Res.string.skip_outro_description,
                options = SKIP_SECONDS.map { secondsLabel(it) },
                selected = SKIP_SECONDS.indexOf(playback.skipOutroSeconds).coerceAtLeast(0),
                onSelected = { onChange(playback.copy(skipOutroSeconds = SKIP_SECONDS[it])) },
            )
            if (supportsDownloads) {
                Choice(
                    title = Res.string.auto_download,
                    description = Res.string.auto_download_description,
                    options = AutoDownload.CHOICES.map { autoDownloadLabel(it) },
                    selected = AutoDownload.CHOICES.indexOf(podcast.autoDownloadCount).coerceAtLeast(0),
                    onSelected = { onAutoDownloadChange(AutoDownload.CHOICES[it]) },
                )
            }
            // Only where new episodes are looked for in the background (18.18).
            if (canRefreshInBackground) NotifySwitch(podcast.notifyNewEpisodes, onNotificationsChange)
        }
    }
}

@Composable
private fun NotifySwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        Text(
            stringResource(Res.string.podcast_notify_new),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun autoDownloadLabel(count: Int): String =
    if (count == 0) {
        stringResource(Res.string.skip_off)
    } else {
        pluralStringResource(Res.plurals.auto_download_newest, count, count)
    }

@Composable
private fun secondsLabel(seconds: Int): String =
    if (seconds == 0) stringResource(Res.string.skip_off) else stringResource(Res.string.seconds_short, seconds)

@Composable
private fun Choice(
    title: StringResource,
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    description: StringResource? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(top = Spacing.m)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = Spacing.l).semantics { heading() },
        )
        description?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.l),
            )
        }
        FilterChipRow(
            options = options.map { FilterOption(it) },
            selectedIndex = selected,
            onSelected = onSelected,
            contentPadding = PaddingValues(horizontal = Spacing.l),
        )
    }
}

/** Opens this podcast's playback settings (18.14). */
@Composable
internal fun PlaybackButton(podcast: Podcast, actions: PodcastDetailActions) {
    var open by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Rounded.Tune, contentDescription = stringResource(Res.string.podcast_playback))
    }
    if (open) {
        PodcastPlaybackSheet(
            podcast,
            actions.onPlaybackChange,
            actions.onNotificationsChange,
            actions.onAutoDownloadChange,
            onDismiss = { open = false },
        )
    }
}
