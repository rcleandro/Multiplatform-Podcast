package br.com.carvalho.podcast.wear

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import br.com.carvalho.podcast.domain.model.Episode

private const val HOME = "home"
private const val PLAYER = "player"
private const val POPULAR = "popular"
private const val QUEUE = "queue"
private const val SPEED = "speed"
private const val TIMER = "timer"
private const val DOWNLOADS = "downloads"
private const val PLAYLISTS = "playlists"
private const val PLAYLIST = "playlist"
private const val PLAYLIST_ID = "playlistId"
private const val EPISODE_ID = "episodeId"
private const val EPISODE_ROUTE = "episode/{$EPISODE_ID}"

/** An episode's id is a hash, but encoded all the same: the route must take whatever the feed gives. */
private fun episodeRoute(episodeId: String) = "episode/${Uri.encode(episodeId)}"
internal const val PODCAST_ID = "id"
internal const val PODCAST_ROUTE = "podcast/{$PODCAST_ID}"

/** A podcast's id is its feed address, slashes and all: encoded, or the route would not match it. */
internal fun podcastRoute(podcastId: String) = "podcast/${Uri.encode(podcastId)}"

/** The watch's screens (19.9a); a swipe from the left edge goes back. */
@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()
    val openActions: (Episode) -> Unit = { navController.navigate(episodeRoute(it.id)) }
    WearTheme {
        AppScaffold {
            SwipeDismissableNavHost(navController = navController, startDestination = HOME) {
                composable(HOME) {
                    HomeScreen(
                        onPlayer = { navController.navigate(PLAYER) },
                        onPopular = { navController.navigate(POPULAR) },
                        onPodcast = { navController.navigate(podcastRoute(it)) },
                        onDownloads = { navController.navigate(DOWNLOADS) },
                        onPlaylists = { navController.navigate(PLAYLISTS) },
                    )
                }
                composable(PODCAST_ROUTE) { entry ->
                    val podcastId = entry.arguments?.getString(PODCAST_ID) ?: return@composable
                    PodcastScreen(podcastId, onPlaying = { navController.navigate(PLAYER) }, onActions = openActions)
                }
                composable(PLAYER) {
                    NowPlayingScreen(
                        onQueue = { navController.navigate(QUEUE) },
                        onSpeed = { navController.navigate(SPEED) },
                        onTimer = { navController.navigate(TIMER) },
                    )
                }
                composable(QUEUE) { QueueScreen() }
                composable(DOWNLOADS) {
                    DownloadsScreen(onPlaying = { navController.navigate(PLAYER) }, onActions = openActions)
                }
                composable(PLAYLISTS) { PlaylistsScreen(onPlaylist = { navController.navigate("$PLAYLIST/$it") }) }
                composable("$PLAYLIST/{$PLAYLIST_ID}") { entry ->
                    val playlistId = entry.arguments?.getString(PLAYLIST_ID)?.toLongOrNull() ?: return@composable
                    PlaylistScreen(playlistId, onPlaying = { navController.navigate(PLAYER) }, onActions = openActions)
                }
                composable(EPISODE_ROUTE) { entry ->
                    val episodeId = entry.arguments?.getString(EPISODE_ID) ?: return@composable
                    EpisodeActionsScreen(episodeId, onDone = navController::popBackStack)
                }
                composable(SPEED) { SpeedScreen(onDone = navController::popBackStack) }
                composable(TIMER) { SleepTimerScreen(onDone = navController::popBackStack) }
                composable(POPULAR) { PopularScreen() }
            }
        }
    }
}
