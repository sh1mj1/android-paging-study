package com.example.android.codelabs.paging.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.local.RepoDatabase
import com.example.android.codelabs.paging.model.RemoteKeys
import com.example.android.codelabs.paging.model.Repo
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class GithubRemoteMediator(
    private val query: String,
    private val service: GithubService,
    private val repoDatabase: RepoDatabase
) : RemoteMediator<Int, Repo>() {

    /**
     * [PagingState]: 이전에 로드된 페이지, 목록에서 가장 최근에 액세스한 색인, 페이징 스트림을 초기화할 때 정의한 [PagingConfig]에 관한 정보를 제공합니다.
     * [LoadType]: 이전에 로드한 데이터의 끝 부분([LoadType.APPEND]) 또는 시작 부분([LoadType.PREPEND])에서 데이터를 로드해야 하는지 또는 데이터를 처음으로 로드하는지([LoadType.REFRESH])를 나타냅니다.
     *
     * ex) 예를 들어 로드 유형이 [LoadType.APPEND]이면 [PagingState]에서 로드된 마지막 항목을 가져옵니다.
     * 이 항목을 기반으로 로드될 다음 페이지를 계산하여 [Repo] 객체의 다음 배치를 로드하는 방법을 확인할 수 있습니다.
     * */
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {
        // load 할 페이지 번호를 결정합니다.
        val page = when (loadType) {
            LoadType.REFRESH -> {
                // 1) 데이터를 처음 로드할 때
                // 2) PagingDataAdapter.refresh()가 호출되는 경우 호출
                val remoteKeys: RemoteKeys? = remoteKeyForCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }

            LoadType.PREPEND -> {
                // 현재 로드된 데이터 세트의 시작 부분에서 데이터를 로드해야 하는 경우
                val remoteKeys: RemoteKeys =
                    remoteKeysForFirstItem(state) ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                val prevKey = remoteKeys.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                prevKey
            }

            LoadType.APPEND -> {
                // 현재 로드된 데이터 세트의 끝 부분에서 데이터를 로드해야 하는 경우
                val remoteKeys: RemoteKeys =
                    remoteKeysForLastItem(state) ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                val nextKey: Int = remoteKeys.nextKey ?: return MediatorResult.Success(
                    endOfPaginationReached = true
                )
                // 다음페이지가 있는 경우
                nextKey
            }
        }

        val apiQuery = "$query $IN_QUALIFIER"

        try {
            val apiResponse = service.searchRepos(apiQuery, page, state.config.pageSize)

            val repos = apiResponse.items
            val endOfPaginationReached = repos.isEmpty()
            repoDatabase.withTransaction {
                // 새로 고침(REFRESH)일 경우 모든 테이블 초기화
                if (loadType == LoadType.REFRESH) {
                    repoDatabase.remoteKeysDao().clearRemoteKeys()
                    repoDatabase.reposDao().clearRepos()
                }
                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = repos.map {
                    RemoteKeys(repoId = it.id, prevKey = prevKey, nextKey = nextKey)
                }
                repoDatabase.remoteKeysDao().insertAll(keys)
                repoDatabase.reposDao().insertAll(repos)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun remoteKeyForCurrentPosition(
        state: PagingState<Int, Repo>
    ): RemoteKeys? {
        // anchorPosition : 사용자가 스크롤한 위치
        val anchorPosition: Int = state.anchorPosition ?: return null
        // 사용자가 스크롤한 위치에 가장 가까운 아이템을 찾습니다.
        val closestItem: Repo = state.closestItemToPosition(anchorPosition) ?: return null

        return repoDatabase.remoteKeysDao().remoteKeysRepoId(closestItem.id)
    }

    private suspend fun remoteKeysForLastItem(state: PagingState<Int, Repo>): RemoteKeys? {
        val pages: List<PagingSource.LoadResult.Page<Int, Repo>> = state.pages
        val lastPageRepos: List<Repo> =
            pages.lastOrNull { it.data.isNotEmpty() }?.data ?: return null
        val lastRepo: Repo = lastPageRepos.lastOrNull() ?: return null

        return repoDatabase.remoteKeysDao().remoteKeysRepoId(lastRepo.id)
    }

    private suspend fun remoteKeysForFirstItem(state: PagingState<Int, Repo>): RemoteKeys? {
        val pages: List<PagingSource.LoadResult.Page<Int, Repo>> = state.pages
        val firstPageRepos: List<Repo> =
            pages.firstOrNull { it.data.isNotEmpty() }?.data ?: return null
        val firstRepo: Repo = firstPageRepos.firstOrNull() ?: return null

        return repoDatabase.remoteKeysDao().remoteKeysRepoId(firstRepo.id)
    }
}