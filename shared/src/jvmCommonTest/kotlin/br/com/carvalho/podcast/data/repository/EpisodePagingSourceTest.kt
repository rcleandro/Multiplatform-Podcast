package br.com.carvalho.podcast.data.repository

import androidx.paging.PagingSource
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpisodePagingSourceTest {

    private val episodeDao = mockk<EpisodeDao>()
    private val podcastId = "p1"

    @Test
    fun `load returns success page on initial load`() = runTest {
        val pagingSource = EpisodePagingSource(episodeDao, podcastId = podcastId)
        val episodes = createEpisodeEntities(3)

        coEvery { episodeDao.getByPodcastPaged(podcastId, 10, 0) } returns episodes

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
        // nextKey should be null if rows.size < loadSize
        assertNull(result.nextKey)
    }

    @Test
    fun `load returns nextKey when more data is available`() = runTest {
        val pagingSource = EpisodePagingSource(episodeDao, podcastId = podcastId)
        val episodes = createEpisodeEntities(10)

        coEvery { episodeDao.getByPodcastPaged(podcastId, 10, 0) } returns episodes

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
        val episodes = createEpisodeEntities(2)

        coEvery { episodeDao.searchPaged(query, 10, 0) } returns episodes

        val params = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 10,
            placeholdersEnabled = false
        )

        val result = pagingSource.load(params)

        assertTrue(result is PagingSource.LoadResult.Page)
        assertEquals(2, result.data.size)
        assertEquals("e0", result.data[0].id)
    }

    @Test
    fun `load returns error on exception`() = runTest {
        val pagingSource = EpisodePagingSource(episodeDao)
        val exception = RuntimeException("DB Error")

        coEvery { episodeDao.getAllPaged(any(), any()) } throws exception

        val params = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 10,
            placeholdersEnabled = false
        )

        val result = pagingSource.load(params)

        assertTrue(result is PagingSource.LoadResult.Error)
        assertEquals(exception, result.throwable)
    }

    private fun createEpisodeEntities(count: Int): List<EpisodeEntity> {
        return List(count) { i ->
            EpisodeEntity(
                id = "e$i",
                podcastId = podcastId,
                podcastTitle = "Podcast",
                title = "Episode $i",
                description = "Desc",
                audioUrl = "url",
                imageUrl = "img",
                duration = 1000L,
                publishDate = i.toLong(),
                isPlayed = false,
                playbackPosition = 0L,
                isDownloaded = false,
                fileSize = null
            )
        }
    }
}
