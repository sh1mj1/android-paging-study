package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.api.RepoSearchResponse
import com.example.android.codelabs.paging.data.GithubRepository.Companion.NETWORK_PAGE_SIZE
import com.example.android.codelabs.paging.model.Repo
import retrofit2.HttpException
import java.io.IOException


class GithubPagingSource(
    private val service: GithubService,
    private val query: String,
) : PagingSource<Int, Repo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val curPage: Int = params.key ?: START_PAGE
        val loadSize: Int = params.loadSize
        val apiQuery: String = "$query $IN_QUALIFIER"
        return try {
            val response: RepoSearchResponse = service.searchRepos(apiQuery, curPage, loadSize)
            val repos: List<Repo> = response.items
            val prePage: Int? = curPage.toPrePage()
            val nextPage: Int? = curPage.toNextPage(loadSize, isEmpty = { repos.isEmpty() })

            LoadResult.Page(
                data = repos,
                prevKey = prePage,
                nextKey = nextPage
            )
        } catch (exception: IOException) {
            println("로그 : IOException : ${exception.message}")
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            println("로그 : HttpException : ${exception.message}")
            return LoadResult.Error(exception)
        }
    }

    // 리프레시 요청 시 해당 RefreshKey 팩토리 함수를 호출하여 새로운 키를 생성한다.
    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        // anchorPosition은 가장 최근에 접근한 position을 나타낸다.
        val anchorPosition: Int = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchorPosition) ?: return null
        // 1) 이전 페이지 번호가 있으면, 이전 키를 통해 현재 페이지에 해당하는 position을 찾아낸다.
        closestPage.prevKey?.let { prePage -> return prePage + 1 }?.also { println("로그 : preKey : $it") }
        // 2) 다음 페이지 번호를 통해 현재 페이지에 해당하는 position을 찾아낸다.
        closestPage.nextKey?.let { nextPage -> return nextPage - 1 }.also { println("로그 : nextKey : $it") }
        // 3) 이전 페이지 번호와 다음 페이지 번호가 모두 없으면 null을 반환한다.
        return null
    }

    private fun Int.toPrePage(): Int? = if (this == START_PAGE) null else this - 1

    private fun Int.toNextPage(pageSize: Int, isEmpty: () -> Boolean): Int? =
        if (isEmpty()) null else this + pageOffset(pageSize)

    /**
     * 첫 요청 때는 page 3개 만큼 요청하고, 그 이후에는 1개만큼 요청하므로 page offset을 계산한다.
     * */
    private fun pageOffset(pageSize: Int): Int = pageSize / NETWORK_PAGE_SIZE

    companion object {
        private const val START_PAGE = 1
    }
}
