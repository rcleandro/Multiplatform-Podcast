package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.AppDimensions
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.app_icon
import br.com.carvalho.podcast.shared.cancel
import br.com.carvalho.podcast.shared.close
import br.com.carvalho.podcast.shared.minimize
import br.com.carvalho.podcast.shared.next
import br.com.carvalho.podcast.shared.no_episode_selected
import br.com.carvalho.podcast.shared.now_playing
import br.com.carvalho.podcast.shared.pause
import br.com.carvalho.podcast.shared.play
import br.com.carvalho.podcast.shared.playback_speed
import br.com.carvalho.podcast.shared.previous
import br.com.carvalho.podcast.shared.queue
import br.com.carvalho.podcast.shared.skip_backward
import br.com.carvalho.podcast.shared.skip_forward
import br.com.carvalho.podcast.shared.sleep_timer
import br.com.carvalho.podcast.shared.timer_15_min
import br.com.carvalho.podcast.shared.timer_30_min
import br.com.carvalho.podcast.shared.timer_45_min
import br.com.carvalho.podcast.shared.timer_5_min
import br.com.carvalho.podcast.shared.timer_60_min
import br.com.carvalho.podcast.shared.timer_disabled
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val episode = playerState.currentEpisode
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified
                    ),
                    title = {
                        Text(
                            text = stringResource(Res.string.now_playing),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(Res.string.minimize),
                                modifier = Modifier.size(AppDimensions.iconLarge)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            if (showSpeedDialog) {
                SpeedSelectorDialog(
                    currentSpeed = playerState.speed,
                    onSpeedSelected = {
                        viewModel.setSpeed(it)
                        showSpeedDialog = false
                    },
                    onDismiss = { showSpeedDialog = false }
                )
            }

            if (showSleepTimerDialog) {
                SleepTimerDialog(
                    playerState = playerState,
                    onTimerSelected = {
                        viewModel.setSleepTimer(it)
                        showSleepTimerDialog = false
                    },
                    onDismiss = { showSleepTimerDialog = false }
                )
            }

            if (showQueueDialog) {
                QueueDialog(
                    queue = playerState.queue,
                    currentEpisodeId = episode?.id,
                    onEpisodeSelected = {
                        viewModel.play(it)
                        showQueueDialog = false
                    },
                    onDismiss = { showQueueDialog = false }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = AppDimensions.paddingLarge)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(AppDimensions.spacingLarge))

                Box(
                    modifier = Modifier
                        .widthIn(max = AppDimensions.podcastImageSize)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(
                            if (episode?.imageUrl == null) AppDimensions.paddingGigantic
                            else AppDimensions.paddingNone
                        )
                ) {
                    AsyncImage(
                        model = episode?.imageUrl,
                        contentDescription = episode?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(Res.drawable.app_icon),
                        error = painterResource(Res.drawable.app_icon)
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.spacingGigantic))

                Text(
                    text = episode?.title ?: stringResource(Res.string.no_episode_selected),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                episode?.podcastTitle?.let { title ->
                    Spacer(modifier = Modifier.height(AppDimensions.spacingMedium))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.spacingGigantic))

                PlayerProgressBar(
                    position = playerState.position,
                    duration = playerState.duration ?: 0,
                    onSeek = { viewModel.seekTo(it) }
                )

                Spacer(modifier = Modifier.height(AppDimensions.spacingHuge))

                val currentIndex = playerState.queue.indexOfFirst { it.id == episode?.id }
                PlaybackControls(
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isBuffering,
                    onPlayPause = { if (playerState.isPlaying) viewModel.pause() else viewModel.resume() },
                    onSkipBack = { viewModel.skipBackward() },
                    onSkipForward = { viewModel.skipForward() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    hasNext = currentIndex != -1 && currentIndex < playerState.queue.size - 1,
                    hasPrevious = currentIndex > 0
                )

                Spacer(modifier = Modifier.height(AppDimensions.paddingGigantic))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { showSpeedDialog = true },
                        label = { Text("${playerState.speed}x") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimensions.iconSmall)
                            )
                        }
                    )

                    IconButton(onClick = { showQueueDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.List,
                            contentDescription = stringResource(Res.string.queue),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        val isTimerActive = playerState.sleepTimerMillis != null
                        val icon = if (isTimerActive) Icons.Rounded.Timer else Icons.Outlined.Timer
                        val tint =
                            if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(Res.string.sleep_timer),
                            tint = tint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.paddingLarge))
            }
        }
    }
}

