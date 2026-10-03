package br.com.carvalho.podcast.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.feature.downloads.presentation.DownloadedEpisodesContent
import br.com.carvalho.podcast.feature.downloads.presentation.DownloadedEpisodesUiState
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailContent
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailUiState
import br.com.carvalho.podcast.feature.library.presentation.LibraryActions
import br.com.carvalho.podcast.feature.library.presentation.LibraryContent
import br.com.carvalho.podcast.feature.library.presentation.LibraryUiState
import br.com.carvalho.podcast.feature.player.presentation.PlayerActions
import br.com.carvalho.podcast.feature.player.presentation.PlayerContent
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailActions
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailContent
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailUiState
import br.com.carvalho.podcast.feature.search.presentation.SearchActions
import br.com.carvalho.podcast.feature.search.presentation.SearchContent
import br.com.carvalho.podcast.feature.search.presentation.SearchUiState
import kotlinx.coroutines.flow.flowOf

// Sample data for previews only.

private val samplePodcasts = listOf(
    Podcast("1", "Hipsters Ponto Tech", "Tecnologia", null, "Alura", "pt", listOf("Tecnologia"), "u1", null, 0, true),
    Podcast("2", "Café da Manhã", "Notícias", null, "Folha de S.Paulo", "pt", listOf("Notícias"), "u2", null, 0, true),
    Podcast("3", "Naruhodo", "Ciência", null, "B9", "pt", listOf("Ciência"), "u3", null, 0, true),
)

private val sampleEpisodes = listOf(
    Episode("e1", "1", "Hipsters Ponto Tech", "Kotlin Multiplatform em produção", "<p>Conversa sobre <b>KMP</b>.</p>",
        "", null, 2_820, 1_790_000_000_000, false, 1_200_000, true, null),
    Episode("e2", "1", "Hipsters Ponto Tech", "Os bastidores do Pix", null, "", null, 4_080, 1_789_000_000_000,
        true, 0, false, null),
    Episode("e3", "1", "Hipsters Ponto Tech", "Design systems que escalam", null, "", null, 3_100, 1_788_000_000_000,
        false, 0, false, null),
)

private val playingState = PlayerState(
    currentEpisode = sampleEpisodes.first(),
    isPlaying = true,
    position = 725_000,
    duration = 2_820_000,
    speed = 1.5f,
    queue = sampleEpisodes,
)

@Composable
private fun episodesPaging() = flowOf(PagingData.from(sampleEpisodes)).collectAsLazyPagingItems()

@Preview
@Composable
internal fun LibraryContentPreview() = PodcastTheme(darkTheme = false) {
    LibraryContent(
        state = LibraryUiState(podcasts = samplePodcasts),
        actions = LibraryActions(),
        isPlayerVisible = true,
    )
}

@Preview
@Composable
internal fun LibraryEmptyDarkPreview() = PodcastTheme(darkTheme = true) {
    LibraryContent(state = LibraryUiState(), actions = LibraryActions())
}

@Preview
@Composable
internal fun LibraryLoadingPreview() = PodcastTheme(darkTheme = false) {
    LibraryContent(state = LibraryUiState(isLoading = true), actions = LibraryActions())
}

@Preview
@Composable
internal fun PodcastDetailDarkPreview() = PodcastTheme(darkTheme = true) {
    PodcastDetailContent(
        state = PodcastDetailUiState(podcast = samplePodcasts.first(), episodes = sampleEpisodes),
        episodes = episodesPaging(),
        playerState = playingState,
        activeDownloads = mapOf("e3" to DownloadStatus.Downloading(0.4f, 0, null)),
        actions = PodcastDetailActions(),
    )
}

@Preview
@Composable
internal fun SearchContentPreview() = PodcastTheme(darkTheme = false) {
    SearchContent(
        state = SearchUiState(searchQuery = "kotlin"),
        results = episodesPaging(),
        playerState = PlayerState(),
        activeDownloads = emptyMap(),
        actions = SearchActions(),
    )
}

@Preview
@Composable
internal fun DownloadsEmptyPreview() = PodcastTheme(darkTheme = false) {
    DownloadedEpisodesContent(
        state = DownloadedEpisodesUiState(),
        playerState = PlayerState(),
        activeDownloads = emptyMap(),
        onEpisodeClick = {}, onPlay = {}, onRemove = {}, onConfirmRemove = {}, onDismissRemove = {},
    )
}

@Preview
@Composable
internal fun EpisodeDetailPreview() = PodcastTheme(darkTheme = true) {
    EpisodeDetailContent(state = EpisodeDetailUiState(episode = sampleEpisodes.first()), onBack = {}, onPlay = {})
}

@Preview
@Composable
internal fun PlayerLightPreview() = PodcastTheme(darkTheme = false) {
    PlayerContent(state = playingState, actions = PlayerActions())
}

@Preview
@Composable
internal fun PlayerDarkPreview() = PodcastTheme(darkTheme = true) {
    PlayerContent(state = playingState.copy(isPlaying = false, isBuffering = true), actions = PlayerActions())
}
