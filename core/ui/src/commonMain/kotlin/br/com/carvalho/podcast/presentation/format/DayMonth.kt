package br.com.carvalho.podcast.presentation.format

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.date_day_month
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

/** A day as "12/10", in the order the language writes it, from days since 1970-01-01. */
@Composable
fun dayMonth(epochDay: Long): String =
    LocalDate.fromEpochDays(epochDay).let { stringResource(Res.string.date_day_month, it.day, it.month.number) }
