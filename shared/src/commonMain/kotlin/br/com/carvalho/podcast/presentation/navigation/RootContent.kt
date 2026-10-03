package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.window.core.layout.WindowSizeClass
import br.com.carvalho.podcast.core.designsystem.Motion
import br.com.carvalho.podcast.core.designsystem.component.MiniPlayer
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.downloads
import br.com.carvalho.podcast.core.ui.generated.resources.library_title
import br.com.carvalho.podcast.core.ui.generated.resources.player
import br.com.carvalho.podcast.core.ui.generated.resources.search
import br.com.carvalho.podcast.core.ui.generated.resources.select_podcast
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.feature.downloads.presentation.DownloadedEpisodesScreen
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailScreen
import br.com.carvalho.podcast.feature.library.presentation.LibraryScreen
import br.com.carvalho.podcast.feature.player.presentation.PlayerIntent
import br.com.carvalho.podcast.feature.player.presentation.PlayerScreen
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailScreen
import br.com.carvalho.podcast.feature.search.presentation.SearchScreen
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Draws [RootComponent.state]: tabs, the list/detail/extra panes of the selected tab, mini player and player. */
@Composable
fun RootContent(component: RootComponent) {
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.playerState.collectAsState()
    val state by component.state.subscribeAsState()
    val isWide = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    NavigationSuiteScaffold(
        layoutType = if (isWide) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
        containerColor = MaterialTheme.colorScheme.surface,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationRailContainerColor = MaterialTheme.colorScheme.surface
        ),
        navigationSuiteItems = {
            TABS.forEach { (tab, icon, label) ->
                item(
                    selected = !state.isPlayerOpen && state.selectedTab == tab,
                    onClick = { component.onTabClicked(tab) },
                    icon = { Icon(icon, contentDescription = stringResource(label)) },
                    label = { Text(stringResource(label)) }
                )
            }
            item(
                selected = state.isPlayerOpen,
                onClick = component::onPlayerClicked,
                icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(Res.string.player)) },
                label = { Text(stringResource(Res.string.player)) }
            )
        }
    ) {
        TabContent(
            component = component,
            state = state,
            playerState = playerState,
            onPlayPause = { playerViewModel.onIntent(PlayerIntent.PlayPause) },
        )

        AnimatedVisibility(
            visible = state.isPlayerOpen,
            enter = slideInVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it },
            exit = slideOutVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it }
        ) {
            PlayerScreen(onBackClick = component::onBackClicked)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun TabContent(
    component: RootComponent,
    state: NavigationState,
    playerState: PlayerState,
    onPlayPause: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
    var isMiniPlayerShown by remember { mutableStateOf(true) }
    val hideMiniPlayerOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -SCROLL_THRESHOLD) isMiniPlayerShown = false
                if (available.y > SCROLL_THRESHOLD) isMiniPlayerShown = true
                return Offset.Zero
            }
        }
    }
    val pane = when {
        state.episode != null -> ListDetailPaneScaffoldRole.Extra
        state.podcast != null -> ListDetailPaneScaffoldRole.Detail
        else -> ListDetailPaneScaffoldRole.List
    }
    LaunchedEffect(state.selectedTab, pane) {
        isMiniPlayerShown = true
        navigator.navigateTo(pane)
    }
    val showMiniPlayer = playerState.currentEpisode != null && isMiniPlayerShown && !state.isPlayerOpen

    Scaffold(
        contentWindowInsets = WindowInsets(),
        modifier = Modifier.nestedScroll(hideMiniPlayerOnScroll),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = { ListPane(component, state.selectedTab, isPlayerVisible = showMiniPlayer) },
                detailPane = { DetailPane(component, state.podcast) },
                extraPane = {
                    state.episode?.let {
                        EpisodeDetailScreen(episodeId = it.episodeId, onBackClick = component::onBackClicked)
                    }
                }
            )
            AnimatedVisibility(
                visible = showMiniPlayer,
                enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.Standard)) { it },
                exit = slideOutVertically(tween(Motion.MEDIUM, easing = Motion.Standard)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MiniPlayerBar(playerState, onPlayPause = onPlayPause, onClick = component::onPlayerClicked)
            }
        }
    }
}

@Composable
private fun ListPane(component: RootComponent, tab: Tab, isPlayerVisible: Boolean) {
    when (tab) {
        Tab.Library -> LibraryScreen(isPlayerVisible = isPlayerVisible, onPodcastClick = component::onPodcastSelected)
        Tab.Search -> SearchScreen(onEpisodeClick = component::onEpisodeSelected)
        Tab.Downloads -> DownloadedEpisodesScreen(onEpisodeClick = component::onEpisodeSelected)
    }
}

@Composable
private fun DetailPane(component: RootComponent, podcast: Detail.Podcast?) {
    if (podcast == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.select_podcast), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    PodcastDetailScreen(
        podcastId = podcast.podcastId,
        onBackClick = component::onBackClicked,
        onEpisodeClick = { id, _ -> component.onEpisodeSelected(id, podcast.podcastId) }
    )
}

@Composable
private fun MiniPlayerBar(playerState: PlayerState, onPlayPause: () -> Unit, onClick: () -> Unit) {
    val episode = playerState.currentEpisode ?: return
    val duration = playerState.duration
    MiniPlayer(
        title = episode.title,
        subtitle = episode.podcastTitle,
        imageUrl = episode.imageUrl,
        isPlaying = playerState.isPlaying,
        isLoading = playerState.isBuffering,
        progress = if (duration != null && duration > 0) playerState.position.toFloat() / duration else 0f,
        onPlayPause = onPlayPause,
        onClick = onClick
    )
}

/** Scroll distance, in pixels per frame, that hides or shows the mini player; ignores jitter. */
private const val SCROLL_THRESHOLD = 1f

private val TABS: List<Triple<Tab, ImageVector, StringResource>> = listOf(
    Triple(Tab.Library, Icons.Rounded.Home, Res.string.library_title),
    Triple(Tab.Search, Icons.Rounded.Search, Res.string.search),
    Triple(Tab.Downloads, Icons.Rounded.DownloadDone, Res.string.downloads),
)
