package br.com.carvalho.podcast.presentation.format

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.date_group_last_24_hours
import br.com.carvalho.podcast.core.ui.generated.resources.date_group_last_30_days
import br.com.carvalho.podcast.core.ui.generated.resources.date_group_last_7_days
import br.com.carvalho.podcast.core.ui.generated.resources.date_group_older
import br.com.carvalho.podcast.core.ui.generated.resources.date_short
import br.com.carvalho.podcast.core.ui.generated.resources.time_days_ago
import br.com.carvalho.podcast.core.ui.generated.resources.time_hours_ago
import br.com.carvalho.podcast.core.ui.generated.resources.time_just_now
import br.com.carvalho.podcast.core.ui.generated.resources.time_minutes_ago
import br.com.carvalho.podcast.core.ui.generated.resources.time_yesterday
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RelativeTime.text(): String = when (this) {
    RelativeTime.JustNow -> stringResource(Res.string.time_just_now)
    is RelativeTime.Minutes -> pluralStringResource(Res.plurals.time_minutes_ago, count, count)
    is RelativeTime.Hours -> pluralStringResource(Res.plurals.time_hours_ago, count, count)
    RelativeTime.Yesterday -> stringResource(Res.string.time_yesterday)
    is RelativeTime.Days -> pluralStringResource(Res.plurals.time_days_ago, count, count)
    is RelativeTime.Date -> stringResource(Res.string.date_short, day, month, year)
}

@Composable
fun DateGroup.text(): String = stringResource(
    when (this) {
        DateGroup.LAST_24_HOURS -> Res.string.date_group_last_24_hours
        DateGroup.LAST_7_DAYS -> Res.string.date_group_last_7_days
        DateGroup.LAST_30_DAYS -> Res.string.date_group_last_30_days
        DateGroup.OLDER -> Res.string.date_group_older
    }
)
