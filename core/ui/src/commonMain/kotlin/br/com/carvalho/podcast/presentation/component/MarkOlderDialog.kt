package br.com.carvalho.podcast.presentation.component

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.designsystem.component.ConfirmDialog
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.mark
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_confirmation
import br.com.carvalho.podcast.core.ui.generated.resources.mark_older_unplayed_confirmation
import br.com.carvalho.podcast.domain.model.Episode
import org.jetbrains.compose.resources.stringResource

/** Marking [episode] and every older one of its podcast as [played] or not, waiting for the user to confirm. */
data class OlderMark(val episode: Episode, val played: Boolean)

/** Marking one episode is done straight from the menu; [mark] changes many, so it asks first. */
@Composable
fun MarkOlderDialog(mark: OlderMark, onConfirm: (OlderMark) -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(if (mark.played) Res.string.mark_older_as_played else Res.string.mark_older_as_unplayed),
        message = stringResource(
            if (mark.played) Res.string.mark_older_confirmation else Res.string.mark_older_unplayed_confirmation,
            mark.episode.title,
        ),
        confirmLabel = stringResource(Res.string.mark),
        dismissLabel = stringResource(Res.string.cancel),
        onConfirm = { onConfirm(mark) },
        onDismiss = onDismiss,
    )
}
