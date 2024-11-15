package com.example.android.codelabs.paging.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.data.GithubRepository.Companion.NETWORK_PAGE_SIZE
import com.example.android.codelabs.paging.model.Repo
import retrofit2.HttpException
import java.io.IOException

class GithubPagingSource(
    private val service: GithubService,
    private val query: String,
) : PagingSource<Int, Repo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val position = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = "$query $IN_QUALIFIER"

        Log.d("yenny", "Loading data for query: $apiQuery, position: $position, load size: ${params.loadSize}")
        return try {
            val response = service.searchRepos(apiQuery, position, params.loadSize).items
            val nextKey =
                if (response.isEmpty()) {
                    null
                } else {
                    position + (params.loadSize / NETWORK_PAGE_SIZE) // 중복된 항목이 로드되지 않도록 보장하기 위함
                }
            LoadResult.Page(
                data = response,
                prevKey = if (position == GITHUB_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey,
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        Log.d("yenny", "getRefreshKey")
        // Anchor position : 사용자가 스크롤을 통해 목록에서 최근에 본 위치
        // state.closestPageToPosition(anchorPosition) : 주어진 anchorPosition에 가장 가까운 페이지를 찾기
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    private companion object {
        const val GITHUB_STARTING_PAGE_INDEX = 1
    }
}
