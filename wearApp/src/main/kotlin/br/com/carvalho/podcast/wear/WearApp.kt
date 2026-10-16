package br.com.carvalho.podcast.wear

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

private const val HOME = "home"
private const val PLAYER = "player"
private const val POPULAR = "popular"
internal const val PODCAST_ID = "id"
internal const val PODCAST_ROUTE = "podcast/{$PODCAST_ID}"

/** A podcast's id is its feed address, slashes and all: encoded, or the route would not match it. */
internal fun podcastRoute(podcastId: String) = "podcast/${Uri.encode(podcastId)}"

/** The watch's screens (19.9a); a swipe from the left edge goes back. */
@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()
    WearTheme {
        AppScaffold {
            SwipeDismissableNavHost(navController = navController, startDestination = HOME) {
                composable(HOME) {
                    HomeScreen(
                        onPlayer = { navController.navigate(PLAYER) },
                        onPopular = { navController.navigate(POPULAR) },
                        onPodcast = { navController.navigate(podcastRoute(it)) },
                    )
                }
                composable(PODCAST_ROUTE) { entry ->
                    val podcastId = entry.arguments?.getString(PODCAST_ID) ?: return@composable
                    PodcastScreen(podcastId, onPlaying = { navController.navigate(PLAYER) })
                }
                composable(PLAYER) { NowPlayingScreen() }
                composable(POPULAR) { PopularScreen() }
            }
        }
    }
}
