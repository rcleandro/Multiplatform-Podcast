package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.PodcastArtwork
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.icons.Headphones
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.stats_by_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.stats_by_week
import br.com.carvalho.podcast.core.ui.generated.resources.stats_empty_message
import br.com.carvalho.podcast.core.ui.generated.resources.stats_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.stats_saved
import br.com.carvalho.podcast.core.ui.generated.resources.stats_saved_description
import br.com.carvalho.podcast.core.ui.generated.resources.stats_this_week
import br.com.carvalho.podcast.core.ui.generated.resources.stats_title
import br.com.carvalho.podcast.core.ui.generated.resources.stats_total
import br.com.carvalho.podcast.core.ui.generated.resources.stats_week_of
import br.com.carvalho.podcast.domain.model.ListeningStats
import br.com.carvalho.podcast.domain.model.PodcastListening
import br.com.carvalho.podcast.domain.model.WeekListening
import br.com.carvalho.podcast.presentation.format.dayMonth
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val MILLIS_PER_SECOND = 1000L

/** Milliseconds as the app writes lengths: "45min", "1h 5min". */
private fun Long.heard(): String = (this / MILLIS_PER_SECOND).toDuration()

@Composable
fun ListeningStatsScreen(onBack: () -> Unit, viewModel: ListeningStatsViewModel = koinViewModel()) {
    val stats by viewModel.stats.collectAsState()
    ListeningStatsContent(stats, onBack)
}

/** Time heard this week, in total and saved, then the last weeks as bars and each podcast's share (19.5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningStatsContent(stats: ListeningStats, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.stats_title), modifier = Modifier.semantics { heading() }) },
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
        if (stats.totalListenedMs <= 0) {
            EmptyState(
                icon = Icons.Rounded.Headphones,
                title = stringResource(Res.string.stats_empty_title),
                message = stringResource(Res.string.stats_empty_message),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item { Totals(stats) }
            item { SectionTitle(stringResource(Res.string.stats_by_week), Modifier.readableWidth()) }
            item { WeekBars(stats.weeks) }
            item { SectionTitle(stringResource(Res.string.stats_by_podcast), Modifier.readableWidth()) }
            items(stats.podcasts, key = { it.podcast.id }) { PodcastRow(it) }
        }
    }
}

/** Three tiles that wrap on a narrow window. */
@Composable
private fun Totals(stats: ListeningStats) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.readableWidth(),
    ) {
        StatTile(stringResource(Res.string.stats_this_week), stats.thisWeekListenedMs.heard())
        StatTile(stringResource(Res.string.stats_total), stats.totalListenedMs.heard())
        if (stats.totalSavedMs > 0) {
            StatTile(
                stringResource(Res.string.stats_saved),
                stats.totalSavedMs.heard(),
                stringResource(Res.string.stats_saved_description),
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, detail: String? = null) {
    val tileWidth = 160.dp
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.widthIn(min = tileWidth, max = tileWidth * 2),
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * One bar per week, oldest on the start side, heights against the busiest week. Only this week's bar carries its
 * value; each bar tells a screen reader its week and time.
 */
@Composable
private fun WeekBars(weeks: List<WeekListening>) {
    val chartHeight = 120.dp
    val barCorner = 4.dp
    val most = weeks.maxOfOrNull { it.listenedMs }?.takeIf { it > 0 } ?: 1
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.readableWidth(),
    ) {
        weeks.forEachIndexed { index, week ->
            val label = dayMonth(week.startEpochDay)
            val description = stringResource(Res.string.stats_week_of, label, week.listenedMs.heard())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
            ) {
                if (index == weeks.lastIndex) {
                    Text(week.listenedMs.heard(), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                Box(modifier = Modifier.height(chartHeight).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(week.listenedMs.toFloat() / most)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topStart = barCorner, topEnd = barCorner),
                            ),
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PodcastRow(entry: PodcastListening) {
    val artworkSize = 48.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.readableWidth().padding(vertical = Spacing.xs),
    ) {
        PodcastArtwork(
            imageUrl = entry.podcast.imageUrl,
            contentDescription = null,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(artworkSize),
        )
        Text(
            entry.podcast.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            entry.listenedMs.heard(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
