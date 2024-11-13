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

package com.example.android.codelabs.paging.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.android.codelabs.paging.data.GithubRepository
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the [SearchRepositoriesActivity] screen.
 * The ViewModel works with the [GithubRepository] to get the data.
 */
class SearchRepositoriesViewModel(
    private val repository: GithubRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val state: StateFlow<UiState>

    val pagingDataFlow: Flow<PagingData<UiModel>>

    val accept: (UiEvent) -> Unit

    private val UiModel.RepoItem.roundedStarCount: Int
        get() = this.repo.stars / 10_000

    init {
        val initialQuery: String = savedStateHandle[LAST_SEARCH_QUERY] ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle[LAST_SEARCH_QUERY] ?: DEFAULT_QUERY
        val uiEvent = MutableSharedFlow<UiEvent>()

        val searchUiEvent = uiEvent
            .filterIsInstance<UiEvent.Search>()
            .distinctUntilChanged()
            .onStart { emit(UiEvent.Search(query = initialQuery)) }

        val scrollUiEvent = uiEvent
            .filterIsInstance<UiEvent.Scroll>()
            .distinctUntilChanged()
            // 이건 마지막으로 스크롤된 쿼리를 캐싱하는 동안 플로우를 "hot" 유지하기 위해 공유됩니다.
            // otherwise each flatMapLatest invocation would lose the last query scrolled,
            // 반면 각 flatMapLatest 호출은 마지막으로 스크롤된 마지막 쿼리를 잃어버릴 것입니다.
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
            .onStart { emit(UiEvent.Scroll(currentQuery = lastQueryScrolled)) }

        pagingDataFlow = searchUiEvent
            .flatMapLatest { repoPagingStream(queryString = it.query) }
            .cachedIn(viewModelScope)

        state = combine(
            searchUiEvent,
            scrollUiEvent,
            ::Pair
        ).map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.currentQuery,
                // 마지막으로 사용자의 검색어와 현재 검색어가 같으면 사용자가 스크롤했다고 판단합니다.
                hasScrolled = search.query == scroll.currentQuery
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = UiState()
        )

        accept = { event: UiEvent ->
            viewModelScope.launch { uiEvent.emit(event) }
        }
    }

    private fun repoPagingStream(queryString: String): Flow<PagingData<UiModel>> =
        repository.repoPagingStream(queryString)
            .map { pagingData -> pagingData.map { UiModel.RepoItem(repo = it) } }
            .map {
                it.insertSeparators { before, after ->
                    if (after == null) return@insertSeparators null
                    if (before == null) return@insertSeparators UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")

                    if (before.roundedStarCount <= after.roundedStarCount) return@insertSeparators null
                    if (after.roundedStarCount >= 1) {
                        UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                    } else {
                        UiModel.SeparatorItem("< 10.000+ stars")
                    }
                }
            }

    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state.value.query
        savedStateHandle[LAST_QUERY_SCROLLED] = state.value.lastQueryScrolled
        super.onCleared()
    }
}

sealed class UiEvent {
    data class Search(val query: String) : UiEvent()
    data class Scroll(val currentQuery: String) : UiEvent()
}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasScrolled: Boolean = false
)

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}

private const val VISIBLE_THRESHOLD = 5
private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val LAST_QUERY_SCROLLED: String = "last_query_scrolled"
private const val DEFAULT_QUERY = "Android"