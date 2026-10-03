package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_download
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_download_failed
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_download_queued
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloaded
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloading
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** What the download button shows. Screens map their own download status to this. */
sealed interface DownloadState {
    data object Idle : DownloadState
    data object Queued : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data object Downloaded : DownloadState
    data object Failed : DownloadState
}

/**
 * One button for the whole download life cycle. Each state has its own action: download, cancel
 * (queued or downloading), remove (downloaded) and retry (failed).
 */
@Composable
fun DownloadButton(
    state: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (state) {
        DownloadState.Idle -> stringResource(Res.string.ds_download)
        DownloadState.Queued -> stringResource(Res.string.ds_download_queued)
        is DownloadState.Downloading ->
            stringResource(Res.string.ds_downloading, (state.progress * PERCENT).roundToInt())
        DownloadState.Downloaded -> stringResource(Res.string.ds_downloaded)
        DownloadState.Failed -> stringResource(Res.string.ds_download_failed)
    }
    val onClick = when (state) {
        DownloadState.Idle, DownloadState.Failed -> onDownload
        DownloadState.Queued, is DownloadState.Downloading -> onCancel
        DownloadState.Downloaded -> onRemove
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.size(Sizes.touchTarget).semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (state) {
                DownloadState.Idle -> StateIcon(Icons.Rounded.Download)
                DownloadState.Queued -> {
                    CircularProgressIndicator(modifier = Modifier.size(Sizes.iconL), strokeWidth = Sizes.progressStroke)
                    StateIcon(Icons.Rounded.Close, small = true)
                }
                is DownloadState.Downloading -> {
                    CircularProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(Sizes.iconL),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeWidth = Sizes.progressStroke,
                    )
                    StateIcon(Icons.Rounded.Close, small = true)
                }
                DownloadState.Downloaded -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = PodcastTheme.colors.downloaded,
                    modifier = Modifier.size(Sizes.iconM),
                )
                DownloadState.Failed -> Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Sizes.iconM),
                )
            }
        }
    }
}

@Composable
private fun StateIcon(icon: ImageVector, small: Boolean = false) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(if (small) Sizes.iconS else Sizes.iconM),
    )
}

private const val PERCENT = 100
