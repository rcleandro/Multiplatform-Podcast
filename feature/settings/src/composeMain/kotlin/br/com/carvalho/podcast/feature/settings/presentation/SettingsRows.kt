package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.component.StorageBar
import br.com.carvalho.podcast.core.designsystem.component.StorageKind
import br.com.carvalho.podcast.core.designsystem.component.StorageSegment
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.seconds_short
import br.com.carvalho.podcast.core.ui.generated.resources.settings_no_limit
import br.com.carvalho.podcast.core.ui.generated.resources.settings_storage_limit
import br.com.carvalho.podcast.core.ui.generated.resources.storage_bar_description
import br.com.carvalho.podcast.core.ui.generated.resources.storage_legend_app
import br.com.carvalho.podcast.core.ui.generated.resources.storage_legend_free
import br.com.carvalho.podcast.core.ui.generated.resources.storage_legend_other
import br.com.carvalho.podcast.core.ui.generated.resources.storage_total
import br.com.carvalho.podcast.core.util.DiskSpace
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.presentation.format.storageSize
import br.com.carvalho.podcast.presentation.format.text
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** One jump, picked among the seconds every button can show (18.5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkipChoice(label: String, seconds: Int, onChange: (Int) -> Unit) {
    val choices = SkipIntervals.CHOICES
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.readableWidth().padding(vertical = Spacing.s),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            choices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = choice == seconds,
                    onClick = { onChange(choice) },
                    shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                    label = { Text(stringResource(Res.string.seconds_short, choice)) },
                )
            }
        }
    }
}

@Composable
internal fun Section(title: String) {
    SectionTitle(title, Modifier.readableWidth().padding(top = Spacing.l, bottom = Spacing.xs))
}

@Composable
internal fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
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

/** A setting that opens its choices: its name and, below it, what is chosen now. */
@Composable
internal fun PickerRow(title: String, value: String?, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        modifier = Modifier
            .readableWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.s),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        value?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The choices of a [PickerRow]; picking one closes it. The list scrolls once there are more than fit. */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (option, label) ->
                    ChoiceRow(label, selected = option == selected, onClick = { onSelect(option) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
internal fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .readableWidth()
            .toggleable(value = checked, onValueChange = onChange, enabled = enabled, role = Role.Switch)
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
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * The space the downloads may take, on a slider of round sizes that ends at "no limit". The choice is saved when the
 * thumb is let go, not on every step it passes.
 */
@Composable
internal fun LimitSlider(stops: List<Long?>, limit: Long?, onChange: (Long?) -> Unit) {
    var index by remember(stops, limit) { mutableFloatStateOf(stops.indexOf(limit).coerceAtLeast(0).toFloat()) }
    val shown = limitLabel(stops[index.roundToInt()])
    Column(modifier = Modifier.readableWidth().padding(vertical = Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.settings_storage_limit),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(shown, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = index,
            onValueChange = { index = it },
            onValueChangeFinished = { onChange(stops[index.roundToInt()]) },
            valueRange = 0f..stops.lastIndex.toFloat(),
            steps = (stops.size - 2).coerceAtLeast(0),
            modifier = Modifier.semantics { stateDescription = shown },
        )
    }
}

/** The disk the downloads live on, with what they take, what everything else takes and what is free. */
@Composable
internal fun DiskUsage(disk: DiskSpace, appBytes: Long, limit: Long?) {
    val other = (disk.totalBytes - disk.freeBytes - appBytes).coerceAtLeast(0)
    val app = storageSize(appBytes).text()
    val others = storageSize(other).text()
    val free = storageSize(disk.freeBytes).text()
    val total = storageSize(disk.totalBytes).text()
    StorageBar(
        segments = listOf(
            // The downloads grow from where the rest of the disk ends, towards the limit marked after them.
            StorageSegment(other, StorageKind.OTHER, stringResource(Res.string.storage_legend_other, others)),
            StorageSegment(appBytes, StorageKind.APP, stringResource(Res.string.storage_legend_app, app)),
            StorageSegment(disk.freeBytes, StorageKind.FREE, stringResource(Res.string.storage_legend_free, free)),
        ),
        totalBytes = disk.totalBytes,
        caption = stringResource(Res.string.storage_total, total),
        description = stringResource(Res.string.storage_bar_description, app, others, free, total),
        limitBytes = limit?.let { other + it },
        modifier = Modifier.readableWidth().padding(vertical = Spacing.s),
    )
}

@Composable
internal fun limitLabel(bytes: Long?): String =
    bytes?.let { storageSize(it).text() } ?: stringResource(Res.string.settings_no_limit)
