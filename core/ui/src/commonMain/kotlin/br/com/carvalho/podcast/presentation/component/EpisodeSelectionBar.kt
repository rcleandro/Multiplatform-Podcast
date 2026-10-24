package br.com.carvalho.podcast.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.carvalho.podcast.core.designsystem.icon.PodcastIcons
import br.com.carvalho.podcast.core.designsystem.icons.Download
import br.com.carvalho.podcast.core.designsystem.icons.automirrored.PlaylistAdd
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_to_queue
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_downloading
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_played
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_queued
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.close_selection
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_unplayed
import br.com.carvalho.podcast.core.ui.generated.resources.selected_count
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.usecase.EpisodeAction
import br.com.carvalho.podcast.presentation.UiMessage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The bar that takes a screen's place while several episodes are selected (18.11): how many, what to do to all of
 * them, and closing the selection, which the back gesture also does.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EpisodeSelectionBar(count: Int, onAction: (EpisodeAction) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    TopAppBar(
        title = { Text(stringResource(Res.string.selected_count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.close_selection))
            }
        },
        actions = {
            EpisodeAction.entries.filter { it != EpisodeAction.DOWNLOAD || supportsDownloads }.forEach { action ->
                val (label, icon) = action.button()
                IconButton(onClick = { onAction(action) }, enabled = count > 0) {
                    Icon(icon, contentDescription = stringResource(label))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    )
}

private fun EpisodeAction.button(): Pair<StringResource, ImageVector> = when (this) {
    EpisodeAction.ADD_TO_QUEUE -> Res.string.add_to_queue to Icons.AutoMirrored.Rounded.PlaylistAdd
    EpisodeAction.DOWNLOAD -> Res.string.download_cd to Icons.Rounded.Download
    EpisodeAction.MARK_PLAYED -> Res.string.mark_as_played to Icons.Rounded.Done
    EpisodeAction.MARK_UNPLAYED -> Res.string.mark_as_unplayed to PodcastIcons.MarkUnplayed
}

/** What the snackbar says after acting on [count] selected episodes. */
fun EpisodeAction.message(count: Int): UiMessage = UiMessage(
    when (this) {
        EpisodeAction.ADD_TO_QUEUE -> Res.string.bulk_queued
        EpisodeAction.DOWNLOAD -> Res.string.bulk_downloading
        EpisodeAction.MARK_PLAYED -> Res.string.bulk_played
        EpisodeAction.MARK_UNPLAYED -> Res.string.bulk_unplayed
    },
    listOf(count),
)

/** The selection with [episode] added, or taken out when it was already there; the order is the order chosen. */
fun List<Episode>.toggled(episode: Episode): List<Episode> =
    if (any { it.id == episode.id }) filterNot { it.id == episode.id } else this + episode
