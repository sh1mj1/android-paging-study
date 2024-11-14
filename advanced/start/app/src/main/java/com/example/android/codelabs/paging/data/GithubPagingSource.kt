package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.model.Repo
import retrofit2.HttpException
import java.io.IOException

class GithubPagingSource(
    private val service: GithubService,
    private val query: String,
) : PagingSource<Int, Repo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val position = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = "$query ${GithubService.IN_QUALIFIER}"
        return try {
            val response = service.searchRepos(apiQuery, position, params.loadSize)
            val githubRepos = response.items
            val nextKey = if (githubRepos.isEmpty()) {
                null
            } else {
                position + (params.loadSize / GithubRepository.NETWORK_PAGE_SIZE)
            }

            LoadResult.Page(
                data = githubRepos,
                prevKey = if (position == GITHUB_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey,
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }

    companion object {
        private const val GITHUB_STARTING_PAGE_INDEX = 1
    }

}
