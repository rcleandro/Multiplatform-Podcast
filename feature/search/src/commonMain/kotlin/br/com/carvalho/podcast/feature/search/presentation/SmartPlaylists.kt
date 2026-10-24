package br.com.carvalho.podcast.feature.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.icons.FilterList
import br.com.carvalho.podcast.core.designsystem.icons.automirrored.PlaylistPlay
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.filter_downloaded
import br.com.carvalho.podcast.core.ui.generated.resources.filter_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_any_category
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_any_length
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_edit
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_length
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_name
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_new
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_play_all
import br.com.carvalho.podcast.core.ui.generated.resources.smart_playlist_save
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.model.SmartPlaylist
import br.com.carvalho.podcast.presentation.format.label
import org.jetbrains.compose.resources.stringResource

/** "Play all" and "Edit" above a smart playlist's episodes (19.4). */
internal fun LazyListScope.playlistActions(playlist: SmartPlaylist, actions: SearchActions) {
    item(key = "playlist-actions-${playlist.id}") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.End),
            modifier = Modifier.readableWidth().padding(horizontal = Spacing.s),
        ) {
            TextButton(onClick = { actions.onEditPlaylist(playlist) }) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Text(stringResource(Res.string.smart_playlist_edit), modifier = Modifier.padding(start = Spacing.s))
            }
            TextButton(onClick = actions.onPlayPlaylist) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null)
                Text(stringResource(Res.string.smart_playlist_play_all), modifier = Modifier.padding(start = Spacing.s))
            }
        }
    }
}

/** Nothing matches yet: says so, and the filters can be changed from here. */
@Composable
internal fun EmptyPlaylist(playlist: SmartPlaylist, onEdit: (SmartPlaylist) -> Unit, modifier: Modifier) {
    EmptyState(
        icon = Icons.Rounded.FilterList,
        title = stringResource(Res.string.smart_playlist_empty_title),
        message = stringResource(Res.string.smart_playlist_empty_message),
        actionLabel = stringResource(Res.string.smart_playlist_edit),
        onAction = { onEdit(playlist) },
        modifier = modifier,
    )
}

/**
 * Makes or changes a smart playlist: its name, a category from the feeds' own, unheard only, downloaded only and the
 * longest episode it takes. A saved one can be deleted here too.
 */
@Composable
internal fun SmartPlaylistEditor(
    playlist: SmartPlaylist,
    onSave: (SmartPlaylist) -> Unit,
    onDelete: (SmartPlaylist) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable(stateSaver = PlaylistSaver) { mutableStateOf(playlist) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(if (playlist.id == 0L) Res.string.smart_playlist_new else Res.string.smart_playlist_edit)
            )
        },
        text = { PlaylistForm(draft) { draft = it } },
        confirmButton = {
            TextButton(enabled = draft.name.isNotBlank(), onClick = { onSave(draft) }) {
                Text(stringResource(Res.string.smart_playlist_save))
            }
        },
        dismissButton = {
            Row {
                if (playlist.id != 0L) {
                    TextButton(onClick = { onDelete(playlist) }) { Text(stringResource(Res.string.delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            }
        },
    )
}

/** The editor's fields; each change hands back the playlist as it now is. */
@Composable
private fun PlaylistForm(draft: SmartPlaylist, onChange: (SmartPlaylist) -> Unit) {
    val categories = listOf(null) + PodcastCategory.entries
    val lengths = listOf(null) + SmartPlaylist.LENGTH_CHOICES
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            label = { Text(stringResource(Res.string.smart_playlist_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FilterChipRow(
            options = categories.map {
                FilterOption(stringResource(it?.label ?: Res.string.smart_playlist_any_category))
            },
            selectedIndex = categories.indexOf(draft.category),
            onSelected = { onChange(draft.copy(category = categories[it])) },
        )
        FilterChipRow(
            options = lengths.map {
                FilterOption(
                    if (it == null) {
                        stringResource(Res.string.smart_playlist_any_length)
                    } else {
                        stringResource(Res.string.smart_playlist_length, it)
                    }
                )
            },
            selectedIndex = lengths.indexOf(draft.maxMinutes),
            onSelected = { onChange(draft.copy(maxMinutes = lengths[it])) },
        )
        SwitchLine(stringResource(Res.string.filter_unplayed), draft.unplayedOnly) {
            onChange(draft.copy(unplayedOnly = it))
        }
        if (supportsDownloads) {
            SwitchLine(stringResource(Res.string.filter_downloaded), draft.downloadedOnly) {
                onChange(draft.copy(downloadedOnly = it))
            }
        }
    }
}

/** Keeps a playlist being edited through rotation and the app being put away. */
private val PlaylistSaver = mapSaver(
    save = {
        mapOf(
            "id" to it.id,
            "name" to it.name,
            "category" to it.category?.name,
            "unplayedOnly" to it.unplayedOnly,
            "downloadedOnly" to it.downloadedOnly,
            "maxMinutes" to it.maxMinutes,
        )
    },
    restore = {
        SmartPlaylist(
            id = it["id"] as Long,
            name = it["name"] as String,
            category = (it["category"] as String?)?.let(PodcastCategory::valueOf),
            unplayedOnly = it["unplayedOnly"] as Boolean,
            downloadedOnly = it["downloadedOnly"] as Boolean,
            maxMinutes = it["maxMinutes"] as Int?,
        )
    },
)

@Composable
private fun SwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
