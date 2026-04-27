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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseInput
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountDataUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.SignInAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SignUpAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.account.model.AccountAuthMode
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.account.model.AccountUiState
import org.koin.core.annotation.Factory

private const val STOP_SHARE_TIMEOUT_MILLIS = 5_000L

@Factory
class AccountViewModel internal constructor(
    coroutineScope: CoroutineScope?,
    private val signUpAccountUseCase: SignUpAccountUseCaseType,
    private val signInAccountUseCase: SignInAccountUseCaseType,
    private val restoreAccountSessionUseCase: RestoreAccountSessionUseCaseType,
    private val restoreAccountDataUseCase: RestoreAccountDataUseCaseType,
    private val getSyncStageUseCase: GetSyncStageUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val loadRequests = MutableSharedFlow<AccountLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateChanges = MutableSharedFlow<AccountStateChange>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val state: StateFlow<AccountViewModelState> = merge(
        loadRequests
            .onStart { emit(value = AccountLoadRequest.RefreshSession) }
            .flatMapLatest { request -> request.toStateChanges() },
        stateChanges,
    )
        .runningFold(
            initial = AccountViewModelState.initial(),
            operation = { state, change -> change.reduce(state = state) },
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_TIMEOUT_MILLIS),
            initialValue = AccountViewModelState.initial(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AccountUiState> = state
        .map { viewModelState -> viewModelState.uiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_TIMEOUT_MILLIS),
            initialValue = state.value.uiState,
        )

    fun retryRestore() {
        val session = state.value.restoredSession
        if (session == null) {
            stateChanges.tryEmit(value = AccountStateChange.SignedOut)
            return
        }

        viewModelScope.launch {
            stateChanges.emit(value = AccountStateChange.Loading)
            restoreAccountDataUseCase(input = session).fold(
                ifErr = { error ->
                    stateChanges.emit(
                        value = AccountStateChange.SyncStageFailed(
                            session = session,
                            syncState = mapFailure(error = error),
                            failureMessage = error.toErrorCause(),
                        ),
                    )
                },
                ifOk = {
                    emitSyncStageChangesToState(session = session)
                },
            )
        }
    }

    fun refreshSession() {
        loadRequests.tryEmit(value = AccountLoadRequest.RefreshSession)
    }

    fun selectSignInMode() {
        selectAuthMode(authMode = AccountAuthMode.SignIn)
    }

    fun selectSignUpMode() {
        selectAuthMode(authMode = AccountAuthMode.SignUp)
    }

    fun onEmailInputChanged(email: String) {
        stateChanges.tryEmit(value = AccountStateChange.EmailInputChanged(email = email))
    }

    fun onPasswordInputChanged(password: String) {
        stateChanges.tryEmit(value = AccountStateChange.PasswordInputChanged(password = password))
    }

    fun submitCredentials() {
        val currentUiState = state.value.uiState
        if (currentUiState.emailInput.isBlank() || currentUiState.passwordInput.isBlank()) {
            stateChanges.tryEmit(value = AccountStateChange.CredentialsRejected)
            return
        }

        viewModelScope.launch {
            stateChanges.emit(value = AccountStateChange.SubmittingCredentials)
            val credentials = EmailPasswordCredentials(
                email = currentUiState.emailInput.trim(),
                password = currentUiState.passwordInput,
            )
            when (currentUiState.authMode) {
                AccountAuthMode.SignIn -> signInAccountUseCase(input = credentials)
                AccountAuthMode.SignUp -> signUpAccountUseCase(input = credentials)
            }.fold(
                ifErr = { error ->
                    stateChanges.emit(value = AccountStateChange.AuthenticationFailed(error = error))
                },
                ifOk = { session ->
                    restoreAuthenticatedAccountData(session = session)
                },
            )
        }
    }

    private fun selectAuthMode(authMode: AccountAuthMode) {
        stateChanges.tryEmit(value = AccountStateChange.AuthModeSelected(authMode = authMode))
    }

    private suspend fun restoreAuthenticatedAccountData(session: AccountSession) {
        restoreAccountDataUseCase(input = session).fold(
            ifErr = { error ->
                stateChanges.emit(
                    value = AccountStateChange.SyncStageFailed(
                        session = session,
                        syncState = mapFailure(error = error),
                        failureMessage = error.toErrorCause(),
                    ),
                )
            },
            ifOk = {
                emitSyncStageChangesToState(session = session)
            },
        )
    }

    private suspend fun emitSyncStageChangesToState(
        session: AccountSession,
    ) {
        getSyncStageUseCase(input = GetSyncStageUseCaseInput(session = session)).fold(
            ifErr = { error ->
                stateChanges.emit(
                    value = AccountStateChange.SyncStageFailed(
                        session = session,
                        syncState = mapFailure(error = error),
                        failureMessage = error.toErrorCause(),
                    ),
                )
            },
            ifOk = { result ->
                stateChanges.emit(
                    value = AccountStateChange.SyncStageResolved(
                        session = session,
                        result = result,
                    ),
                )
            },
        )
    }

    private fun mapFailure(error: DomainError): SyncState = getSyncStageUseCase.mapFailure(
        error = error,
        pendingOperationCount = state.value.uiState.pendingOperationCount,
    )

    private fun AccountLoadRequest.toStateChanges(): Flow<AccountStateChange> = flow {
        emit(value = AccountStateChange.Loading)
        when (this@toStateChanges) {
            AccountLoadRequest.RefreshSession ->
                restoreAccountSessionUseCase(input = Unit).fold(
                    ifErr = { error ->
                        emit(value = AccountStateChange.SessionRestoreFailed(error = error))
                    },
                    ifOk = { session ->
                        if (session == null) {
                            emit(value = AccountStateChange.SignedOut)
                        } else {
                            emitSyncStageChanges(session = session)
                        }
                    },
                )
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
                            pendingOperationCount = state.value.uiState.pendingOperationCount,
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

    private enum class AccountLoadRequest {
        RefreshSession,
    }

    private sealed interface AccountStateChange {
        fun reduce(state: AccountViewModelState): AccountViewModelState

        data object Loading : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    uiState = state.uiState.copy(
                        isLoading = true,
                        isSubmitting = false,
                        failureMessage = null,
                    ),
                )
        }

        data class AuthModeSelected(
            private val authMode: AccountAuthMode,
        ) : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    uiState = state.uiState.copy(authMode = authMode, failureMessage = null),
                )
        }

        data class EmailInputChanged(
            private val email: String,
        ) : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    uiState = state.uiState.copy(emailInput = email, failureMessage = null),
                )
        }

        data class PasswordInputChanged(
            private val password: String,
        ) : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    uiState = state.uiState.copy(passwordInput = password, failureMessage = null),
                )
        }

        data object CredentialsRejected : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    uiState = state.uiState.copy(
                        isSubmitting = false,
                        failureMessage = "Email and password are required",
                    ),
                )
        }

        data object SubmittingCredentials : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    uiState = state.uiState.copy(isSubmitting = true, failureMessage = null),
                )
        }

        data class AuthenticationFailed(
            private val error: DomainError,
        ) : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    restoredSession = null,
                    uiState = state.uiState.copy(
                        isSubmitting = false,
                        isAuthenticated = false,
                        accountEmail = null,
                        dataMode = DataMode.LocalOnly,
                        syncStatus = SyncStatus.Idle,
                        pendingOperationCount = 0,
                        failureMessage = error.toErrorCause(),
                    ),
                )
        }

        data object SignedOut : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    restoredSession = null,
                    uiState = state.uiState.copy(
                        isLoading = false,
                        isSubmitting = false,
                        isAuthenticated = false,
                        accountEmail = null,
                        dataMode = DataMode.LocalOnly,
                        syncStatus = SyncStatus.Idle,
                        pendingOperationCount = 0,
                        failureMessage = null,
                    ),
                )
        }

        data class SessionRestoreFailed(
            private val error: DomainError,
        ) : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    restoredSession = null,
                    uiState = state.uiState.copy(
                        isLoading = false,
                        isSubmitting = false,
                        isAuthenticated = false,
                        accountEmail = null,
                        dataMode = DataMode.LocalOnly,
                        syncStatus = SyncStatus.Idle,
                        pendingOperationCount = 0,
                        failureMessage = error.toErrorCause(),
                    ),
                )
        }

        data class SyncStageResolved(
            private val session: AccountSession,
            private val result: SyncStageResult,
        ) : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    restoredSession = session,
                    uiState = state.uiState.copy(
                        isLoading = false,
                        isSubmitting = false,
                        isAuthenticated = true,
                        accountEmail = session.email,
                        dataMode = result.dataMode,
                        syncStatus = result.syncState.status,
                        pendingOperationCount = result.syncState.pendingOperationCount,
                        failureMessage = result.syncState.failureReason,
                    ),
                )
        }

        data class SyncStageFailed(
            private val session: AccountSession,
            private val syncState: SyncState,
            private val failureMessage: String,
        ) : AccountStateChange {
            override fun reduce(state: AccountViewModelState): AccountViewModelState =
                state.copy(
                    restoredSession = session,
                    uiState = state.uiState.copy(
                        isLoading = false,
                        isSubmitting = false,
                        isAuthenticated = true,
                        accountEmail = session.email,
                        dataMode = DataMode.AccountBackedPendingSync,
                        syncStatus = syncState.status,
                        pendingOperationCount = syncState.pendingOperationCount,
                        failureMessage = syncState.failureReason ?: failureMessage,
                    ),
                )
        }
    }
}

private data class AccountViewModelState(
    val restoredSession: AccountSession?,
    val uiState: AccountUiState,
) {
    companion object {
        fun initial(): AccountViewModelState = AccountViewModelState(
            restoredSession = null,
            uiState = AccountUiState.getDefault().copy(isLoading = true),
        )
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Account data not found"
    is DomainError.Unknown -> message
}
