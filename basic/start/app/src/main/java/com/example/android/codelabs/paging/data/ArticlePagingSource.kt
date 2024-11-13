package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay

private const val LOAD_DELAY_MILLIS = 3_000L

class ArticlePagingSource : PagingSource<Int, Article>() {
    /**
     * [load] : 사용자가 스크롤할 때 표시할 더 많은 데이터를 비동기식으로 가져오기 위해 Paging 라이브러리에서 호출
     * [LoadParams] : 로드할 페이지의 키: load()가 처음 호출되는 경우 LoadParams.key는 null입니다. 여기서는 초기 페이지 키를 정의해야 합니다.
     * 이 프로젝트에서는 기사 ID를 키로 사용합니다. 초기 페이지 키의 ArticlePagingSource 파일 상단에 STARTING_KEY 상수 0도 추가해 보겠습니다.
     *
     * */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        // load()가 처음 호출되는 경우 LoadParams.key는 null
        // 따라서, 초기 페이지 key를 정의 해주어야 한다.
        val start = params.key ?: STARTING_KEY
        // params.loadSize 만큼 범위를 잡는다.
        val loadRange = start until (start + params.loadSize)
        println("ArticlePagingSource - params : ${params.loadSize}")
        println("ArticlePagingSource - params : ${params.key}")
        println("ArticlePagingSource - params : ${params.placeholdersEnabled}")
        if (start != STARTING_KEY) delay(LOAD_DELAY_MILLIS)

        return LoadResult.Page(
            data = loadRange.map { number ->
                Article(
                    id = number,
                    title = "Article $number",
                    description = "This describes article $number",
                    created = firstArticleCreatedTime.minusDays(number.toLong())
                )
            },
            prevKey = when (start) {
                STARTING_KEY -> null
                else -> ensureValidKey(loadRange.first - params.loadSize)
            },
            nextKey = loadRange.last + 1
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        println("ArticlePagingSource - getRefreshKey() - anchorPosition - $anchorPosition")
        val article = state.closestItemToPosition(anchorPosition) ?: return null
        println("ArticlePagingSource - getRefreshKey() - article - $article")
        return ensureValidKey(key = article.id - (state.config.pageSize / 2))
    }

    /**
     * 페이징 키가 유효한지 검증하는 함수
     * */
    private fun ensureValidKey(key: Int) = maxOf(STARTING_KEY, key)

    companion object {
        private const val STARTING_KEY = 0
    }
}