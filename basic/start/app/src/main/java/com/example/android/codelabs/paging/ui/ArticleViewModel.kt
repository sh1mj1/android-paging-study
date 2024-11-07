/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.android.codelabs.paging.data.Article
import com.example.android.codelabs.paging.data.ArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the [ArticleActivity] screen.
 * The ViewModel works with the [ArticleRepository] to get the data.
 */
class ArticleViewModel(
    repository: ArticleRepository,
) : ViewModel() {

    val items: Flow<PagingData<Article>> = Pager(
        config = PagingConfig(pageSize = ITEMS_PER_PAGE, enablePlaceholders = false),
        pagingSourceFactory = { repository.articlePagingSource() }
    )
        .flow
        .cachedIn(viewModelScope)

}

private const val ITEMS_PER_PAGE = 50

/*
Note: The pagingSourceFactory lambda should always return a brand new PagingSource when invoked as PagingSource instances are not reusable.
PagingSource 인스턴스는 재사용할 수 없기 때문에, 각 호출마다 새로운 PagingSource를 반환해야 합니다. 이는 PagingSource가 데이터 로드 상태를 유지하고, 데이터 소스가 변경될 때마다 새로운 인스턴스를 생성하여 일관된 데이터 로드를 보장하기 위함입니다.

PagingSource는 데이터 로드 중에 상태를 유지하고, 데이터 소스가 변경되면 무효화됩니다. 따라서, 새로운 데이터 로드를 위해 항상 새로운 PagingSource 인스턴스를 반환해야 합니다.

Note: PagingData Flows는 cold가 아니기 때문에 stateIn() 또는 sharedIn() 연산자를 사용하지 마십시오.

또한 Flow에 대한 작업을 수행하는 경우, map 또는 filter와 같은 작업을 수행한 후에 cachedIn을 호출하여 이러한 작업을 다시 트리거할 필요가 없도록 해야 합니다.


Note: `PagingData`는 `RecyclerView`에 표시할 데이터의 업데이트를 포함하는 독립적인 타입입니다.
각 `PagingData`의 발행은 완전히 독립적이며, 기본 데이터 세트의 변경으로 인해 백업 `PagingSource`가 무효화되면 단일 쿼리에 대해 여러 `PagingData` 인스턴스가 발행될 수 있습니다.
따라서, `PagingData`의 `Flow`는 다른 `Flow`와 독립적으로 노출되어야 합니다.


Note: PagingData Flow 를 다른 Flows 와 mix 하거나 combine 하지 마세요.
각 PagingData 의 발행은 독립적으로 소비되어야 합니다.

 */

