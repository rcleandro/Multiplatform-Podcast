package br.com.carvalho.podcast.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing

// Sample data for previews and snapshot tests only.

@Composable
internal fun PreviewSurface(darkTheme: Boolean, content: @Composable () -> Unit) {
    PodcastTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.padding(Spacing.l)) {
                content()
            }
        }
    }
}

@Composable
internal fun ButtonsSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        PlayPauseButton(isPlaying = false, onClick = {})
        PlayPauseButton(isPlaying = true, onClick = {})
        PlayPauseButton(isPlaying = false, isLoading = true, onClick = {})
        PlayPauseButton(isPlaying = false, onClick = {}, style = PlayButtonStyle.Tonal)
        PlayPauseButton(isPlaying = false, onClick = {}, style = PlayButtonStyle.Tonal, progress = 0.62f)
        PlayPauseButton(isPlaying = false, onClick = {}, size = Sizes.playButtonLarge)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        listOf(
            DownloadState.Idle,
            DownloadState.Queued,
            DownloadState.Downloading(0.64f),
            DownloadState.Downloaded,
            DownloadState.Failed,
        ).forEach { DownloadButton(state = it, onDownload = {}, onCancel = {}, onRemove = {}) }
    }
}

@Composable
internal fun EpisodeRowsSample() {
    EpisodeRow(
        title = "#412 · Os bastidores da Copa do Mundo de 2026",
        metadata = "hoje · 52 min",
        imageUrl = null,
        isNew = true,
        playback = EpisodePlayback(),
        downloadState = DownloadState.Idle,
        onClick = {}, onPlay = {}, onDownload = {}, onCancelDownload = {}, onRemoveDownload = {},
    )
    EpisodeRow(
        title = "Kotlin Multiplatform em produção",
        metadata = "3 dias · faltam 18 min",
        imageUrl = null,
        playback = EpisodePlayback(progress = 0.62f),
        downloadState = DownloadState.Downloaded,
        onClick = {}, onPlay = {}, onDownload = {}, onCancelDownload = {}, onRemoveDownload = {},
    )
    EpisodeRow(
        title = "Como funciona o Pix por dentro",
        metadata = "12 set · 1 h 08 min",
        imageUrl = null,
        playback = EpisodePlayback(isPlayed = true),
        downloadState = DownloadState.Downloading(0.3f),
        onClick = {}, onPlay = {}, onDownload = {}, onCancelDownload = {}, onRemoveDownload = {},
    )
}

@Composable
internal fun PlayerPartsSample() {
    MiniPlayer(
        title = "#142 · O fim das senhas, de verdade",
        subtitle = "Hipsters Ponto Tech",
        imageUrl = null,
        isPlaying = true,
        isLoading = false,
        progress = 0.26f,
        onPlayPause = {},
        onClick = {},
    )
    PlayerSlider(positionMs = 725_000, durationMs = 2_850_000, onSeek = {}, formatTime = { "${it / 60_000}:00" })
    FilterChipRow(
        options = listOf(FilterOption("Todos"), FilterOption("Tecnologia", 5), FilterOption("Comédia", 3)),
        selectedIndex = 0,
        onSelected = {},
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        PodcastCard(title = "Hipsters Ponto Tech", author = "Alura", imageUrl = null, unplayedCount = 3, onClick = {},
            modifier = Modifier.width(Sizes.artworkM))
        PodcastCard(title = "Café da Manhã", author = "Folha de S.Paulo", imageUrl = null, onClick = {},
            modifier = Modifier.width(Sizes.artworkM))
    }
}

@Composable
internal fun StatesSample() {
    EmptyState(
        icon = Icons.Rounded.Mic,
        title = "Sua biblioteca está vazia",
        message = "Busque um podcast pelo nome ou escolha entre os populares.",
        actionLabel = "Buscar podcasts",
        onAction = {},
    )
    ErrorState(
        icon = Icons.Rounded.WifiOff,
        title = "Sem conexão",
        message = "Não deu para atualizar os feeds. Os episódios baixados continuam disponíveis.",
        actionLabel = "Tentar de novo",
        onAction = {},
    )
}

@Preview
@Composable
internal fun ButtonsLightPreview() = PreviewSurface(darkTheme = false) { ButtonsSample() }

@Preview
@Composable
internal fun ButtonsDarkPreview() = PreviewSurface(darkTheme = true) { ButtonsSample() }

@Preview
@Composable
internal fun EpisodeRowsLightPreview() = PreviewSurface(darkTheme = false) { EpisodeRowsSample() }

@Preview
@Composable
internal fun EpisodeRowsDarkPreview() = PreviewSurface(darkTheme = true) { EpisodeRowsSample() }

@Preview
@Composable
internal fun PlayerPartsLightPreview() = PreviewSurface(darkTheme = false) { PlayerPartsSample() }

@Preview
@Composable
internal fun PlayerPartsDarkPreview() = PreviewSurface(darkTheme = true) { PlayerPartsSample() }

@Preview
@Composable
internal fun StatesLightPreview() = PreviewSurface(darkTheme = false) { StatesSample() }

@Preview
@Composable
internal fun StatesDarkPreview() = PreviewSurface(darkTheme = true) { StatesSample() }
