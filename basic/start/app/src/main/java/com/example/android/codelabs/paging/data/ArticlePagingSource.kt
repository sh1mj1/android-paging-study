package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.time.LocalDateTime
import kotlin.math.max

private val firstArticleCreatedTime = LocalDateTime.now()

class ArticlePagingSource : PagingSource<Int, Article>() {

    // ui를 새로고침할 때 사용
    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val article = state.closestItemToPosition(anchorPosition) ?: return null
        return ensureValidKey(key = article.id - (state.config.pageSize / 2))
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        val start = params.key ?: STARTING_KEY
        val range = start.until(start + params.loadSize)

        return LoadResult.Page(
            data = range.map { number ->
                Article(
                    id = number,
                    title = "Article $number",
                    description = "This describes article $number",
                    created = firstArticleCreatedTime.minusDays(number.toLong())
                )
            },
            prevKey = when (start) {
                STARTING_KEY -> null
                else -> ensureValidKey(key = range.first - params.loadSize)
            },
            nextKey = start + params.loadSize
        )
    }

    private fun ensureValidKey(key: Int) = max(STARTING_KEY, key)

    companion object {
        private const val STARTING_KEY = 0

    }
}
