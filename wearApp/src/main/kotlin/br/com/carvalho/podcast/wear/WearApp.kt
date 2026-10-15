package br.com.carvalho.podcast.wear

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

private const val HOME = "home"
private const val PLAYER = "player"
private const val POPULAR = "popular"
private const val PODCAST = "podcast"
private const val PODCAST_ID = "id"

/** The watch's screens (19.9a); a swipe from the left edge goes back. */
@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()
    MaterialTheme {
        AppScaffold {
            SwipeDismissableNavHost(navController = navController, startDestination = HOME) {
                composable(HOME) {
                    HomeScreen(
                        onPlayer = { navController.navigate(PLAYER) },
                        onPopular = { navController.navigate(POPULAR) },
                        onPodcast = { navController.navigate("$PODCAST/$it") },
                    )
                }
                composable("$PODCAST/{$PODCAST_ID}") { entry ->
                    val podcastId = entry.arguments?.getString(PODCAST_ID) ?: return@composable
                    PodcastScreen(podcastId, onPlaying = { navController.navigate(PLAYER) })
                }
                composable(PLAYER) { NowPlayingScreen() }
                composable(POPULAR) { PopularScreen() }
            }
        }
    }
}
