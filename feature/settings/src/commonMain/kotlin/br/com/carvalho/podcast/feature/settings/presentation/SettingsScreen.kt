package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.downloads_storage_used
import br.com.carvalho.podcast.core.ui.generated.resources.settings
import br.com.carvalho.podcast.core.ui.generated.resources.settings_appearance
import br.com.carvalho.podcast.core.ui.generated.resources.settings_download_next
import br.com.carvalho.podcast.core.ui.generated.resources.settings_download_next_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_downloads
import br.com.carvalho.podcast.core.ui.generated.resources.settings_no_limit
import br.com.carvalho.podcast.core.ui.generated.resources.settings_privacy
import br.com.carvalho.podcast.core.ui.generated.resources.settings_storage_limit
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_dark
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_light
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_system
import br.com.carvalho.podcast.core.ui.generated.resources.settings_wifi_only
import br.com.carvalho.podcast.core.ui.generated.resources.settings_wifi_only_description
import br.com.carvalho.podcast.core.util.canLimitDownloadsToWifi
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

private const val GIGABYTE = 1024L * 1024 * 1024

/** The download space choices, from none to plenty; null is no limit. */
@Suppress("MagicNumber") // the gigabytes are the choices themselves
private val DOWNLOAD_LIMITS: List<Long?> = listOf(null) + listOf(1, 2, 5, 10, 20).map { it * GIGABYTE }

private val THEME_LABELS: List<Pair<ThemeMode, StringResource>> = listOf(
    ThemeMode.SYSTEM to Res.string.settings_theme_system,
    ThemeMode.LIGHT to Res.string.settings_theme_light,
    ThemeMode.DARK to Res.string.settings_theme_dark,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            THEME_LABELS.forEach { (mode, label) ->
                item {
                    ChoiceRow(
                        stringResource(label),
                        selected = state.themeMode == mode,
                        onClick = { onIntent(SettingsIntent.SetTheme(mode)) }
                    )
                }
            }
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
}

/** Where there are downloads: their space, Wi-Fi only on phones and tablets, their limit and the next episode. */
private fun LazyListScope.downloadSettings(bytes: Long, state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    item { Section(stringResource(Res.string.settings_downloads)) }
    item {
        Text(
            stringResource(Res.string.downloads_storage_used, storageSize(bytes).text()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.readableWidth().padding(vertical = Spacing.s),
        )
    }
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
    item { LimitRow(state.downloadLimitBytes) { onIntent(SettingsIntent.SetDownloadLimit(it)) } }
    item {
        SwitchRow(
            title = stringResource(Res.string.settings_download_next),
            description = stringResource(Res.string.settings_download_next_description),
            checked = state.downloadNextOnFinish,
            onChange = { onIntent(SettingsIntent.SetDownloadNext(it)) },
        )
    }
}

@Composable
private fun Section(title: String) {
    SectionTitle(title, Modifier.readableWidth().padding(top = Spacing.l, bottom = Spacing.xs))
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .readableWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(vertical = Spacing.m))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .readableWidth()
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch)
            .padding(vertical = Spacing.s),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs), modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** The space the downloads may take: the current choice, and the others in a menu. */
@Composable
private fun LimitRow(limit: Long?, onChange: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.readableWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = Spacing.s),
        ) {
            Text(stringResource(Res.string.settings_storage_limit), style = MaterialTheme.typography.bodyLarge)
            Text(
                limitLabel(limit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DOWNLOAD_LIMITS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(limitLabel(option)) },
                    onClick = {
                        open = false
                        onChange(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun limitLabel(bytes: Long?): String =
    bytes?.let { storageSize(it).text() } ?: stringResource(Res.string.settings_no_limit)
