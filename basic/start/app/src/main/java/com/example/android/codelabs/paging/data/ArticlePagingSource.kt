package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.max


class ArticlePagingSource : PagingSource<Int, Article>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        // 첫번째 로드라면 STARTING_KEY 로 페이징을 시작한다.
        val start = params.key ?: STARTING_KEY

        // 로드할 아이템의 수만큼 로드한다. params.loaddSize 가 로드할 아이템 수를 나타낸다
        val range = start.until(start + params.loadSize)

        if (start != STARTING_KEY) delay(LOAD_DELAY_MILLIS)

        return LoadResult.Page(
            data = range.map { number ->
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
                else -> validKey(key = range.first - params.loadSize)
            },
            nextKey = range.last + 1,
        )
    }

    // 무효화 이후에 다음 페이징 소스의 첫 로드에서 사용되는 키를 반환한다.
    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        // 우리는  anchor position 에서 가장 가까운 아이템을 찾아서 그것의 id - (state.config.pageSize / 2) 를 반환한다. as a buffer
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

/**
 *
 * PagingSource 는 load() 와 getRefreshKey() 두 가지 함수를 구현해야 합니다.
 * load() 함수는 사용자가 스크롤 할 때 추가 데이터를 비동기적으로 가져 오도록 페이징 라이브러리에 의해 호출됩니다.
 * LoadParams 객체에는 로드 작업과 관련된 정보가 포함되어 있습니다.
 *
 * * 로드 할 페이지의 키 - 이 함수가 처음 호출되는 경우 LoadParams.key는 null이 됩니다. 이 경우 초기 페이지 키를 정의해야합니다.
 * * 로드 크기 - 로드 할 요청 항목 수입니다.
 *
 *
 * load() 함수는 LoadResult를 반환합니다. LoadResult 는 sealed class 로 되어 잇습니다. LoadResult는 다음 중 하나 일 수 있습니다.
 *
 * * LoadResult.Page, 결과가 성공한 경우.
 * * LoadResult.Error, 오류가 발생한 경우.
 * * LoadResult.Invalid: 페이징 소스가 더 이상 결과의 무결성을 보장할 수 없으므로 무효화되어야하는 경우.
 *
 *
 * LoadResult.page 는 세가지 필수 인수가 있습니다.
 *
 * * data: 가져온 항목의 목록.
 * * prevKey: 현재 페이지 뒤에 항목을 가져 오려면 load() 메서드에서 사용하는 키입니다.
 * * nextKey: 현재 페이지 뒤에 항목을 가져 오려면 load() 메서드에서 사용하는 키입니다.
 * * ... and two optional ones
 *
 * * itemsBefore: 로드 된 데이터 앞에 표시 할 플레이스 홀더 수입니다.
 * * itemsAfter: 로드 된 데이터 뒤에 표시 할 플레이스 홀더 수입니다.
 *
 *
 * 우리의 로딩 키는 Article.id 필드입니다. Article ID는 각 게시물마다 1씩 증가하기 때문에 키로 사용할 수 있습니다.
 * 즉, 기사 ID는 연속적으로 1씩 증가하는 정수입니다.
 *
 * nextKey 혹은 prevKey 는 해당 방향으로 더 이상 데이터를로드 할 수없는 경우 null입니다.
 * 우리의 경우 prevKey에 대해 다음과 같이합니다.
 *
 * 만약 startKey 가 STARTING_KEY 와 같다면, 우리는 null 을 반환합니다. 왜냐하면 이 키 뒤에 더 이상 아이템들을 로드 할 수 없기 때문입니다.
 * 만약 startKey 가 STARTING_KEY 와 다르다면, 우리는 목록에서 첫 번째 항목을 가져 와서 그 뒤에 LoadParams.loadSize 만큼 로드해서 항상 STARTING_KEY보다 작은 키를 반환하지 않도록 합니다.
 * ensureValidKey() 메서드를 정의하여 이 작업을 수행합니다.
 * 이 함수는 페이징 키가 유효한지 확인합니다.
 *
 * ---
 *
 * Next we need to implement getRefreshKey().
 * This method is called when the Paging library needs to reload items for the UI because the data in its backing PagingSource has changed.
 * This situation where the underlying data for a PagingSource has changed and needs to be updated in the UI is called invalidation.
 * When invalidated, the Paging Library creates a new PagingSource to reload the data, and informs the UI by emitting new PagingData. We'll learn more about invalidation in a later section.
 *
 * When loading from a new PagingSource, getRefreshKey() is called to provide the key
 * the new PagingSource should start loading with to make sure the user does not lose their current place in the list after the refresh.
 *
 * Invalidation in the paging library occurs for one of two reasons:
 *
 * You called refresh() on the PagingAdapter.
 * You called invalidate() on the PagingSource.
 *
 * 반환된 키는 LoadParams argument 를 통해서 새로운 PagingSource의 load() 메서드에 전달됩니다.
 * 무효화 이후에 아이템이 이동하는 것을 방지하기 위해, 반환된 키가 화면을 채울만큼의 아이템을 로드하는지 확인해야합니다.
 * 이렇게 하면 새로운 아이템 세트가 무효화된 데이터에 있던 아이템을 포함할 가능성이 높아져 현재 스크롤 위치를 유지하는데 도움이 됩니다.
 *
 *
 * 위 getRefreshKey 코드에서 PagingState.anchorPosition 을 사용했습니다.
 * 만약 페이징 라이브러리가 더 많은 데이터를 어떻게 가져올 수 있는지 궁금하다면, 이것이 답입니다!
 * UI가 PagingData에서 항목을 읽으려고 시도하면 특정 인덱스에서 읽으려고 시도합니다.
 * 데이터가 읽히면 그 데이터가 UI에 표시됩니다. 그러나 데이터가 없으면 페이징 라이브러리는 데이터를 가져와 실패한 읽기 요청을 충족해야 한다는 것을 알게됩니다.
 * 데이터가 성공적으로 읽힌 가장 최근 인덱스는 anchorPosition입니다.
 * 새로 고칠 때, anchorPosition에 가장 가까운 Article의 키를 가져와서 로드 키로 사용합니다.
 * 이렇게 하면 새로운 PagingSource에서 다시 로드를 시작할 때 이미 로드된 항목이 포함된 항목 세트가 포함되어 부드럽고 일관된 사용자 경험을 보장합니다.
 * 이렇게 하면 새로운 PagingSource를 완전히 정의했습니다. 다음 단계는 UI에 연결하는 것입니다.
 *
 *
 *
 *
 */