package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.carvalho.podcast.core.designsystem.component.PodcastSnackbarHost
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.share_from_time
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.presentation.share.episodeShareText
import br.com.carvalho.podcast.presentation.share.rememberShare
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class PlayerDialog { Speed, SleepTimer, Chapters, Bookmarks }

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    artworkModifier: Modifier = Modifier,
    onQueueClick: () -> Unit = {},
) {
    val playerState by viewModel.playerState.collectAsState()
    var dialog by remember { mutableStateOf<PlayerDialog?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val share = rememberEpisodeShare(playerState, snackbarHostState)

    Box {
        PlayerContent(
            state = playerState,
            artworkModifier = artworkModifier,
            actions = PlayerActions(
                onShare = share,
                onMinimize = onBackClick,
                onPlayPause = { viewModel.onIntent(PlayerIntent.PlayPause) },
                onSeek = { viewModel.onIntent(PlayerIntent.SeekTo(it)) },
                onSkipBackward = { viewModel.onIntent(PlayerIntent.SkipBackward) },
                onSkipForward = { viewModel.onIntent(PlayerIntent.SkipForward) },
                onPrevious = { viewModel.onIntent(PlayerIntent.Previous) },
                onNext = { viewModel.onIntent(PlayerIntent.Next) },
                onSpeedClick = { dialog = PlayerDialog.Speed },
                onQueueClick = onQueueClick,
                onSleepTimerClick = { dialog = PlayerDialog.SleepTimer },
                onExtendSleepTimer = { viewModel.onIntent(PlayerIntent.ExtendSleepTimer) },
                onChaptersClick = { dialog = PlayerDialog.Chapters },
                onBookmarksClick = { dialog = PlayerDialog.Bookmarks },
            ),
        )
        PodcastSnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            clearMiniPlayer = false,
        )
    }

    dialog?.let { PlayerDialogs(it, playerState, viewModel, onDismiss = { dialog = null }) }
}

@Composable
private fun PlayerDialogs(
    dialog: PlayerDialog,
    playerState: PlayerState,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        PlayerDialog.Speed -> SpeedSelectorDialog(
            currentSpeed = playerState.speed,
            onSpeedSelected = {
                viewModel.onIntent(PlayerIntent.SetSpeed(it))
                onDismiss()
            },
            onDismiss = onDismiss
        )
        PlayerDialog.SleepTimer -> SleepTimerDialog(
            playerState = playerState,
            onTimerSelected = {
                viewModel.onIntent(PlayerIntent.SetSleepTimer(it))
                onDismiss()
            },
            onDismiss = onDismiss
        )
        PlayerDialog.Chapters -> ChaptersDialog(
            chapters = playerState.chapters,
            current = playerState.currentChapter,
            onChapterSelected = {
                viewModel.onIntent(PlayerIntent.SeekTo(it.startMs))
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        PlayerDialog.Bookmarks -> {
            val bookmarks by viewModel.bookmarks.collectAsState()
            BookmarksDialog(
                positionMs = playerState.position,
                bookmarks = bookmarks,
                onAdd = { viewModel.onIntent(PlayerIntent.AddBookmark(it)) },
                onSelect = {
                    viewModel.onIntent(PlayerIntent.SeekTo(it.positionMs))
                    onDismiss()
                },
                onDelete = { viewModel.onIntent(PlayerIntent.DeleteBookmark(it.id)) },
                onDismiss = onDismiss,
            )
        }
    }
}

/** Shares the episode being heard from the minute it is at (18.4). */
@Composable
private fun rememberEpisodeShare(state: PlayerState, snackbarHostState: SnackbarHostState): () -> Unit {
    val share = rememberShare(snackbarHostState)
    val startsAt = stringResource(Res.string.share_from_time, state.position.toTime())
    return { state.currentEpisode?.let { share(episodeShareText(it, startsAt, state.position)) } }
}