@Composable
private fun PlayerProgressBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var sliderPosition by remember(position) { mutableStateOf(position.toFloat()) }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onSeek(sliderPosition.toLong()) },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = position.toTime(),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = duration.toTime(),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    hasNext: Boolean,
    hasPrevious: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = hasPrevious,
            modifier = Modifier.size(AppDimensions.touchTarget)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(Res.string.previous),
                modifier = Modifier.size(AppDimensions.iconLarge)
            )
        }

        IconButton(
            onClick = onSkipBack,
            modifier = Modifier.size(AppDimensions.touchTarget)
        ) {
            Icon(
                imageVector = Icons.Rounded.Replay10,
                contentDescription = stringResource(Res.string.skip_backward),
                modifier = Modifier.size(AppDimensions.iconLarge)
            )
        }

        Spacer(modifier = Modifier.width(AppDimensions.spacingLarge))

        Box(
            modifier = Modifier.size(AppDimensions.playButtonSize),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimensions.paddingGigantic),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = AppDimensions.strokeWidthNormal
                )
            } else {
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(AppDimensions.playButtonRadius)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                        modifier = Modifier.size(AppDimensions.iconExtraLarge)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(AppDimensions.spacingLarge))

        IconButton(
            onClick = onSkipForward,
            modifier = Modifier.size(AppDimensions.touchTarget)
        ) {
            Icon(
                imageVector = Icons.Rounded.Forward30,
                contentDescription = stringResource(Res.string.skip_forward),
                modifier = Modifier.size(AppDimensions.iconLarge)
            )
        }

        IconButton(
            onClick = onNext,
            enabled = hasNext,
            modifier = Modifier.size(AppDimensions.touchTarget)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(Res.string.next),
                modifier = Modifier.size(AppDimensions.iconLarge)
            )
        }
    }
    }

    @Composable
    private fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = AppConfig.PLAYBACK_SPEEDS
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.playback_speed)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = speed == currentSpeed,
                                onClick = { onSpeedSelected(speed) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = speed == currentSpeed,
                            onClick = { onSpeedSelected(speed) }
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.spacingLarge))
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun SleepTimerDialog(
    playerState: PlayerState,
    onTimerSelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        null to stringResource(Res.string.timer_disabled),
        5 to stringResource(Res.string.timer_5_min),
        15 to stringResource(Res.string.timer_15_min),
        30 to stringResource(Res.string.timer_30_min),
        45 to stringResource(Res.string.timer_45_min),
        60 to stringResource(Res.string.timer_60_min)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sleep_timer)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                options.forEach { (minutes, label) ->
                    val isSelected = minutes == playerState.selectedSleepTimerMinutes

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onTimerSelected(minutes) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onTimerSelected(minutes) }
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.spacingLarge))
                        Text(
                            text = if (isSelected && minutes != null) {
                                "$label (${formatRemainingTime(playerState.sleepTimerMillis)})"
                            } else {
                                label
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        }
    )
}

@Composable
private fun QueueDialog(
    queue: List<Episode>,
    currentEpisodeId: String?,
    onEpisodeSelected: (Episode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.queue)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = AppDimensions.maxPlayerContentHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                queue.forEach { episode ->
                    val isCurrent = episode.id == currentEpisodeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEpisodeSelected(episode) }
                            .padding(vertical = AppDimensions.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrent) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(AppDimensions.spacingMedium))
                        }
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        }
    )
}

private fun formatRemainingTime(millis: Long?): String {
    if (millis == null) return ""
    val totalSeconds = millis / AppConfig.MILLIS_PER_SECOND
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
