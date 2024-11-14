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

class SearchRepositoriesViewModel(
    private val repository: GithubRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val state1: StateFlow<UiState>
    val accept1: (UiAction) -> Unit
    val pagingDataFlow: Flow<PagingData<UiModel>>

    init {
        val initialQuery: String = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle.get(LAST_QUERY_SCROLLED) ?: DEFAULT_QUERY
        val actionStateFlow = MutableSharedFlow<UiAction>()

        val searches = actionStateFlow
            .filterIsInstance<UiAction.Search>()
            .distinctUntilChanged()
            .onStart { emit(UiAction.Search(query = initialQuery)) }

        val queriesScrolled = actionStateFlow
            .filterIsInstance<UiAction.Scroll>()
            .distinctUntilChanged()
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
            .onStart { emit(UiAction.Scroll(currentQuery = lastQueryScrolled)) }

        @Suppress("OPT_IN_USAGE")
        pagingDataFlow = searches
            .flatMapLatest { searchRepo(queryString = it.query) }
            .cachedIn(viewModelScope)

        state1 = combine(
            searches,
            queriesScrolled,
            ::Pair
        ).map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.currentQuery,
                // If the search query matches the scroll query, the user has scrolled
                hasNotScrolledForCurrentSearch = search.query != scroll.currentQuery
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = UiState()
            )

        accept1 = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }

    }

    private fun searchRepo(queryString: String): Flow<PagingData<UiModel>> =
        repository.searchResultStream(queryString)
            .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
            .map { pagingData ->
                pagingData.insertSeparators { before, after ->
                    when {
                        (after == null) -> return@insertSeparators null
                        (before == null) -> return@insertSeparators UiModel.SeparatorItem("${after.roundedStarCount}0k🔺 stars")
                        (before.roundedStarCount > after.roundedStarCount) -> {
                            if (after.roundedStarCount >= 1) {
                                UiModel.SeparatorItem("${after.roundedStarCount}0k🔺 stars")
                            } else {
                                UiModel.SeparatorItem("10k🔻 stars")
                            }
                        }

                        else -> return@insertSeparators null
                    }
                }
            }


    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state1.value.query
        savedStateHandle[LAST_QUERY_SCROLLED] = state1.value.lastQueryScrolled

        super.onCleared()
    }
}

sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(val currentQuery: String) : UiAction()

}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasNotScrolledForCurrentSearch: Boolean = false
)

private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val LAST_QUERY_SCROLLED: String = "last_query_scrolled"
private const val DEFAULT_QUERY = "Android"

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}

private val UiModel.RepoItem.roundedStarCount: Int
    get() = this.repo.stars / 10_000