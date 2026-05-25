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
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailScreen
import br.com.carvalho.podcast.feature.player.presentation.PlayerScreen
import br.com.carvalho.podcast.feature.search.presentation.SearchScreen
import br.com.carvalho.podcast.feature.downloads.presentation.DownloadedEpisodesScreen
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.collectAsState
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.presentation.component.MiniPlayer
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.downloads
import br.com.carvalho.podcast.shared.library_title
import br.com.carvalho.podcast.shared.player
import br.com.carvalho.podcast.shared.search
import br.com.carvalho.podcast.shared.select_podcast
import org.jetbrains.compose.resources.stringResource

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
        it is RootComponent.Child.Library
            || it is RootComponent.Child.Search
            || it is RootComponent.Child.DownloadedEpisodes
    }

    var isMiniPlayerVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1) {
                    isMiniPlayerVisible = false
                } else if (available.y > 1) {
                    isMiniPlayerVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isMediumScreenWidth = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        widthDpBreakpoint = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val navSuiteType = if (isMediumScreenWidth) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

    LaunchedEffect(activeChild) {
        isMiniPlayerVisible = true
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

    fun isTabSelected(tabClass: kotlin.reflect.KClass<out RootComponent.Child>): Boolean {
        val isCurrent = activeChild::class == tabClass
        val isLastList = lastListChild != null && lastListChild::class == tabClass
        val isOnStack = activeChild !is RootComponent.Child.Library &&
                       activeChild !is RootComponent.Child.Search &&
                       activeChild !is RootComponent.Child.DownloadedEpisodes &&
                       activeChild !is RootComponent.Child.Player

        return isCurrent || (isOnStack && isLastList)
    }

    NavigationSuiteScaffold(
        layoutType = navSuiteType,
        containerColor = MaterialTheme.colorScheme.surface,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationRailContainerColor = MaterialTheme.colorScheme.surface
        ),
        navigationSuiteItems = {
            item(
                selected = isTabSelected(RootComponent.Child.Library::class),
                onClick = component::onLibraryTabClicked,
                icon = { Icon(Icons.Rounded.Home, contentDescription = stringResource(Res.string.library_title)) },
                label = { Text(stringResource(Res.string.library_title)) }
            )
            item(
                selected = isTabSelected(RootComponent.Child.Search::class),
                onClick = component::onSearchTabClicked,
                icon = { Icon(Icons.Rounded.Search, contentDescription = stringResource(Res.string.search)) },
                label = { Text(stringResource(Res.string.search)) }
            )
            item(
                selected = isTabSelected(RootComponent.Child.DownloadedEpisodes::class),
                onClick = component::onDownloadsTabClicked,
                icon = { Icon(Icons.Rounded.DownloadDone, contentDescription = stringResource(Res.string.downloads)) },
                label = { Text(stringResource(Res.string.downloads)) }
            )
            item(
                selected = activeChild is RootComponent.Child.Player,
                onClick = component::onPlayerTabClicked,
                icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(Res.string.player)) },
                label = { Text(stringResource(Res.string.player)) }
            )
        }
    ) {
        val showMiniPlayer = playerState.currentEpisode != null
                && activeChild !is RootComponent.Child.Player

        Scaffold(
            contentWindowInsets = WindowInsets(),
            modifier = Modifier.nestedScroll(nestedScrollConnection)
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    listPane = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (lastListChild) {
                                is RootComponent.Child.Search -> {
                                    SearchScreen(
                                        onEpisodeClick = { id, podcastId ->
                                            component.onEpisodeSelected(id, podcastId)
                                        }
                                    )
                                }
                                is RootComponent.Child.DownloadedEpisodes -> {
                                    DownloadedEpisodesScreen(
                                        onEpisodeClick = { id, podcastId ->
                                            component.onEpisodeSelected(id, podcastId)
                                        }
                                    )
                                }
                                else -> {
                                    LibraryScreen(
                                        isPlayerVisible = showMiniPlayer && isMiniPlayerVisible,
                                        onPodcastClick = { id -> component.onPodcastSelected(id) }
                                    )
                                }
                            }
                        }
                    },
                    detailPane = {
                        val podcastChild = allChildren.filterIsInstance<RootComponent.Child.PodcastDetail>().lastOrNull()

                        if (podcastChild != null && activeChild !is RootComponent.Child.Library && activeChild !is RootComponent.Child.Search && activeChild !is RootComponent.Child.DownloadedEpisodes) {
                            PodcastDetailScreen(
                                podcastId = podcastChild.podcastId,
                                onBackClick = { component.onBackClicked() },
                                onEpisodeClick = { id, _ -> component.onEpisodeSelected(id, podcastChild.podcastId) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(Res.string.select_podcast), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    },
                    extraPane = {
                        val episodeChild = allChildren.filterIsInstance<RootComponent.Child.EpisodeDetail>().lastOrNull()

                        if (episodeChild != null && activeChild !is RootComponent.Child.Library && activeChild !is RootComponent.Child.Search && activeChild !is RootComponent.Child.DownloadedEpisodes) {
                            EpisodeDetailScreen(
                                episodeId = episodeChild.episodeId,
                                onBackClick = { component.onBackClicked() }
                            )
                        }
                    }
                )

                AnimatedVisibility(
                    visible = showMiniPlayer && isMiniPlayerVisible && activeChild !is RootComponent.Child.Player,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    playerState.currentEpisode?.let { episode ->
                        val duration = playerState.duration
                        val progress = if (duration != null && duration > 0) {
                            playerState.position.toFloat() / duration.toFloat()
                        } else 0f

                        MiniPlayer(
                            episode = episode,
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                            progress = progress,
                            onPlayPauseClick = {
                                if (playerState.isPlaying) playerViewModel.pause()
                                else playerViewModel.resume()
                            },
                            onClick = { component.onPlayerTabClicked() }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = activeChild is RootComponent.Child.Player,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            PlayerScreen(
                onBackClick = { component.onBackClicked() }
            )
        }
    }
}
