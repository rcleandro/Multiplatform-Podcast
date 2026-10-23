package br.com.carvalho.podcast.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.report_channels
import br.com.carvalho.podcast.core.ui.generated.resources.report_email_body
import br.com.carvalho.podcast.core.ui.generated.resources.report_email_subject
import br.com.carvalho.podcast.core.ui.generated.resources.report_episode_title
import br.com.carvalho.podcast.core.ui.generated.resources.report_hide
import br.com.carvalho.podcast.core.ui.generated.resources.report_podcast_title
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_advertising
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_gambling
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_harm_to_health
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_other
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_pornography
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_sexual_abuse
import br.com.carvalho.podcast.core.ui.generated.resources.report_reason_violence
import br.com.carvalho.podcast.core.ui.generated.resources.report_send
import br.com.carvalho.podcast.domain.model.ReportReason
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// ponytail: a stand-in address, never shown on screen; replace it with the app's real contact before publishing.
private const val REPORT_EMAIL = "denuncias@example.com"

/** What is reported (26.7): a podcast, or one of its episodes when [episodeTitle] is set. */
data class ReportTarget(val podcastTitle: String, val feedUrl: String, val episodeTitle: String? = null)

private val REASON_LABELS: Map<ReportReason, StringResource> = mapOf(
    ReportReason.SEXUAL_ABUSE to Res.string.report_reason_sexual_abuse,
    ReportReason.VIOLENCE to Res.string.report_reason_violence,
    ReportReason.HARM_TO_HEALTH to Res.string.report_reason_harm_to_health,
    ReportReason.GAMBLING to Res.string.report_reason_gambling,
    ReportReason.ADVERTISING to Res.string.report_reason_advertising,
    ReportReason.PORNOGRAPHY to Res.string.report_reason_pornography,
    ReportReason.OTHER to Res.string.report_reason_other,
)

/**
 * A report of content that violates children's rights (26.7, ECA Digital art. 28): the law's reasons, the official
 * channels for a crime, and whether to hide it here at once.
 */
@Composable
fun ReportDialog(target: ReportTarget, onSend: (ReportReason, hide: Boolean) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf<ReportReason?>(null) }
    var hide by rememberSaveable { mutableStateOf(true) }
    val title = if (target.episodeTitle == null) Res.string.report_podcast_title else Res.string.report_episode_title
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                REASON_LABELS.forEach { (option, label) ->
                    ReportOption(stringResource(label), reason == option, onClick = { reason = option }, radio = true)
                }
                ReportOption(stringResource(Res.string.report_hide), hide, onClick = { hide = !hide }, radio = false)
                Text(
                    stringResource(Res.string.report_channels),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { reason?.let { onSend(it, hide) } }, enabled = reason != null) {
                Text(stringResource(Res.string.report_send))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun ReportOption(label: String, selected: Boolean, onClick: () -> Unit, radio: Boolean) {
    val choice = if (radio) {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxWidth().then(choice),
    ) {
        if (radio) {
            RadioButton(selected = selected, onClick = null)
        } else {
            Checkbox(checked = selected, onCheckedChange = null)
        }
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The report of [target], when there is one: sent by email, then [onHide] if the user asked to hide it. */
@Composable
fun ReportFlow(target: ReportTarget?, onHide: () -> Unit, onClose: () -> Unit) {
    val send = rememberReportSender()
    if (target == null) return
    ReportDialog(
        target = target,
        onSend = { reason, hide ->
            onClose()
            send(target, reason)
            if (hide) onHide()
        },
        onDismiss = onClose,
    )
}

/**
 * Opens the user's email app with the report filled in: the reason, the titles and the feed's host. Never the feed's
 * address, which may carry a private token.
 */
@Composable
fun rememberReportSender(): (ReportTarget, ReportReason) -> Unit {
    val uriHandler = LocalUriHandler.current
    val labels = REASON_LABELS.mapValues { (_, label) -> stringResource(label) }
    val subjectFormat = stringResource(Res.string.report_email_subject, "%1\$s")
    val bodyFormat = stringResource(Res.string.report_email_body, "%1\$s", "%2\$s", "%3\$s", "%4\$s")
    return { target, reason ->
        val subject = subjectFormat.replace("%1\$s", target.episodeTitle ?: target.podcastTitle)
        val body = bodyFormat
            .replace("%1\$s", labels.getValue(reason))
            .replace("%2\$s", target.podcastTitle)
            .replace("%3\$s", target.episodeTitle.orEmpty())
            .replace("%4\$s", feedHost(target.feedUrl))
        // No email app to take it: nothing to do but leave the dialog closed.
        runCatching { uriHandler.openUri(reportMailto(subject, body)) }
    }
}

internal fun reportMailto(subject: String, body: String) =
    "mailto:$REPORT_EMAIL?subject=${subject.percentEncoded()}&body=${body.percentEncoded()}"

/** Only the host: no scheme, credentials, path or query, where a private feed keeps its token. */
internal fun feedHost(feedUrl: String): String =
    feedUrl.substringAfter("://").substringBefore('/').substringBefore('?').substringAfterLast('@')

private const val UNRESERVED = "-._~"
private const val HEX = "0123456789ABCDEF"
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F
private const val BYTE_MASK = 0xFF
private const val ASCII_END = 0x80

private fun String.percentEncoded(): String = buildString {
    for (byte in this@percentEncoded.encodeToByteArray()) {
        val value = byte.toInt() and BYTE_MASK
        val char = value.toChar()
        if (value < ASCII_END && (char.isLetterOrDigit() || char in UNRESERVED)) {
            append(char)
        } else {
            append('%').append(HEX[value shr NIBBLE_BITS]).append(HEX[value and NIBBLE_MASK])
        }
    }
}
