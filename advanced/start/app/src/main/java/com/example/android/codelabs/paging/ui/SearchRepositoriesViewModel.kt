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
import com.example.android.codelabs.paging.data.GithubRepository
import com.example.android.codelabs.paging.model.Repo
import com.example.android.codelabs.paging.model.RepoSearchResult
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

    /**
     * Stream of immutable states representative of the UI.
     */
//    val state: LiveData<UiState>

    /**
     * Processor of side effects from the UI which in turn feedback into [state]
     */
//    val accept: (UiAction) -> Unit

    val state1: StateFlow<UiState1>

    val accept1: (UiAction1) -> Unit

    val pagingDataFlow: Flow<PagingData<Repo>>


//    init {
//        val queryLiveData =
//            MutableLiveData(savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY)
//
//        state = queryLiveData
//            .distinctUntilChanged()
//            .switchMap { queryString ->
//                liveData {
//                    val uiState = repository.getSearchResultStream(queryString)
//                        .map {
//                            UiState(
//                                query = queryString,
//                                searchResult = it
//                            )
//                        }
//                        .asLiveData(Dispatchers.Main)
//                    emitSource(uiState)
//                }
//            }
//
//        accept = { action ->
//            when (action) {
//                is UiAction.Search -> queryLiveData.postValue(action.query)
//                is UiAction.Scroll -> if (action.shouldFetchMore) {
//                    val immutableQuery = queryLiveData.value
//                    if (immutableQuery != null) {
//                        viewModelScope.launch {
//                            repository.requestMore(immutableQuery)
//                        }
//                    }
//                }
//            }
//        }
//    }

    init {
        val initialQuery: String = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle.get(LAST_QUERY_SCROLLED) ?: DEFAULT_QUERY
        val actionStateFlow = MutableSharedFlow<UiAction1>()

        val searches = actionStateFlow
            .filterIsInstance<UiAction1.Search>()
            .distinctUntilChanged()
            .onStart { emit(UiAction1.Search(query = initialQuery)) }

        val queriesScrolled = actionStateFlow
            .filterIsInstance<UiAction1.Scroll>()
            .distinctUntilChanged()
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
            .onStart { emit(UiAction1.Scroll(currentQuery = lastQueryScrolled)) }

        @Suppress("OPT_IN_USAGE")
        pagingDataFlow = searches
            .flatMapLatest { searchRepo(queryString = it.query) }
            .cachedIn(viewModelScope)

        state1 = combine(
            searches,
            queriesScrolled,
            ::Pair
        ).map { (search, scroll) ->
            UiState1(
                query = search.query,
                lastQueryScrolled = scroll.currentQuery,
                // If the search query matches the scroll query, the user has scrolled
                hasNotScrolledForCurrentSearch = search.query != scroll.currentQuery
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = UiState1()
            )

        accept1 = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }

    }

    private fun searchRepo(queryString: String): Flow<PagingData<Repo>> =
        repository.searchResultStream1(queryString)

    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state1.value.query
        savedStateHandle[LAST_QUERY_SCROLLED] = state1.value.lastQueryScrolled

        super.onCleared()
    }
}

private val UiAction.Scroll.shouldFetchMore
    get() = visibleItemCount + lastVisibleItemPosition + VISIBLE_THRESHOLD >= totalItemCount

sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(
        val visibleItemCount: Int,
        val lastVisibleItemPosition: Int,
        val totalItemCount: Int
    ) : UiAction()
}

sealed class UiAction1 {
    data class Search(val query: String) : UiAction1()
    data class Scroll(val currentQuery: String) : UiAction1()

}

data class UiState(
    val query: String,
    val searchResult: RepoSearchResult
)

data class UiState1(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasNotScrolledForCurrentSearch: Boolean = false
)

private const val VISIBLE_THRESHOLD = 5
private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val LAST_QUERY_SCROLLED: String = "last_query_scrolled"
private const val DEFAULT_QUERY = "Android"