package br.com.carvalho.podcast.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaTreeTest {
    private val repository = FakePodcastRepository().apply {
        podcasts.value = listOf(podcast("a", "Alpha"), podcast("b", "Beta"))
        episodes.value = listOf(episode("a1", "a"), episode("a2", "a"), episode("b1", "b"))
    }
    private val tree = MediaTree(repository) { "Library" }

    @Test
    fun theRootHoldsTheLibraryAsAGrid() = runTest {
        val library = tree.children(ROOT_ID).single()

        assertEquals(LIBRARY_ID, library.mediaId)
        assertEquals("Library", library.mediaMetadata.title)
        assertEquals(true, library.mediaMetadata.isBrowsable)
        assertEquals(MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM, library.contentStyle())
    }

    @Test
    fun theLibraryListsEveryPodcastAsABrowsableFolder() = runTest {
        val podcasts = tree.children(LIBRARY_ID)

        assertEquals(listOf("podcast_a", "podcast_b"), podcasts.map { it.mediaId })
        assertEquals(listOf("Alpha", "Beta"), podcasts.map { it.mediaMetadata.title.toString() })
        assertTrue(podcasts.all { it.mediaMetadata.isBrowsable == true && it.mediaMetadata.isPlayable == false })
        assertEquals(MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM, podcasts.first().contentStyle())
        // Alone on the screen, they need no title of their own.
        assertNull(podcasts.first().group())
    }

    @Test
    fun theLibraryOpensWithWhatWasLeftHalfwayAsTheAppsDoes() = runTest {
        repository.episodes.value = listOf(
            episode("a1", "a"),
            episode("a2", "a", position = STOPPED_MS),
            episode("a3", "a", isPlayed = true, position = STOPPED_MS),
            episode("b1", "b", position = STOPPED_MS).copy(explicit = true),
        )
        val texts = mapOf(R.string.continue_listening to "Continue listening", R.string.library_podcasts to "Podcasts")
        val rule = ExplicitRule(hidesExplicit = true)
        val tree = MediaTree(repository, rule = { rule }) { texts[it] ?: "Library" }

        val library = tree.children(LIBRARY_ID)

        assertEquals(listOf("a2", "podcast_a", "podcast_b"), library.map { it.mediaId })
        assertEquals(true, library.first().mediaMetadata.isPlayable)
        assertEquals(listOf("Continue listening", "Podcasts", "Podcasts"), library.map { it.group() })
    }

    @Test
    fun aPodcastListsItsPlayableEpisodesWithTheirAudio() = runTest {
        val episodes = tree.children("podcast_a")

        assertEquals(listOf("a1", "a2"), episodes.map { it.mediaId })
        assertEquals("https://cdn.example.com/a1.mp3", episodes.first().localConfiguration?.uri.toString())
        assertEquals("Alpha", episodes.first().mediaMetadata.artist.toString())
        assertTrue(episodes.all { it.mediaMetadata.isPlayable == true })
        assertEquals(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE, episodes.first().mediaMetadata.mediaType)
    }

    @Test
    fun anUnknownParentHasNoChildren() = runTest {
        assertEquals(emptyList<MediaItem>(), tree.children("something_else"))
    }

    @Test
    fun itemsAreFoundByTheirIds() = runTest {
        assertEquals("Library", tree.item(LIBRARY_ID)?.mediaMetadata?.title.toString())
        assertNull(tree.item(LIBRARY_ID)?.contentStyle())
        assertEquals("Beta", tree.item("podcast_b")?.mediaMetadata?.title.toString())
        assertEquals("Alpha", tree.item("a2")?.mediaMetadata?.artist.toString())
        assertNull(tree.item("podcast_missing"))
        assertNull(tree.item("missing"))
    }

    @Test
    fun aVoiceSearchPlaysWhatWasAskedFor() = runTest {
        repository.episodes.value = listOf(
            episode("a1", "a", publishDate = 1),
            episode("a2", "a", publishDate = 2, isPlayed = true),
            episode("a3", "a", publishDate = 3, position = 60),
            episode("b1", "b", title = "Pix explained"),
        )

        assertEquals(listOf("a3"), tree.search("  ").map { it.mediaId })
        assertEquals(listOf("a3"), tree.search("alpha").map { it.mediaId })
        assertEquals(listOf("b1"), tree.search("pix").map { it.mediaId })
        assertEquals("Beta", tree.search("pix").single().mediaMetadata.artist.toString())
        assertEquals(emptyList<MediaItem>(), tree.search("nothing like it"))
    }

    @Test
    fun underTheEcaDigitalNothingExplicitOrHiddenIsListedFoundOrPlayed() = runTest {
        repository.podcasts.value = listOf(podcast("a", "Alpha"), podcast("b", "Beta").copy(explicit = true))
        repository.episodes.value = listOf(
            episode("a1", "a"),
            episode("a2", "a").copy(explicit = true),
            episode("a3", "a", title = "Reported"),
            episode("b1", "b", title = "Pix explained"),
        )
        val rule = ExplicitRule(hidesExplicit = true, explicitPodcasts = setOf("b"), hiddenEpisodes = setOf("a3"))
        val tree = MediaTree(repository, rule = { rule }) { "Library" }

        assertEquals(listOf("podcast_a"), tree.children(LIBRARY_ID).map { it.mediaId })
        assertEquals(listOf("a1"), tree.children("podcast_a").map { it.mediaId })
        assertEquals(emptyList<MediaItem>(), tree.children("podcast_b"))
        assertNull(tree.item("podcast_b"))
        assertNull(tree.item("a2"))
        assertNull(tree.item("b1"))
        assertEquals(emptyList<MediaItem>(), tree.search("pix"))
        assertEquals(emptyList<MediaItem>(), tree.search("beta"))
        assertEquals(emptyList<MediaItem>(), tree.search("reported"))
    }

    @Test
    fun aDownloadedEpisodePlaysFromItsFileAndKeepsItsAddressForCasting() = runTest {
        repository.episodes.value = listOf(episode("a1", "a").copy(isDownloaded = true), episode("a2", "a"))
        val tree = MediaTree(repository, localPath = { "/downloads/$it.mp3" }) { "Library" }

        val (downloaded, streamed) = tree.children("podcast_a")

        assertEquals("file:///downloads/a1.mp3", downloaded.localConfiguration?.uri.toString())
        assertEquals("https://cdn.example.com/a1.mp3", downloaded.requestMetadata.mediaUri.toString())
        assertEquals("https://cdn.example.com/a2.mp3", streamed.localConfiguration?.uri.toString())
    }

    @Test
    fun anEpisodeStartsWhereItStoppedOrWhereItsPodcastsJingleEnds() = runTest {
        repository.podcasts.value = listOf(
            podcast("a", "Alpha").copy(playback = PodcastPlayback(skipIntroSeconds = INTRO_SECONDS)),
            podcast("b", "Beta"),
        )
        repository.episodes.value = listOf(
            episode("a1", "a", position = STOPPED_MS),
            episode("a2", "a", isPlayed = true, position = STOPPED_MS),
            episode("b1", "b"),
        )

        assertEquals(STOPPED_MS, tree.startPositionMs("a1"))
        // A heard one starts over, after the jingle.
        assertEquals(INTRO_SECONDS * MILLIS_PER_SECOND, tree.startPositionMs("a2"))
        assertEquals(0L, tree.startPositionMs("b1"))
        assertEquals(0L, tree.startPositionMs("missing"))
    }

    private fun MediaItem.contentStyle(): Int? =
        mediaMetadata.extras?.getInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE)

    private fun MediaItem.group(): String? =
        mediaMetadata.extras?.getString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE)

    private fun podcast(id: String, title: String) = Podcast(
        id = id, title = title, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "https://feeds.example.com/$id", siteUrl = null, lastUpdated = 0,
        isSubscribed = true,
    )

    private fun episode(
        id: String,
        podcastId: String,
        title: String = id,
        publishDate: Long = 0,
        isPlayed: Boolean = false,
        position: Long = 0,
    ) = Episode(
        id = id, podcastId = podcastId, title = title, description = null, audioUrl = "https://cdn.example.com/$id.mp3",
        imageUrl = null, duration = 0, publishDate = publishDate, isPlayed = isPlayed, playbackPosition = position,
        isDownloaded = false,
        fileSize = null,
    )

    private companion object {
        const val STOPPED_MS = 600_000L
        const val INTRO_SECONDS = 30
        const val MILLIS_PER_SECOND = 1_000L
    }
}
