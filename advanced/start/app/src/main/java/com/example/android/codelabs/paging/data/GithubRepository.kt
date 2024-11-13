/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.local.RepoDatabase
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.flow.Flow

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
const val GITHUB_STARTING_PAGE_INDEX = 1

/**
 * Repository class that works with local and remote data sources.
 */
class GithubRepository(
    private val service: GithubService,
    private val database: RepoDatabase
) {
    /**
     * Paging3 는 메모리 캐시 및 스크롤이 끝 지점에 도달하면 자동으로 load 해주기 때문에 기존 코드 삭제해도 됨
     * */
    @OptIn(ExperimentalPagingApi::class)
    fun repoPagingStream(query: String): Flow<PagingData<Repo>> {
        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory = { database.reposDao().reposByName(dbQuery) }
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = GithubRemoteMediator(
                query,
                service,
                database
            ),
            pagingSourceFactory = pagingSourceFactory,
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}
