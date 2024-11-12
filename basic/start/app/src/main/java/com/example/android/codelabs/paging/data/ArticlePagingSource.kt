package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.time.LocalDateTime

class ArticlePagingSource : PagingSource<Int, Article>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        val start = params.key ?: STARTING_KEY
        val range = start.until(start + params.loadSize)
        val data =
            range.map { number ->
                Article(
                    id = number,
                    title = "Article $number",
                    description = "This describes article $number",
                    created = LocalDateTime.now().minusDays(number.toLong()),
                )
            }

        val prevKey =
            when (start) {
                STARTING_KEY -> null
                else -> ensureValidKey(range.first - params.loadSize)
            }

        val nextKey = range.last + 1

        return LoadResult.Page(
            data = data,
            prevKey = prevKey,
            nextKey = nextKey,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val article = state.closestItemToPosition(anchorPosition) ?: return null
        return ensureValidKey(article.id - (state.config.pageSize / 2))
    }

    private fun ensureValidKey(key: Int) = maxOf(STARTING_KEY, key)

    companion object {
        private const val STARTING_KEY = 0
    }
}
