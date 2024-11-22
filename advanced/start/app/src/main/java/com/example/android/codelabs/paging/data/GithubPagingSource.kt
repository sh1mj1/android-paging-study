package com.example.android.codelabs.paging.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.data.GithubRepository.Companion.NETWORK_PAGE_SIZE
import com.example.android.codelabs.paging.model.Repo
import okio.IOException
import retrofit2.HttpException

class GithubPagingSource(
    private val service: GithubService,
    private val query: String
) : PagingSource<Int, Repo>() {
    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        // 새로 고침시 사용할 키
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        // 로드 페이지, 로드 사이즈
        val position = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = "$query $IN_QUALIFIER"

        Log.d("load", "Loading data for query: $apiQuery, position: $position, load size: ${params.loadSize}")
        return try {
            val response = service.searchRepos(apiQuery, position, params.loadSize)
            val repos = response.items
            val nextKey = if (repos.isEmpty()) {
                null
            } else {
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = repos,
                prevKey = if (position == GITHUB_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }

    companion object {
        private const val GITHUB_STARTING_PAGE_INDEX = 1
    }
}
