package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.carvalho.podcast.core.extensions.toTime
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.app_icon
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = {
                        Text(
                            text = "AGORA OUVINDO",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Minimizar",
                                modifier = Modifier.size(32.dp)
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
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(if (episode?.imageUrl == null) 48.dp else 0.dp)
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

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = episode?.title ?: "Nenhum episódio selecionado",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                episode?.podcastTitle?.let { title ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                PlayerProgressBar(
                    position = playerState.position,
                    duration = playerState.duration ?: 0,
                    onSeek = { viewModel.seekTo(it) }
                )

                Spacer(modifier = Modifier.height(32.dp))

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

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { showSpeedDialog = true },
                        label = { Text("${playerState.speed}x") },
                        leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )

                    IconButton(onClick = { showQueueDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.List, 
                            contentDescription = "Fila",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        val isTimerActive = playerState.sleepTimerMillis != null
                        val icon = if (isTimerActive) Icons.Rounded.Timer else Icons.Outlined.Timer
                        val tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                        Icon(
                            imageVector = icon,
                            contentDescription = "Timer",
                            tint = tint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Anterior",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(
            onClick = onSkipBack,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Replay10,
                contentDescription = "Voltar 10s",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            } else {
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
            onClick = onSkipForward,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Forward30,
                contentDescription = "Avançar 30s",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(
            onClick = onNext,
            enabled = hasNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Próximo",
                modifier = Modifier.size(32.dp)
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
    val speeds = listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Velocidade de reprodução") },
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
                        Spacer(modifier = Modifier.width(16.dp))
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
                Text("Cancelar")
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
        null to "Desativado",
        5 to "5 minutos",
        15 to "15 minutos",
        30 to "30 minutos",
        45 to "45 minutos",
        60 to "60 minutos"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
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
                        Spacer(modifier = Modifier.width(16.dp))
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
            TextButton(onClick = onDismiss) { Text("Fechar") }
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
        title = { Text("Fila de Reprodução") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                queue.forEach { episode ->
                    val isCurrent = episode.id == currentEpisodeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEpisodeSelected(episode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrent) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
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
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

private fun formatRemainingTime(millis: Long?): String {
    if (millis == null) return ""
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
