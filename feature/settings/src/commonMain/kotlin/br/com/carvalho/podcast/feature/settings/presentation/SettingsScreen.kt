package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import br.com.carvalho.podcast.core.ui.generated.resources.settings_downloads
import br.com.carvalho.podcast.core.ui.generated.resources.settings_privacy
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_dark
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_light
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_system
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.presentation.format.storageSize
import br.com.carvalho.podcast.presentation.format.text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    SettingsContent(
        state = state,
        onBack = onBack,
        onThemeChange = { viewModel.onIntent(SettingsIntent.SetTheme(it)) },
        onTelemetryChange = { viewModel.onIntent(SettingsIntent.SetTelemetry(it)) },
    )
}

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
    onThemeChange: (ThemeMode) -> Unit,
    onTelemetryChange: (Boolean) -> Unit,
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
                        onClick = { onThemeChange(mode) }
                    )
                }
            }
            state.usedBytes?.let { bytes ->
                item { Section(stringResource(Res.string.settings_downloads)) }
                item {
                    Text(
                        stringResource(Res.string.downloads_storage_used, storageSize(bytes).text()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.readableWidth().padding(vertical = Spacing.s),
                    )
                }
            }
            item { Section(stringResource(Res.string.settings_privacy)) }
            item { TelemetryRow(state.telemetryEnabled, onTelemetryChange) }
        }
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
private fun TelemetryRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .readableWidth()
            .toggleable(value = enabled, onValueChange = onChange, role = Role.Switch)
            .padding(vertical = Spacing.s),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs), modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.settings_telemetry), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(Res.string.settings_telemetry_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}
