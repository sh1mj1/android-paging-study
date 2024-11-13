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

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.android.codelabs.paging.Injection
import com.example.android.codelabs.paging.databinding.ActivitySearchRepositoriesBinding
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch

class SearchRepositoriesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySearchRepositoriesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // get the view model
        val viewModel = ViewModelProvider(this, Injection.provideViewModelFactory(owner = this))
            .get(SearchRepositoriesViewModel::class.java)

        // add dividers between RecyclerView's row items
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.list.addItemDecoration(decoration)

        // bind the state
        binding.bindState(
            uiState = viewModel.state,
            pagingData = viewModel.pagingDataFlow,
            uiEvent = viewModel.accept
        )
    }

    /**
     * Binds the [UiState] provided  by the [SearchRepositoriesViewModel] to the UI,
     * and allows the UI to feed back user actions to it.
     *
     * [ReposLoadStateAdapter]: [LoadStateAdapter를] 확장하여 header와 footer에 표시할 뷰를 정의합니다.
     * [header]: 이전 페이지를 로드하거나, 데이터 로드 중 오류가 발생했을 때 나타납니다. retry()를 호출해 다시 시도할 수 있습니다.
     * [footer]: 다음 페이지를 로드할 때 상태를 표시하며, 로딩 중일 때나 오류가 발생했을 때 나타납니다.
     */
    private fun ActivitySearchRepositoriesBinding.bindState(
        uiState: StateFlow<UiState>,
        pagingData: Flow<PagingData<Repo>>,
        uiEvent: (UiEvent) -> Unit
    ) {
        val repoAdapter = ReposAdapter()
        list.adapter = repoAdapter.withLoadStateHeaderAndFooter(
            header = ReposLoadStateAdapter { repoAdapter.retry() },
            footer = ReposLoadStateAdapter { repoAdapter.retry() }
        )
        bindSearch(
            uiState = uiState,
            onQueryChanged = uiEvent
        )
        bindList(
            repoAdapter = repoAdapter,
            uiState = uiState,
            pagingData = pagingData,
            onScrollChanged = uiEvent
        )
    }

    private fun ActivitySearchRepositoriesBinding.bindSearch(
        uiState: StateFlow<UiState>,
        onQueryChanged: (UiEvent.Search) -> Unit
    ) {
        searchRepo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }
        searchRepo.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            uiState
                .map { it.query }
                .distinctUntilChanged()
                .collect(searchRepo::setText)
        }
    }

    private fun ActivitySearchRepositoriesBinding.updateRepoListFromInput(onQueryChanged: (UiEvent.Search) -> Unit) {
        searchRepo.text.trim().let {
            if (it.isNotEmpty()) {
                list.scrollToPosition(0)
                onQueryChanged(UiEvent.Search(query = it.toString()))
            }
        }
    }

    /**
     *
     * [CombinedLoadStates]를 사용하면 세 가지 유형의 로드 작업의 로드 상태를 가져올 수 있습니다.
     *
     * [CombinedLoadStates.refresh]: PagingData를 처음 로드할 때의 로드 상태를 나타냅니다.
     * [CombinedLoadStates.prepend]: 목록의 시작 부분에서 데이터를 로드하는 작업의 로드 상태를 나타냅니다.
     * [CombinedLoadStates.append]: 목록의 끝에서 데이터를 로드하는 작업의 로드 상태를 나타냅니다.
     * */
    private fun ActivitySearchRepositoriesBinding.bindList(
        repoAdapter: ReposAdapter,
        uiState: StateFlow<UiState>,
        pagingData: Flow<PagingData<Repo>>,
        onScrollChanged: (UiEvent.Scroll) -> Unit
    ) {
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) onScrollChanged(UiEvent.Scroll(currentQuery = uiState.value.query))
            }
        })
        val isNotLoading = repoAdapter.loadStateFlow
            // Refresh 요청이 완료되었을 때만 반응합니다.
            .distinctUntilChangedBy { it.source.refresh }
            // Only react to cases where REFRESH completes i.e., NotLoading.
            // 데이터 로드 완료
            .map { it.source.refresh is LoadState.NotLoading }

        val hasNotScrolled = uiState
            .map { it.hasScrolled.not() }
            .distinctUntilChanged()
        val shouldScrollToTop = combine(
            isNotLoading,
            hasNotScrolled,
            Boolean::and
        ).distinctUntilChanged()
        isNotLoading.onEach { println("로그: isNotLoading: $it") }.launchIn(lifecycleScope)
        hasNotScrolled.onEach { println("로그: hasNotScrolled: $it") }.launchIn(lifecycleScope)
        shouldScrollToTop.onEach { println("로그: shouldScrollToTop: $it") }.launchIn(lifecycleScope)

        lifecycleScope.launch {
            pagingData.collectLatest(repoAdapter::submitData)
        }

        lifecycleScope.launch {
            shouldScrollToTop.collect { shouldScroll ->
                if (shouldScroll) list.scrollToPosition(0)
            }
        }
    }
}