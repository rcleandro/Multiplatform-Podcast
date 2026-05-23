package br.com.carvalho.podcast.presentation.navigation

import br.com.carvalho.podcast.core.util.AppLogger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

private const val TAG = "RootComponent"
class RootComponentImpl(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Library,
        handleBackButton = true,
        childFactory = { config, _ -> createChild(config) }
    )

    private fun createChild(config: Config): RootComponent.Child =
        when (config) {
            is Config.Library -> {
                AppLogger.d(TAG, "Navigating to Library")
                RootComponent.Child.Library(Unit)
            }
            is Config.PodcastDetail -> {
                AppLogger.d(TAG, "Navigating to Podcast Detail: ${config.podcastId}")
                RootComponent.Child.PodcastDetail(config.podcastId)
            }
            is Config.EpisodeDetail -> {
                AppLogger.d(TAG, "Navigating to Episode Detail: ${config.episodeId}")
                RootComponent.Child.EpisodeDetail(config.episodeId, config.podcastId)
            }
            is Config.DownloadedEpisodes -> {
                AppLogger.d(TAG, "Navigating to Downloads")
                RootComponent.Child.DownloadedEpisodes
            }
            is Config.Player -> {
                AppLogger.d(TAG, "Navigating to Player")
                RootComponent.Child.Player
            }
            is Config.Search -> {
                AppLogger.d(TAG, "Navigating to Search")
                RootComponent.Child.Search
            }
        }

    override fun onLibraryTabClicked() {
        selectTab(Config.Library)
    }

    override fun onSearchTabClicked() {
        selectTab(Config.Search)
    }

    override fun onDownloadsTabClicked() {
        selectTab(Config.DownloadedEpisodes)
    }

    override fun onPlayerTabClicked() {
        selectTab(Config.Player)
    }

    private fun selectTab(config: Config) {
        AppLogger.d(TAG, "Action: Tab selected: $config")
        navigation.navigate { stack ->
            val cleanStack = stack.filterNot { it is Config.PodcastDetail || it is Config.EpisodeDetail }
            if (cleanStack.contains(config)) {
                cleanStack.filterNot { it == config } + config
            } else {
                cleanStack + config
            }
        }
    }

    fun onPodcastSelected(podcastId: String) {
        AppLogger.d(TAG, "Action: Podcast selected: $podcastId")
        navigation.navigate { stack ->
            stack.filterNot { it is Config.PodcastDetail || it is Config.EpisodeDetail }
                .let { it + Config.PodcastDetail(podcastId) }
        }
    }

    fun onEpisodeSelected(episodeId: String, podcastId: String) {
        AppLogger.d(TAG, "Action: Episode selected: $episodeId (Podcast: $podcastId)")
        
        navigation.navigate { stack ->
            val cleanStack = stack.filterNot { 
                (it is Config.PodcastDetail && it.podcastId == podcastId) || 
                (it is Config.EpisodeDetail && it.episodeId == episodeId)
            }
            cleanStack + Config.PodcastDetail(podcastId) + Config.EpisodeDetail(episodeId, podcastId)
        }
    }

    fun onBackClicked() {
        AppLogger.d(TAG, "Action: Back clicked")
        navigation.pop()
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Library : Config
        @Serializable
        data class PodcastDetail(val podcastId: String) : Config
        @Serializable
        data class EpisodeDetail(val episodeId: String, val podcastId: String) : Config
        @Serializable
        data object DownloadedEpisodes : Config
        @Serializable
        data object Player : Config
        @Serializable
        data object Search : Config
    }
}
