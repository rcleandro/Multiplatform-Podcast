package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.downloads_storage_used
import br.com.carvalho.podcast.core.ui.generated.resources.language_english
import br.com.carvalho.podcast.core.ui.generated.resources.language_portuguese
import br.com.carvalho.podcast.core.ui.generated.resources.language_spanish
import br.com.carvalho.podcast.core.ui.generated.resources.settings
import br.com.carvalho.podcast.core.ui.generated.resources.settings_appearance
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_all
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_all_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_finished
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_finished_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_download_next
import br.com.carvalho.podcast.core.ui.generated.resources.settings_download_next_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_downloads
import br.com.carvalho.podcast.core.ui.generated.resources.settings_language
import br.com.carvalho.podcast.core.ui.generated.resources.settings_language_ios
import br.com.carvalho.podcast.core.ui.generated.resources.settings_playback
import br.com.carvalho.podcast.core.ui.generated.resources.settings_privacy
import br.com.carvalho.podcast.core.ui.generated.resources.settings_skip_backward
import br.com.carvalho.podcast.core.ui.generated.resources.settings_skip_forward
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_dark
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_light
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_system
import br.com.carvalho.podcast.core.ui.generated.resources.settings_wifi_only
import br.com.carvalho.podcast.core.ui.generated.resources.settings_wifi_only_description
import br.com.carvalho.podcast.core.util.canLimitDownloadsToWifi
import br.com.carvalho.podcast.core.util.openSystemLanguageSettings
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.presentation.format.storageSize
import br.com.carvalho.podcast.presentation.format.text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    SettingsContent(state = state, onBack = onBack, onIntent = viewModel::onIntent)
}

private val THEME_LABELS: Map<ThemeMode, StringResource> = mapOf(
    ThemeMode.SYSTEM to Res.string.settings_theme_system,
    ThemeMode.LIGHT to Res.string.settings_theme_light,
    ThemeMode.DARK to Res.string.settings_theme_dark,
)

/** Each language by its own name, so whoever does not read the current one still finds theirs. */
private val LANGUAGE_LABELS: Map<AppLanguage, StringResource> = mapOf(
    AppLanguage.SYSTEM to Res.string.settings_theme_system,
    AppLanguage.ENGLISH to Res.string.language_english,
    AppLanguage.PORTUGUESE to Res.string.language_portuguese,
    AppLanguage.SPANISH to Res.string.language_spanish,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf<Picker?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item { Section(stringResource(Res.string.settings_appearance)) }
            item {
                PickerRow(
                    title = stringResource(Res.string.settings_theme),
                    value = stringResource(THEME_LABELS.getValue(state.themeMode)),
                    onClick = { picking = Picker.THEME },
                )
            }
            item { LanguageRow(state.language) { picking = Picker.LANGUAGE } }
            playbackSettings(state.skipIntervals, onIntent)
            state.usedBytes?.let { downloadSettings(it, state, onIntent) }
            item { Section(stringResource(Res.string.settings_privacy)) }
            item {
                SwitchRow(
                    title = stringResource(Res.string.settings_telemetry),
                    description = stringResource(Res.string.settings_telemetry_description),
                    checked = state.telemetryEnabled,
                    onChange = { onIntent(SettingsIntent.SetTelemetry(it)) },
                )
            }
        }
    }

    picking?.let { PickerDialog(it, state, onDone = { picking = null }, onIntent = onIntent) }

    if (state.isConfirmingDeleteAll) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_delete_all),
            message = stringResource(
                Res.string.settings_delete_all_confirmation,
                storageSize(state.usedBytes ?: 0).text()
            ),
            confirmLabel = stringResource(Res.string.delete),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = { onIntent(SettingsIntent.ConfirmDeleteAll) },
            onDismiss = { onIntent(SettingsIntent.DismissDeleteAll) },
        )
    }
}

/** Settings whose choices open in a dialog, so the screen stays short as they grow (18.5). */
private enum class Picker { THEME, LANGUAGE }

