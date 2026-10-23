package br.com.carvalho.podcast.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
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

    private fun MediaItem.contentStyle(): Int? =
        mediaMetadata.extras?.getInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE)

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
}
