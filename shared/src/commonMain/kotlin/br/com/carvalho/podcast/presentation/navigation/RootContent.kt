package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.designsystem.LocalMiniPlayerInset
import br.com.carvalho.podcast.core.designsystem.Motion
import br.com.carvalho.podcast.core.designsystem.PaneLayout
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.component.MiniPlayer
import br.com.carvalho.podcast.core.designsystem.currentPaneLayout
import br.com.carvalho.podcast.core.designsystem.currentWindowIsShort
import br.com.carvalho.podcast.core.designsystem.icons.VideoLibrary
import br.com.carvalho.podcast.core.designsystem.icons.automirrored.FeaturedPlayList
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.episodes_tab
import br.com.carvalho.podcast.core.ui.generated.resources.library_title
import br.com.carvalho.podcast.core.ui.generated.resources.select_podcast
import br.com.carvalho.podcast.core.util.NetworkMonitor
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.feature.discover.presentation.DiscoverScreen
import br.com.carvalho.podcast.feature.discover.presentation.PodcastSuggestions
import br.com.carvalho.podcast.feature.episode.presentation.EpisodeDetailScreen
import br.com.carvalho.podcast.feature.library.presentation.LibraryOrderScreen
import br.com.carvalho.podcast.feature.library.presentation.LibraryScreen
import br.com.carvalho.podcast.feature.player.presentation.PlayerIntent
import br.com.carvalho.podcast.feature.player.presentation.PlayerScreen
import br.com.carvalho.podcast.feature.player.presentation.PlayerViewModel
import br.com.carvalho.podcast.feature.player.presentation.QueueScreen
import br.com.carvalho.podcast.feature.player.presentation.TranscriptScreen
import br.com.carvalho.podcast.feature.podcast.presentation.PodcastDetailScreen
import br.com.carvalho.podcast.feature.search.presentation.SearchScreen
import br.com.carvalho.podcast.feature.settings.presentation.ListeningStatsScreen
import br.com.carvalho.podcast.feature.settings.presentation.SettingsScreen
import br.com.carvalho.podcast.presentation.platformVideoSurface
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Draws [RootComponent.state]: tabs, the list/detail/extra panes of the selected tab, mini player and player. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RootContent(component: RootComponent, keyboardShortcuts: Boolean = false) {
    val playerViewModel: PlayerViewModel = koinViewModel()
    // The position moves every 500 ms while playing (23.2): the tabs only follow what the mini player shows, and the
    // position itself is read where the progress line is drawn, so nothing recomposes as it moves.
    val playerState = playerViewModel.playerState.collectAsState()
    val nowPlaying by remember { derivedStateOf(structuralEqualityPolicy()) { NowPlaying.of(playerState.value) } }
    val progress = remember { { playerState.value.progressFraction } }
    val state by component.state.subscribeAsState()
    val networkMonitor: NetworkMonitor = koinInject()
    LaunchedEffect(component) {
        if (!networkMonitor.isOnline()) component.onOpenedOffline()
    }
    // Bar on a compact window, rail from 600 dp (21.1) and on a low one, where a bar would take a quarter of the
    // height, like a flip phone's cover screen (21.5).
    val hasRail = currentPaneLayout() != PaneLayout.COMPACT || currentWindowIsShort()

    // The player, "Organize library" and "Add podcast" cover the tabs (ADR 0005): the bar only moves between tabs.
    // Opening the player grows the mini player's cover into the player's, and minimizing shrinks it back (24.2).
    val shortcuts = if (keyboardShortcuts) appShortcuts(component, playerViewModel) else Modifier
    SharedTransitionLayout(modifier = Modifier.fillMaxSize().then(shortcuts)) {
        val sharedCover: @Composable (AnimatedVisibilityScope) -> Modifier = { coverShared(it) }
        NavigationSuiteScaffold(
            layoutType = if (hasRail) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
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
                nowPlaying = nowPlaying,
                progress = progress,
                onPlayPause = { playerViewModel.onIntent(PlayerIntent.PlayPause) },
                sharedCover = sharedCover,
            )
        }

        Overlay(visible = state.isOrganizingLibrary) { LibraryOrderScreen(onBack = component::onBackClicked) }
        AddPodcastOverlay(state, component)
        SettingsOverlays(state, component)
        Overlay(visible = state.isPlayerOpen) {
            PlayerScreen(
                onBackClick = component::onBackClicked,
                artworkModifier = sharedCover(this),
                onQueueClick = component::onQueueClicked,
                onTranscriptClick = component::onTranscriptClicked,
                videoSurface = platformVideoSurface,
            )
        }
        Overlay(visible = state.isQueueOpen) { QueueScreen(onBack = component::onBackClicked) }
        Overlay(visible = state.isTranscriptOpen) { TranscriptScreen(onBack = component::onBackClicked) }
    }
}

