package br.com.carvalho.podcast.data.repository

import androidx.paging.PagingSource
import br.com.carvalho.podcast.data.local.dao.FakeEpisodeDao
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpisodePagingSourceTest {

    private val episodeDao = FakeEpisodeDao()
    private val podcastId = "p1"

    @Test
    fun `load returns success page on initial load`() = runTest {
        val pagingSource = EpisodePagingSource(episodeDao, podcastId = podcastId)
        val episodes = createEpisodeEntities(3)
        episodeDao.episodes.value = episodes

        val params = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 10,
            placeholdersEnabled = false
        )

        val result = pagingSource.load(params)

        assertTrue(result is PagingSource.LoadResult.Page)
        assertEquals(3, result.data.size)
        assertEquals("e0", result.data[0].id)
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `load returns nextKey when more data is available`() = runTest {
        val pagingSource = EpisodePagingSource(episodeDao, podcastId = podcastId)
        val episodes = createEpisodeEntities(10)
        episodeDao.episodes.value = episodes

        val params = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 10,
            placeholdersEnabled = false
        )

        val result = pagingSource.load(params)

        assertTrue(result is PagingSource.LoadResult.Page)
        assertEquals(10, result.data.size)
        assertEquals(10, result.nextKey)
    }

    @Test
    fun `load handles search query correctly`() = runTest {
        val query = "search"
        val pagingSource = EpisodePagingSource(episodeDao, query = query)
        val episodes = listOf(
            createEpisodeEntity("e1", "search match"),
            createEpisodeEntity("e2", "no match")
        )
        episodeDao.episodes.value = episodes

        val params = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 10,
            placeholdersEnabled = false
        )

        val result = pagingSource.load(params)

        assertTrue(result is PagingSource.LoadResult.Page)
        assertEquals(1, result.data.size)
        assertEquals("e1", result.data[0].id)
    }

    private fun createEpisodeEntities(count: Int): List<EpisodeEntity> {
        return List(count) { i ->
            createEpisodeEntity("e$i", "Episode $i")
        }
    }

    private fun createEpisodeEntity(id: String, title: String) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        podcastTitle = "Podcast",
        title = title,
        description = "Desc",
        audioUrl = "url",
        imageUrl = "img",
        duration = 1000L,
        publishDate = 0L,
        isPlayed = false,
        playbackPosition = 0L,
        isDownloaded = false,
        fileSize = null
    )
}