/** The choices of [picker]; [onDone] closes it, whether one was picked or not. */
@Composable
private fun PickerDialog(
    picker: Picker,
    state: SettingsUiState,
    onDone: () -> Unit,
    onIntent: (SettingsIntent) -> Unit,
) {
    when (picker) {
        Picker.THEME -> ChoiceDialog(
            title = stringResource(Res.string.settings_theme),
            options = THEME_LABELS.map { (mode, label) -> mode to stringResource(label) },
            selected = state.themeMode,
            onSelect = {
                onDone()
                onIntent(SettingsIntent.SetTheme(it))
            },
            onDismiss = onDone,
        )
        Picker.LANGUAGE -> ChoiceDialog(
            title = stringResource(Res.string.settings_language),
            options = LANGUAGE_LABELS.map { (language, label) -> language to stringResource(label) },
            selected = state.language,
            onSelect = {
                onDone()
                onIntent(SettingsIntent.SetLanguage(it))
            },
            onDismiss = onDone,
        )
    }
}

/** The app's language, chosen in a dialog or, on iOS, in the system's Settings (18.5). */
@Composable
private fun LanguageRow(language: AppLanguage, onPick: () -> Unit) {
    val openSystemSettings = openSystemLanguageSettings
    PickerRow(
        title = stringResource(Res.string.settings_language),
        value = stringResource(
            if (openSystemSettings != null) Res.string.settings_language_ios else LANGUAGE_LABELS.getValue(language)
        ),
        onClick = openSystemSettings ?: onPick,
    )
}

/** How far the jump buttons go, everywhere they show (18.5). */
private fun LazyListScope.playbackSettings(skips: SkipIntervals, onIntent: (SettingsIntent) -> Unit) {
    item { Section(stringResource(Res.string.settings_playback)) }
    item {
        SkipChoice(stringResource(Res.string.settings_skip_backward), skips.backwardSeconds) {
            onIntent(SettingsIntent.SetSkipIntervals(skips.copy(backwardSeconds = it)))
        }
    }
    item {
        SkipChoice(stringResource(Res.string.settings_skip_forward), skips.forwardSeconds) {
            onIntent(SettingsIntent.SetSkipIntervals(skips.copy(forwardSeconds = it)))
        }
    }
}

/** Where there are downloads: their space, Wi-Fi only on phones and tablets, their limit and the next episode. */
private fun LazyListScope.downloadSettings(bytes: Long, state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    item { Section(stringResource(Res.string.settings_downloads)) }
    item {
        val space = state.disk
        if (space != null) {
            DiskUsage(space, appBytes = bytes, limit = state.downloadLimitBytes)
        } else {
            Text(
                stringResource(Res.string.downloads_storage_used, storageSize(bytes).text()),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.readableWidth().padding(vertical = Spacing.s),
            )
        }
    }
    item { LimitSlider(state.limitStops, state.downloadLimitBytes) { onIntent(SettingsIntent.SetDownloadLimit(it)) } }
    if (canLimitDownloadsToWifi) {
        item {
            SwitchRow(
                title = stringResource(Res.string.settings_wifi_only),
                description = stringResource(Res.string.settings_wifi_only_description),
                checked = state.wifiOnlyDownloads,
                onChange = { onIntent(SettingsIntent.SetWifiOnly(it)) },
            )
        }
    }
    item {
        SwitchRow(
            title = stringResource(Res.string.settings_download_next),
            description = stringResource(Res.string.settings_download_next_description),
            checked = state.downloadNextOnFinish,
            onChange = { onIntent(SettingsIntent.SetDownloadNext(it)) },
        )
    }
    item {
        SwitchRow(
            title = stringResource(Res.string.settings_delete_finished),
            description = stringResource(Res.string.settings_delete_finished_description),
            checked = state.deleteFinishedDownloads,
            onChange = { onIntent(SettingsIntent.SetDeleteFinished(it)) },
        )
    }
    if (bytes > 0) {
        item {
            OutlinedButton(
                onClick = { onIntent(SettingsIntent.RequestDeleteAll) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(vertical = Spacing.s),
            ) {
                Text(stringResource(Res.string.settings_delete_all))
            }
        }
    }
}