/** "Add podcast" over the tabs: the directory, a link opened in the app (18.4) or podcasts like one (19.6). */
@Composable
private fun AddPodcastOverlay(state: NavigationState, component: RootComponent) {
    Overlay(visible = state.isAddingPodcast) {
        DiscoverScreen(
            onBack = component::onBackClicked,
            session = state.addPodcastSession,
            link = state.podcastLink,
            onLinkHandled = component::onPodcastLinkHandled,
            onOpenPodcast = component::onPodcastSelected,
            similarTo = state.similarTo,
            onSimilarHandled = component::onSimilarHandled,
        )
    }
}

/** Settings over the tabs, and the listening stats (19.5) over Settings. */
@Composable
private fun SettingsOverlays(state: NavigationState, component: RootComponent) {
    Overlay(visible = state.isShowingSettings) {
        SettingsScreen(onBack = component::onBackClicked, onOpenStats = component::onOpenListeningStats)
    }
    Overlay(visible = state.isShowingStats) { ListeningStatsScreen(onBack = component::onBackClicked) }
}

/** A screen that slides up over the tabs: the player, the queue, "Organize library", "Add podcast" and Settings. */
@Composable
private fun Overlay(visible: Boolean, content: @Composable AnimatedVisibilityScope.() -> Unit) = AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it },
    exit = slideOutVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it },
    content = content,
)

/** Space plays or pauses, the arrows skip and Escape goes back, wherever the app is (21.7). */
@Composable
private fun appShortcuts(component: RootComponent, playerViewModel: PlayerViewModel): Modifier =
    Modifier.keyboardShortcuts { shortcut ->
        when (shortcut) {
            Shortcut.PLAY_PAUSE -> playerViewModel.onIntent(PlayerIntent.PlayPause)
            Shortcut.SKIP_BACKWARD -> playerViewModel.onIntent(PlayerIntent.SkipBackward)
            Shortcut.SKIP_FORWARD -> playerViewModel.onIntent(PlayerIntent.SkipForward)
            Shortcut.BACK -> component.onBackClicked()
        }
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun TabContent(
    component: RootComponent,
    state: NavigationState,
    nowPlaying: NowPlaying?,
    progress: () -> Float,
    onPlayPause: () -> Unit,
    sharedCover: @Composable (AnimatedVisibilityScope) -> Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>(
        scaffoldDirective = paneDirective(currentWindowAdaptiveInfo()),
    )
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
    val showMiniPlayer = nowPlaying != null && isMiniPlayerShown && !state.isPlayerOpen

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
                    nowPlaying,
                    progress,
                    onPlayPause = onPlayPause,
                    onClick = component::onPlayerClicked,
                    onQueueClick = component::onQueueClicked,
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
            onAddPodcast = component::onAddPodcast,
            onOpenSettings = component::onOpenSettings,
            // Features do not depend on each other (ADR 0003): the app puts discovery's suggestions in the library.
            emptyLibrary = { PodcastSuggestions(onAddPodcast = component::onAddPodcast) },
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
        onEpisodeClick = { id, _ -> component.onEpisodeSelected(id, podcast.podcastId) },
        onSimilarClick = component::onShowSimilar,
    )
}

@Composable
private fun MiniPlayerBar(
    nowPlaying: NowPlaying?,
    progress: () -> Float,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    onQueueClick: () -> Unit,
    artworkModifier: Modifier,
) {
    val playing = nowPlaying ?: return
    MiniPlayer(
        title = playing.title,
        subtitle = playing.podcastTitle,
        imageUrl = playing.imageUrl,
        isPlaying = playing.isPlaying,
        isLoading = playing.isLoading,
        progress = progress,
        onPlayPause = onPlayPause,
        onClick = onClick,
        onQueueClick = onQueueClick,
        artworkModifier = artworkModifier,
    )
}

/**
 * What the mini player shows of the player, without the position: the episode's fields, not the episode, whose saved
 * progress changes every few seconds too.
 */
private data class NowPlaying(
    val title: String,
    val podcastTitle: String?,
    val imageUrl: String?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
) {
    companion object {
        fun of(state: PlayerState): NowPlaying? = state.currentEpisode?.let {
            NowPlaying(it.title, it.podcastTitle, it.imageUrl, state.isPlaying, state.isBuffering)
        }
    }
}

private val PlayerState.progressFraction: Float
    get() = duration?.takeIf { it > 0 }?.let { position.toFloat() / it } ?: 0f

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
    Triple(Tab.Library, Icons.Rounded.VideoLibrary, Res.string.library_title),
    Triple(Tab.Episodes, Icons.AutoMirrored.Rounded.FeaturedPlayList, Res.string.episodes_tab),
)
