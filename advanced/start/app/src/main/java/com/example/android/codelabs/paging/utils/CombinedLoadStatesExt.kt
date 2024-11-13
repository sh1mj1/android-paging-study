package com.example.android.codelabs.paging.utils

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.runningFold

fun Flow<CombinedLoadStates>.asRemotePresentationState(): Flow<RemotePresentationState> =
    runningFold(RemotePresentationState.INITIAL) { state, loadState ->
        when (state) {
            RemotePresentationState.PRESENTED, RemotePresentationState.INITIAL -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                else -> state
            }

            RemotePresentationState.REMOTE_LOADING -> when (loadState.source.refresh) {
                is LoadState.Loading -> RemotePresentationState.SOURCE_LOADING
                else -> state
            }

            RemotePresentationState.SOURCE_LOADING -> when (loadState.source.refresh) {
                is LoadState.NotLoading -> RemotePresentationState.PRESENTED
                else -> state
            }
        }
    }.distinctUntilChanged()

enum class RemotePresentationState {
    INITIAL, REMOTE_LOADING, SOURCE_LOADING, PRESENTED
}