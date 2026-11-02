package br.com.carvalho.podcast.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.usage_reminder_keep_listening
import br.com.carvalho.podcast.core.ui.generated.resources.usage_reminder_message
import br.com.carvalho.podcast.core.ui.generated.resources.usage_reminder_title
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.usecase.UsageReminder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** The break [UsageReminder] suggests to a minor in Brazil (26.5), over whatever screen is open. */
@Composable
fun UsageReminderHost() {
    val reminder = koinInject<UsageReminder>()
    val player = koinInject<AudioPlayer>()
    val due by reminder.due.collectAsState()
    if (due) {
        UsageReminderDialog(
            onPause = {
                player.pause()
                reminder.dismiss()
            },
            onKeepListening = reminder::dismiss,
        )
    }
}

@Composable
fun UsageReminderDialog(onPause: () -> Unit, onKeepListening: () -> Unit) = AlertDialog(
    onDismissRequest = onKeepListening,
    title = { Text(stringResource(Res.string.usage_reminder_title)) },
    text = { Text(stringResource(Res.string.usage_reminder_message)) },
    confirmButton = { TextButton(onClick = onPause) { Text(stringResource(Res.string.pause)) } },
    dismissButton = {
        TextButton(onClick = onKeepListening) { Text(stringResource(Res.string.usage_reminder_keep_listening)) }
    },
)
