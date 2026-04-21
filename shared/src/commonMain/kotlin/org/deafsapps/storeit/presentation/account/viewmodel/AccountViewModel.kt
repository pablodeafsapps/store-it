package org.deafsapps.storeit.presentation.account.viewmodel

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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseInput
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.account.model.AccountUiState
import org.koin.core.annotation.Factory

private const val STOP_SHARE_TIMEOUT_MILLIS = 5_000L

@Factory
class AccountViewModel internal constructor(
    coroutineScope: CoroutineScope?,
    private val restoreAccountSessionUseCase: RestoreAccountSessionUseCaseType,
    private val getSyncStageUseCase: GetSyncStageUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val loadRequests = MutableSharedFlow<AccountLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private var restoredSession: AccountSession? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AccountUiState> = loadRequests
        .onStart { emit(value = AccountLoadRequest.RefreshSession) }
        .flatMapLatest { request -> request.toStateChanges() }
        .runningFold(
            initial = AccountUiState.getDefault().copy(isLoading = true),
            operation = { state, change -> change.reduce(state = state) },
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_TIMEOUT_MILLIS),
            initialValue = AccountUiState.getDefault().copy(isLoading = true),
        )

    fun retryRestore() {
        loadRequests.tryEmit(value = AccountLoadRequest.RetryRestore)
    }

    fun refreshSession() {
        loadRequests.tryEmit(value = AccountLoadRequest.RefreshSession)
    }

    private fun AccountLoadRequest.toStateChanges(): Flow<AccountStateChange> = flow {
        emit(value = AccountStateChange.Loading)
        when (this@toStateChanges) {
            AccountLoadRequest.RefreshSession ->
                restoreAccountSessionUseCase(input = Unit).fold(
                    ifErr = { error -> emit(value = AccountStateChange.SessionRestoreFailed(error = error)) },
                    ifOk = { session ->
                        restoredSession = session
                        if (session == null) {
                            emit(value = AccountStateChange.SignedOut)
                        } else {
                            emitSyncStageChanges(session = session)
                        }
                    },
                )

            AccountLoadRequest.RetryRestore -> {
                val session = restoredSession
                if (session == null) {
                    emit(value = AccountStateChange.SignedOut)
                } else {
                    emitSyncStageChanges(session = session)
                }
            }
        }
    }

    private suspend fun FlowCollector<AccountStateChange>.emitSyncStageChanges(
        session: AccountSession,
    ) {
        getSyncStageUseCase(input = GetSyncStageUseCaseInput(session = session)).fold(
            ifErr = { error ->
                emit(
                    value = AccountStateChange.SyncStageFailed(
                        session = session,
                        syncState = getSyncStageUseCase.mapFailure(
                            error = error,
                            pendingOperationCount = uiState.value.pendingOperationCount,
                        ),
                        failureMessage = error.toErrorCause(),
                    ),
                )
            },
            ifOk = { result ->
                emit(
                    value = AccountStateChange.SyncStageResolved(
                        session = session,
                        result = result,
                    ),
                )
            },
        )
    }

    private sealed interface AccountLoadRequest {
        data object RefreshSession : AccountLoadRequest

        data object RetryRestore : AccountLoadRequest
    }

    private sealed interface AccountStateChange {
        fun reduce(state: AccountUiState): AccountUiState

        data object Loading : AccountStateChange {
            override fun reduce(state: AccountUiState): AccountUiState =
                state.copy(isLoading = true, failureMessage = null)
        }

        data object SignedOut : AccountStateChange {
            override fun reduce(state: AccountUiState): AccountUiState =
                state.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    accountEmail = null,
                    dataMode = DataMode.LocalOnly,
                    syncStatus = SyncStatus.Idle,
                    pendingOperationCount = 0,
                    failureMessage = null,
                )
        }

        data class SessionRestoreFailed(
            private val error: DomainError,
        ) : AccountStateChange {
            override fun reduce(state: AccountUiState): AccountUiState =
                state.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    accountEmail = null,
                    dataMode = DataMode.LocalOnly,
                    syncStatus = SyncStatus.Idle,
                    pendingOperationCount = 0,
                    failureMessage = error.toErrorCause(),
                )
        }

        data class SyncStageResolved(
            private val session: AccountSession,
            private val result: SyncStageResult,
        ) : AccountStateChange {
            override fun reduce(state: AccountUiState): AccountUiState =
                state.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    accountEmail = session.email,
                    dataMode = result.dataMode,
                    syncStatus = result.syncState.status,
                    pendingOperationCount = result.syncState.pendingOperationCount,
                    failureMessage = result.syncState.failureReason,
                )
        }

        data class SyncStageFailed(
            private val session: AccountSession,
            private val syncState: SyncState,
            private val failureMessage: String,
        ) : AccountStateChange {
            override fun reduce(state: AccountUiState): AccountUiState =
                state.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    accountEmail = session.email,
                    dataMode = DataMode.AccountBackedPendingSync,
                    syncStatus = syncState.status,
                    pendingOperationCount = syncState.pendingOperationCount,
                    failureMessage = syncState.failureReason ?: failureMessage,
                )
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Account data not found"
    is DomainError.Unknown -> message
}
