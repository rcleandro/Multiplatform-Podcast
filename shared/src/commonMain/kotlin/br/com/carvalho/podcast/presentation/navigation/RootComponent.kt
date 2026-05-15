package br.com.carvalho.podcast.presentation.navigation

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Library(val component: Unit) : Child()
        data class PodcastDetail(val podcastId: String) : Child()
        data class EpisodeDetail(val episodeId: String, val podcastId: String) : Child()
        data object DownloadedEpisodes : Child()
        data object Player : Child()
        data object Search : Child()
    }

    fun onLibraryTabClicked()
    fun onSearchTabClicked()
    fun onDownloadsTabClicked()
    fun onPlayerTabClicked()
}
