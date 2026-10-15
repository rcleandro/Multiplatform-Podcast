package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.backup_not_valid
import br.com.carvalho.podcast.core.ui.generated.resources.opml_export_failed
import br.com.carvalho.podcast.core.ui.generated.resources.opml_exported
import br.com.carvalho.podcast.core.ui.generated.resources.opml_imported
import br.com.carvalho.podcast.core.ui.generated.resources.opml_importing
import br.com.carvalho.podcast.core.ui.generated.resources.opml_nothing_to_import
import br.com.carvalho.podcast.core.ui.generated.resources.settings_export_backup
import br.com.carvalho.podcast.core.ui.generated.resources.settings_export_backup_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_export_opml
import br.com.carvalho.podcast.core.ui.generated.resources.settings_export_opml_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_import_opml
import br.com.carvalho.podcast.core.ui.generated.resources.settings_import_opml_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_restore_backup
import br.com.carvalho.podcast.core.ui.generated.resources.settings_restore_backup_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_subscriptions
import br.com.carvalho.podcast.domain.usecase.OpmlImport
import br.com.carvalho.podcast.presentation.file.rememberTextFilePicker
import br.com.carvalho.podcast.presentation.file.rememberTextFileSaver
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The OPML and backup files' pickers, each with the save dialog its export opens. */
@Composable
internal fun rememberSubscriptionFiles(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) = Pair(
    rememberTextFiles(
        toSave = state.opmlToSave,
        fileName = OPML_FILE_NAME,
        extension = OPML_EXTENSION,
        onPicked = { onIntent(SettingsIntent.ImportOpml(it)) },
        onHandedOver = { onIntent(SettingsIntent.OpmlHandedOver) },
        onSaved = { onIntent(SettingsIntent.OpmlSaved(it)) },
    ),
    rememberTextFiles(
        toSave = state.backupToSave,
        fileName = BACKUP_FILE_NAME,
        extension = BACKUP_EXTENSION,
        onPicked = { onIntent(SettingsIntent.RestoreBackup(it)) },
        onHandedOver = { onIntent(SettingsIntent.BackupHandedOver) },
        onSaved = { onIntent(SettingsIntent.BackupSaved(it)) },
    ),
)

/** OPML for other apps (18.13) and the backup of everything for this one (19.11). */
internal fun LazyListScope.subscriptionSettings(
    state: SettingsUiState,
    pickOpml: () -> Unit,
    pickBackup: () -> Unit,
    onIntent: (SettingsIntent) -> Unit,
) {
    item { Section(stringResource(Res.string.settings_subscriptions)) }
    fileRows(
        state.opml,
        FileRowTexts(
            Res.string.settings_import_opml,
            Res.string.settings_import_opml_description,
            Res.string.settings_export_opml,
            Res.string.settings_export_opml_description,
        ),
        onImport = pickOpml,
        onExport = { onIntent(SettingsIntent.ExportOpml) },
    )
    fileRows(
        state.backup,
        FileRowTexts(
            Res.string.settings_restore_backup,
            Res.string.settings_restore_backup_description,
            Res.string.settings_export_backup,
            Res.string.settings_export_backup_description,
        ),
        onImport = pickBackup,
        onExport = { onIntent(SettingsIntent.ExportBackup) },
    )
}

/** The file picker for an import, and the save dialog an export opens once its file is ready; returns the picker. */
@Composable
@Suppress("LongParameterList") // the file's name and what to tell the view model at each step
private fun rememberTextFiles(
    toSave: String?,
    fileName: String,
    extension: String,
    onPicked: (String?) -> Unit,
    onHandedOver: () -> Unit,
    onSaved: (Boolean) -> Unit,
): () -> Unit {
    val pick = rememberTextFilePicker(onPicked)
    val save = rememberTextFileSaver(onSaved)
    LaunchedEffect(toSave) {
        toSave?.let {
            onHandedOver()
            save(fileName, extension, it)
        }
    }
    return pick
}

/** The titles and descriptions of an import row and its export row. */
private class FileRowTexts(
    val importTitle: StringResource,
    val importDescription: StringResource,
    val exportTitle: StringResource,
    val exportDescription: StringResource,
)

/**
 * Bringing the library from a file and taking it to one: as OPML for other apps (18.13), or as a backup of everything
 * for this app (19.11). Each row shows how its last run went.
 */
private fun LazyListScope.fileRows(
    status: OpmlStatus,
    texts: FileRowTexts,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    item {
        PickerRow(
            title = stringResource(texts.importTitle),
            value = when (status) {
                is OpmlStatus.Importing -> stringResource(Res.string.opml_importing, status.done, status.total)
                is OpmlStatus.Imported -> status.result.let {
                    if (it == OpmlImport(0, 0, 0)) {
                        stringResource(Res.string.opml_nothing_to_import)
                    } else {
                        stringResource(Res.string.opml_imported, it.added, it.known, it.failed)
                    }
                }
                OpmlStatus.NotABackup -> stringResource(Res.string.backup_not_valid)
                else -> stringResource(texts.importDescription)
            },
            onClick = { if (status !is OpmlStatus.Importing) onImport() },
        )
    }
    item {
        PickerRow(
            title = stringResource(texts.exportTitle),
            value = when (status) {
                is OpmlStatus.Exported -> stringResource(Res.string.opml_exported, status.podcasts)
                OpmlStatus.ExportFailed -> stringResource(Res.string.opml_export_failed)
                else -> stringResource(texts.exportDescription)
            },
            onClick = onExport,
        )
    }
}

private const val OPML_FILE_NAME = "podcasts"
private const val OPML_EXTENSION = "opml"
private const val BACKUP_FILE_NAME = "podcast-backup"
private const val BACKUP_EXTENSION = "json"
