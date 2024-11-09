package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.max


class ArticlePagingSource : PagingSource<Int, Article>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        val start = params.key ?: STARTING_KEY

        val rangeItemLoaded = start.until(start + params.loadSize)


        if (start != STARTING_KEY) delay(LOAD_DELAY_MILLIS)

        return LoadResult.Page(
            data = rangeItemLoaded.map { number ->
                Article(
                    id = number, // 게시물 id 로 1씩 증가하는 숫자
                    title = "Article $number",
                    description = "This describes article $number",
                    created = firstArticleCreatedTime.minusDays(number.toLong()),
                )
            },
            // STARTING_KEY 이전에는 아이템을 로드하지 않도록 한다
            prevKey = when (start) {
                STARTING_KEY -> null
                else -> validKey(key = rangeItemLoaded.first - params.loadSize)
            },
            nextKey = rangeItemLoaded.last + 1,
        )
    }

    // 무효화 이후에 다음 페이징 소스의 첫 로드에서 사용되는 키를 반환한다.
    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        // 우리는 anchor position 에서 가장 가까운 아이템을 찾아서 그것의 id - (state.config.pageSize / 2) 를 반환한다. as a buffer
        // anchor position: Most recently accessed index in the list, including placeholders.
        val anchorPosition = state.anchorPosition ?: return null
        val article: Article = state.closestItemToPosition(anchorPosition) ?: return null
        return validKey(key = article.id - (state.config.pageSize / 2))
    }

    // 페이징 키가 [STARTING_KEY] 보다 작지 않도록 보장
    private fun validKey(key: Int): Int = max(STARTING_KEY, key)
}

private const val STARTING_KEY = 0
private val firstArticleCreatedTime = LocalDateTime.now()
private const val LOAD_DELAY_MILLIS = 3_000L
