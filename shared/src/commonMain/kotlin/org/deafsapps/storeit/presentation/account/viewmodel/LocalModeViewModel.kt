package org.deafsapps.storeit.presentation.account.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
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
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.KeepLocalReconciliationUseCaseType
import org.deafsapps.storeit.domain.usecase.KeepRemoteReconciliationUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.ResolveAccountSyncStageUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.account.model.LocalModeUiState
import org.koin.core.annotation.Factory

private const val STOP_SHARE_TIMEOUT_MILLIS = 5_000L

@Factory
class LocalModeViewModel internal constructor(
    coroutineScope: CoroutineScope?,
    private val restoreAccountSessionUseCase: RestoreAccountSessionUseCaseType,
    private val resolveAccountSyncStageUseCase: ResolveAccountSyncStageUseCaseType,
    private val keepLocalReconciliationUseCase: KeepLocalReconciliationUseCaseType,
    private val keepRemoteReconciliationUseCase: KeepRemoteReconciliationUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {
    private val loadRequests = MutableSharedFlow<LocalModeLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateChanges = MutableSharedFlow<LocalModeStateChange>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val state: StateFlow<LocalModeViewModelState> = merge(
        loadRequests
            .onStart { emit(value = LocalModeLoadRequest.Refresh) }
            .flatMapLatest { request -> request.toStateChanges() },
        stateChanges,
    ).runningFold(
        initial = LocalModeViewModelState.initial(),
        operation = { currentState, change -> change.reduce(state = currentState) },
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_TIMEOUT_MILLIS),
        initialValue = LocalModeViewModelState.initial(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LocalModeUiState> = state
        .map { viewModelState -> viewModelState.uiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_TIMEOUT_MILLIS),
            initialValue = state.value.uiState,
        )

    fun onRefreshRequested() {
        loadRequests.tryEmit(value = LocalModeLoadRequest.Refresh)
    }

    fun onKeepLocalSelected() {
        applyDecision(
            decision = ReconciliationDecisionAction.KeepLocal,
            useCase = { accountId -> keepLocalReconciliationUseCase(input = accountId) },
        )
    }

    fun onKeepRemoteSelected() {
        applyDecision(
            decision = ReconciliationDecisionAction.KeepRemote,
            useCase = { accountId -> keepRemoteReconciliationUseCase(input = accountId) },
        )
    }

    private fun applyDecision(
        decision: ReconciliationDecisionAction,
        useCase: suspend (String) -> Result<DomainError, DataMode>,
    ) {
        val session = state.value.restoredSession
        if (session == null) {
            stateChanges.tryEmit(
                value = LocalModeStateChange.OperationFailed(error = DomainError.AuthenticationFailed())
            )
            return
        }

        viewModelScope.launch {
            stateChanges.emit(value = LocalModeStateChange.Loading)
            useCase(session.accountId).fold(
                ifErr = { error ->
                    stateChanges.emit(value = LocalModeStateChange.OperationFailed(error = error))
                },
                ifOk = { dataMode ->
                    stateChanges.emit(
                        value = LocalModeStateChange.ReconciliationApplied(
                            session = session,
                            dataMode = dataMode,
                            decision = decision,
                        ),
                    )
                },
            )
        }
    }

    private fun LocalModeLoadRequest.toStateChanges() = flow {
        emit(value = LocalModeStateChange.Loading)
        restoreAccountSessionUseCase(input = Unit).fold(
            ifErr = { error -> emit(value = LocalModeStateChange.OperationFailed(error = error)) },
            ifOk = { session ->
                if (session == null) {
                    emit(value = LocalModeStateChange.SignedOut)
                } else {
                    resolveAccountSyncStageUseCase(input = session).fold(
                        ifErr = { error -> emit(value = LocalModeStateChange.OperationFailed(error = error)) },
                        ifOk = { stage ->
                            emit(
                                value = LocalModeStateChange.StageResolved(
                                    session = session,
                                    dataMode = stage.dataMode,
                                    syncStatus = stage.syncState.status,
                                ),
                            )
                        },
                    )
                }
            },
        )
    }
}

private data class LocalModeViewModelState(
    val restoredSession: AccountSession?,
    val uiState: LocalModeUiState,
) {
    companion object {
        fun initial(): LocalModeViewModelState = LocalModeViewModelState(
            restoredSession = null,
            uiState = LocalModeUiState.getDefault(),
        )
    }
}

private enum class LocalModeLoadRequest {
    Refresh,
}

private enum class ReconciliationDecisionAction {
    KeepLocal,
    KeepRemote,
}

private sealed interface LocalModeStateChange {
    fun reduce(state: LocalModeViewModelState): LocalModeViewModelState

    data object Loading : LocalModeStateChange {
        override fun reduce(state: LocalModeViewModelState): LocalModeViewModelState = state.copy(
            uiState = state.uiState.copy(isLoading = true, failureMessage = null),
        )
    }

    data object SignedOut : LocalModeStateChange {
        override fun reduce(state: LocalModeViewModelState): LocalModeViewModelState = state.copy(
            restoredSession = null,
            uiState = LocalModeUiState(
                isLoading = false,
                dataMode = DataMode.SignedOutWithLocalCopy,
                syncStatus = SyncStatus.Idle,
                isAuthenticated = false,
                failureMessage = null,
            ),
        )
    }

    data class StageResolved(
        private val session: AccountSession,
        private val dataMode: DataMode,
        private val syncStatus: SyncStatus,
    ) : LocalModeStateChange {
        override fun reduce(state: LocalModeViewModelState): LocalModeViewModelState = state.copy(
            restoredSession = session,
            uiState = LocalModeUiState(
                isLoading = false,
                dataMode = dataMode,
                syncStatus = syncStatus,
                isAuthenticated = true,
                failureMessage = null,
            ),
        )
    }

    data class ReconciliationApplied(
        private val session: AccountSession,
        private val dataMode: DataMode,
        private val decision: ReconciliationDecisionAction,
    ) : LocalModeStateChange {
        override fun reduce(state: LocalModeViewModelState): LocalModeViewModelState = state.copy(
            restoredSession = session,
            uiState = LocalModeUiState(
                isLoading = false,
                dataMode = dataMode,
                syncStatus = SyncStatus.PendingUpload,
                isAuthenticated = true,
                failureMessage = when (decision) {
                    ReconciliationDecisionAction.KeepLocal -> "Keeping local data. Upload will continue."
                    ReconciliationDecisionAction.KeepRemote -> "Keeping remote data. Sync is resuming."
                },
            ),
        )
    }

    data class OperationFailed(
        private val error: DomainError,
    ) : LocalModeStateChange {
        override fun reduce(state: LocalModeViewModelState): LocalModeViewModelState = state.copy(
            uiState = state.uiState.copy(
                isLoading = false,
                failureMessage = error.toUserMessage(),
            ),
        )
    }
}

private fun DomainError.toUserMessage(): String = when (this) {
    is DomainError.AuthenticationFailed,
    is DomainError.ServiceUnavailable,
    is DomainError.ConfigurationError,
    is DomainError.Unknown -> message
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Required reconciliation data was not found."
}
