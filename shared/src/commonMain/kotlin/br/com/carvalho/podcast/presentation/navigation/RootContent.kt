package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import br.com.carvalho.podcast.feature.library.presentation.LibraryScreen
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailScreen
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailViewModel
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailScreen
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailViewModel
import br.com.carvalho.podcast.feature.player.presentation.PlayerScreen
import br.com.carvalho.podcast.feature.search.presentation.SearchScreen
import br.com.carvalho.podcast.feature.downloads.presentation.DownloadedEpisodesScreen
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.collectAsState
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.search.presentation.SearchViewModel
import br.com.carvalho.podcast.presentation.component.MiniPlayer
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun RootContent(component: RootComponentImpl) {
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.playerState.collectAsState()
    val stack by component.stack.subscribeAsState()
    val activeChild = stack.active.instance

    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()

    val allChildren = stack.backStack.map { it.instance } + activeChild
    val lastListChild = allChildren.lastOrNull {
        it is RootComponent.Child.Library || it is RootComponent.Child.Search || it is RootComponent.Child.DownloadedEpisodes
    }

    LaunchedEffect(activeChild) {
        when (activeChild) {
            is RootComponent.Child.Library, is RootComponent.Child.Search, is RootComponent.Child.DownloadedEpisodes -> {
                navigator.navigateTo(ListDetailPaneScaffoldRole.List)
            }
            is RootComponent.Child.PodcastDetail -> {
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
            }
            is RootComponent.Child.EpisodeDetail -> {
                navigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
            }
            else -> {}
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = activeChild is RootComponent.Child.Library || (activeChild !is RootComponent.Child.Search && activeChild !is RootComponent.Child.DownloadedEpisodes && activeChild !is RootComponent.Child.Player && lastListChild is RootComponent.Child.Library),
                onClick = component::onLibraryTabClicked,
                icon = { Icon(Icons.Rounded.Home, contentDescription = "Biblioteca") },
                label = { Text("Biblioteca") }
            )
            item(
                selected = activeChild is RootComponent.Child.Search || (activeChild !is RootComponent.Child.Library && activeChild !is RootComponent.Child.DownloadedEpisodes && activeChild !is RootComponent.Child.Player && lastListChild is RootComponent.Child.Search),
                onClick = component::onSearchTabClicked,
                icon = { Icon(Icons.Rounded.Search, contentDescription = "Busca") },
                label = { Text("Busca") }
            )
            item(
                selected = activeChild is RootComponent.Child.DownloadedEpisodes || (activeChild !is RootComponent.Child.Library && activeChild !is RootComponent.Child.Search && activeChild !is RootComponent.Child.Player && lastListChild is RootComponent.Child.DownloadedEpisodes),
                onClick = component::onDownloadsTabClicked,
                icon = { Icon(Icons.Rounded.DownloadDone, contentDescription = "Downloads") },
                label = { Text("Downloads") }
            )
            item(
                selected = activeChild is RootComponent.Child.Player,
                onClick = component::onPlayerTabClicked,
                icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = "Player") },
                label = { Text("Player") }
            )
        }
    ) {
        val showMiniPlayer = playerState.currentEpisode != null
                && activeChild !is RootComponent.Child.Player

        Scaffold(
            bottomBar = {
                if (showMiniPlayer) {
                    playerState.currentEpisode?.let { episode ->
                        MiniPlayer(
                            episode = episode,
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                            onPlayPauseClick = {
                                if (playerState.isPlaying) playerViewModel.pause()
                                else playerViewModel.resume()
                            },
                            onClick = { component.onPlayerTabClicked() }
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets()
        ) { padding ->
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (lastListChild) {
                            is RootComponent.Child.Search -> {
                                val viewModel: SearchViewModel = koinViewModel()
                                SearchScreen(
                                    viewModel = viewModel,
                                    onEpisodeClick = { id, podcastId ->
                                        component.onEpisodeSelected(id, podcastId)
                                    },
                                    onPlayEpisode = { episode ->
                                        playerViewModel.play(episode)
                                        component.onPlayerTabClicked()
                                    }
                                )
                            }
                            is RootComponent.Child.DownloadedEpisodes -> {
                                DownloadedEpisodesScreen(
                                    viewModel = koinViewModel(),
                                    onEpisodeClick = { id, podcastId ->
                                        component.onEpisodeSelected(id, podcastId)
                                    }
                                )
                            }
                            else -> {
                                LibraryScreen(
                                    viewModel = koinViewModel(),
                                    onPodcastClick = { id -> component.onPodcastSelected(id) }
                                )
                            }
                        }
                    }
                },
                detailPane = {
                    val podcastChild = allChildren.filterIsInstance<RootComponent.Child.PodcastDetail>().lastOrNull()

                    if (podcastChild != null && activeChild !is RootComponent.Child.Library && activeChild !is RootComponent.Child.Search && activeChild !is RootComponent.Child.DownloadedEpisodes) {
                        val viewModel: PodcastDetailViewModel = koinViewModel(key = podcastChild.podcastId) { parametersOf(podcastChild.podcastId) }
                        PodcastDetailScreen(
                            viewModel = viewModel,
                            onBackClick = { component.onBackClicked() },
                            onEpisodeClick = { id, _ -> component.onEpisodeSelected(id, podcastChild.podcastId) },
                            onPlayEpisode = { id ->
                                viewModel.playEpisode(id)
                                component.onPlayerTabClicked()
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Selecione um podcast", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                },
                extraPane = {
                    val episodeChild = allChildren.filterIsInstance<RootComponent.Child.EpisodeDetail>().lastOrNull()

                    if (episodeChild != null && activeChild !is RootComponent.Child.Library && activeChild !is RootComponent.Child.Search && activeChild !is RootComponent.Child.DownloadedEpisodes) {
                        val viewModel: EpisodeDetailViewModel = koinViewModel(key = episodeChild.episodeId) { parametersOf(episodeChild.episodeId) }
                        EpisodeDetailScreen(
                            viewModel = viewModel,
                            onBackClick = { component.onBackClicked() },
                            onPlayClick = {
                                viewModel.play()
                                component.onPlayerTabClicked()
                            }
                        )
                    }
                },
                modifier = Modifier.padding(padding)
            )

            if (activeChild is RootComponent.Child.Player) {
                PlayerScreen(
                    viewModel = koinViewModel(),
                    onBackClick = { component.onBackClicked() }
                )
            }
        }
    }
}
