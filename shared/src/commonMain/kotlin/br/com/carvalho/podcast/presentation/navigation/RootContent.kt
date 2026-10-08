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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Podcasts
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
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.CompositionLocalProvider
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
import br.com.carvalho.podcast.core.designsystem.LocalMiniPlayerInset
import br.com.carvalho.podcast.core.designsystem.Motion
import br.com.carvalho.podcast.core.designsystem.Sizes
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.component.MiniPlayer
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.library_title
import br.com.carvalho.podcast.core.ui.generated.resources.episodes_tab
import br.com.carvalho.podcast.core.ui.generated.resources.select_podcast
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailScreen
import br.com.carvalho.podcast.feature.library.presentation.LibraryOrderScreen
import br.com.carvalho.podcast.feature.library.presentation.LibraryScreen
import br.com.carvalho.podcast.feature.player.presentation.PlayerIntent
import br.com.carvalho.podcast.feature.player.presentation.PlayerScreen
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailScreen
import br.com.carvalho.podcast.feature.search.presentation.SearchScreen
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import br.com.carvalho.podcast.core.util.NetworkMonitor

/** Draws [RootComponent.state]: tabs, the list/detail/extra panes of the selected tab, mini player and player. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RootContent(component: RootComponent) {
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.playerState.collectAsState()
    val state by component.state.subscribeAsState()
    val networkMonitor: NetworkMonitor = koinInject()
    LaunchedEffect(component) {
        if (!networkMonitor.isOnline()) component.onOpenedOffline()
    }
    val isWide = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    // The player and "Organize library" cover the tabs too (ADR 0005): the bar is for moving between tabs only.
    // Opening the player grows the mini player's cover into the player's, and minimizing shrinks it back (24.2).
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val sharedCover: @Composable (AnimatedVisibilityScope) -> Modifier = { coverShared(it) }
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
                        selected = state.selectedTab == tab,
                        onClick = { component.onTabClicked(tab) },
                        icon = { Icon(icon, contentDescription = stringResource(label)) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        ) {
            TabContent(
                component = component,
                state = state,
                playerState = playerState,
                onPlayPause = { playerViewModel.onIntent(PlayerIntent.PlayPause) },
                sharedCover = sharedCover,
            )
        }

        AnimatedVisibility(
            visible = state.isOrganizingLibrary,
            enter = slideInVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it },
            exit = slideOutVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it }
        ) {
            LibraryOrderScreen(onBack = component::onBackClicked)
        }

        AnimatedVisibility(
            visible = state.isPlayerOpen,
            enter = slideInVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it },
            exit = slideOutVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it }
        ) {
            PlayerScreen(onBackClick = component::onBackClicked, artworkModifier = sharedCover(this))
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
    sharedCover: @Composable (AnimatedVisibilityScope) -> Modifier,
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
            // Screens move their messages and buttons above the mini player drawn over them.
            val miniPlayerInset = if (showMiniPlayer) Sizes.miniPlayerHeight else 0.dp
            CompositionLocalProvider(LocalMiniPlayerInset provides miniPlayerInset) {
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    listPane = { ListPane(component, state.selectedTab) },
                    detailPane = { DetailPane(component, state.podcast) },
                    extraPane = {
                        state.episode?.let {
                            EpisodeDetailScreen(episodeId = it.episodeId, onBackClick = component::onBackClicked)
                        }
                    }
                )
            }
            AnimatedVisibility(
                visible = showMiniPlayer,
                enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.Standard)) { it },
                exit = slideOutVertically(tween(Motion.MEDIUM, easing = Motion.Standard)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MiniPlayerBar(
                    playerState,
                    onPlayPause = onPlayPause,
                    onClick = component::onPlayerClicked,
                    artworkModifier = sharedCover(this),
                )
            }
        }
    }
}

@Composable
private fun ListPane(component: RootComponent, tab: Tab) {
    when (tab) {
        Tab.Library -> LibraryScreen(
            onOrganize = component::onOrganizeLibrary,
            onPodcastClick = component::onPodcastSelected,
        )
        Tab.Episodes -> SearchScreen(
            onEpisodeClick = component::onEpisodeSelected,
            onPodcastClick = component::onPodcastSelected,
        )
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
private fun MiniPlayerBar(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    artworkModifier: Modifier,
) {
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
        onClick = onClick,
        artworkModifier = artworkModifier,
    )
}

private const val SHARED_COVER_KEY = "player-cover"

/** The cover that the mini player and the player share, moving in step with the player sliding up. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.coverShared(visibility: AnimatedVisibilityScope): Modifier = Modifier.sharedElement(
    sharedContentState = rememberSharedContentState(SHARED_COVER_KEY),
    animatedVisibilityScope = visibility,
    boundsTransform = { _, _ -> tween(Motion.LONG, easing = Motion.Emphasized) },
)

/** Scroll distance, in pixels per frame, that hides or shows the mini player; ignores jitter. */
private const val SCROLL_THRESHOLD = 1f

private val TABS: List<Triple<Tab, ImageVector, StringResource>> = listOf(
    Triple(Tab.Library, Icons.Rounded.Home, Res.string.library_title),
    Triple(Tab.Episodes, Icons.Rounded.Podcasts, Res.string.episodes_tab),
)
