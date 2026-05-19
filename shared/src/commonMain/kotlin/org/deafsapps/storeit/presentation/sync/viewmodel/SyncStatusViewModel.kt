package org.deafsapps.storeit.presentation.sync.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.CatchUpSignedInSyncUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.RetryPendingSyncUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.sync.model.SyncStatusUiState
import org.koin.core.annotation.Factory

private const val STOP_SHARE_TIMEOUT_MILLIS = 5_000L

@Factory
class SyncStatusViewModel internal constructor(
    coroutineScope: CoroutineScope?,
    private val restoreAccountSessionUseCase: RestoreAccountSessionUseCaseType,
    private val catchUpSignedInSyncUseCase: CatchUpSignedInSyncUseCaseType,
    private val retryPendingSyncUseCase: RetryPendingSyncUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {
    private val loadRequests = MutableSharedFlow<SyncStatusLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateChanges = MutableSharedFlow<SyncStatusStateChange>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val state: StateFlow<SyncStatusViewModelState> = merge(
        loadRequests
            .onStart { emit(value = SyncStatusLoadRequest.Refresh) }
            .flatMapLatest { request -> request.toStateChanges() },
        stateChanges,
    ).runningFold(
        initial = SyncStatusViewModelState.initial(),
        operation = { currentState, change -> change.reduce(state = currentState) },
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_TIMEOUT_MILLIS),
        initialValue = SyncStatusViewModelState.initial(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SyncStatusUiState> = state
        .map { viewModelState -> viewModelState.uiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_TIMEOUT_MILLIS),
            initialValue = state.value.uiState,
        )

    fun refresh() {
        loadRequests.tryEmit(value = SyncStatusLoadRequest.Refresh)
    }

    fun retry() {
        val session = state.value.restoredSession
        if (session == null) {
            refresh()
            return
        }

        viewModelScope.launch {
            stateChanges.emit(value = SyncStatusStateChange.Loading)
            retryPendingSyncUseCase(input = session).fold(
                ifErr = { error ->
                    stateChanges.emit(
                        value = SyncStatusStateChange.SyncStageFailed(
                            session = session,
                            pendingOperationCount = state.value.uiState.pendingOperationCount,
                            error = error,
                        ),
                    )
                },
                ifOk = { stage ->
                    stateChanges.emit(
                        value = SyncStatusStateChange.SyncStageResolved(
                            session = session,
                            result = stage,
                        ),
                    )
                },
            )
        }
    }

    private fun SyncStatusLoadRequest.toStateChanges(): Flow<SyncStatusStateChange> = flow {
        emit(value = SyncStatusStateChange.Loading)
        restoreAccountSessionUseCase(input = Unit).fold(
            ifErr = { error -> emit(value = SyncStatusStateChange.SessionRestoreFailed(error = error)) },
            ifOk = { session ->
                if (session == null) {
                    emit(value = SyncStatusStateChange.SignedOut)
                } else {
                    emitCatchUpSyncStage(session = session)
                }
            },
        )
    }

    private suspend fun FlowCollector<SyncStatusStateChange>.emitCatchUpSyncStage(
        session: AccountSession,
    ) {
        catchUpSignedInSyncUseCase(input = session).fold(
            ifErr = { error ->
                emit(
                    value = SyncStatusStateChange.SyncStageFailed(
                        session = session,
                        pendingOperationCount = state.value.uiState.pendingOperationCount,
                        error = error,
                    ),
                )
            },
            ifOk = { result ->
                emit(
                    value = SyncStatusStateChange.SyncStageResolved(
                        session = session,
                        result = result,
                    ),
                )
            },
        )
    }
}

private data class SyncStatusViewModelState(
    val restoredSession: AccountSession?,
    val uiState: SyncStatusUiState,
) {
    companion object {
        fun initial(): SyncStatusViewModelState = SyncStatusViewModelState(
            restoredSession = null,
            uiState = SyncStatusUiState.getDefault(),
        )
    }
}

private enum class SyncStatusLoadRequest {
    Refresh,
}

private sealed interface SyncStatusStateChange {
    fun reduce(state: SyncStatusViewModelState): SyncStatusViewModelState

    data object Loading : SyncStatusStateChange {
        override fun reduce(state: SyncStatusViewModelState): SyncStatusViewModelState = state
    }

    data object SignedOut : SyncStatusStateChange {
        override fun reduce(state: SyncStatusViewModelState): SyncStatusViewModelState = state.copy(
            restoredSession = null,
            uiState = SyncStatusUiState.getDefault(),
        )
    }

    data class SessionRestoreFailed(
        private val error: DomainError,
    ) : SyncStatusStateChange {
        override fun reduce(state: SyncStatusViewModelState): SyncStatusViewModelState = state.copy(
            restoredSession = null,
            uiState = SyncStatusUiState.getDefault().copy(
                syncStatus = SyncStatus.Failed,
                failureMessage = error.toErrorCause(),
            ),
        )
    }

    data class SyncStageResolved(
        private val session: AccountSession,
        private val result: SyncStageResult,
    ) : SyncStatusStateChange {
        override fun reduce(state: SyncStatusViewModelState): SyncStatusViewModelState = state.copy(
            restoredSession = session,
            uiState = SyncStatusUiState(
                isAuthenticated = true,
                dataMode = result.dataMode,
                syncStatus = result.syncState.status,
                pendingOperationCount = result.syncState.pendingOperationCount,
                failureMessage = result.syncState.failureReason,
            ),
        )
    }

    data class SyncStageFailed(
        private val session: AccountSession,
        private val pendingOperationCount: Int,
        private val error: DomainError,
    ) : SyncStatusStateChange {
        override fun reduce(state: SyncStatusViewModelState): SyncStatusViewModelState = state.copy(
            restoredSession = session,
            uiState = SyncStatusUiState(
                isAuthenticated = true,
                dataMode = state.uiState.dataMode,
                syncStatus = SyncStatus.Failed,
                pendingOperationCount = pendingOperationCount,
                failureMessage = error.toErrorCause(),
            ),
        )
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.AuthenticationFailed,
    is DomainError.ServiceUnavailable,
    is DomainError.ConfigurationError,
    is DomainError.Unknown -> message
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Sync data not found"
}
