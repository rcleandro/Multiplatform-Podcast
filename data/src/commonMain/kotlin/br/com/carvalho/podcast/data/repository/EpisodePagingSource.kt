package br.com.carvalho.podcast.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.domain.model.Episode

class EpisodePagingSource(
    private val episodeDao: EpisodeDao,
    private val podcastId: String? = null,
    private val query: String? = null
) : PagingSource<Int, Episode>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> {
        val offset = params.key ?: 0
        return try {
            val rows = when {
                !query.isNullOrBlank() -> episodeDao.searchPaged(query, params.loadSize, offset)
                !podcastId.isNullOrBlank() -> episodeDao.getByPodcastPaged(podcastId, params.loadSize, offset)
                else -> episodeDao.getAllPaged(params.loadSize, offset)
            }
            LoadResult.Page(
                data = rows.map { it.toDomain() },
                prevKey = if (offset == 0) null else offset - params.loadSize,
                nextKey = if (rows.size < params.loadSize) null else offset + params.loadSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episode>) =
        state.anchorPosition?.let { (it - (state.config.pageSize / 2)).coerceAtLeast(0) }
}
