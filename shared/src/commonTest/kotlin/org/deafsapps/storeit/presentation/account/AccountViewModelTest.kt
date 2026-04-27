package org.deafsapps.storeit.presentation.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseInput
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountDataUseCaseType
import org.deafsapps.storeit.domain.usecase.SignInAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SignUpAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageAction
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.account.viewmodel.AccountViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private lateinit var fakeSignUpAccountUseCase: FakeSignUpAccountUseCase
    private lateinit var fakeSignInAccountUseCase: FakeSignInAccountUseCase
    private lateinit var fakeRestoreAccountSessionUseCase: FakeRestoreAccountSessionUseCase
    private lateinit var fakeRestoreAccountDataUseCase: FakeRestoreAccountDataUseCase
    private lateinit var fakeGetSyncStageUseCase: FakeGetSyncStageUseCase

    @BeforeTest
    fun setUp() {
        fakeSignUpAccountUseCase = FakeSignUpAccountUseCase()
        fakeSignInAccountUseCase = FakeSignInAccountUseCase()
        fakeRestoreAccountSessionUseCase = FakeRestoreAccountSessionUseCase()
        fakeRestoreAccountDataUseCase = FakeRestoreAccountDataUseCase()
        fakeGetSyncStageUseCase = FakeGetSyncStageUseCase()
    }

    @Test
    fun `GIVEN restored active session with synchronized state WHEN AccountViewModel initializes THEN ui state shows authenticated synchronized account`() =
        runTest {
            val restoredSession = accountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.None,
                dataMode = DataMode.AccountBackedSynchronized,
                syncState = SyncState(
                    status = SyncStatus.Synchronized,
                    pendingOperationCount = 0,
                ),
            ).ok()

            val sut = createSut(testScope = this)
            collectUiState(sut = sut)

            advanceUntilIdle()

            assertTrue(actual = fakeRestoreAccountSessionUseCase.wasInvoked)
            assertEquals(expected = restoredSession, actual = fakeGetSyncStageUseCase.lastInput?.session)
            assertEquals(expected = false, actual = sut.uiState.value.isLoading)
            assertEquals(expected = true, actual = sut.uiState.value.isAuthenticated)
            assertEquals(expected = restoredSession.email, actual = sut.uiState.value.accountEmail)
            assertEquals(expected = DataMode.AccountBackedSynchronized, actual = sut.uiState.value.dataMode)
            assertEquals(expected = SyncStatus.Synchronized, actual = sut.uiState.value.syncStatus)
        }

    @Test
    fun `GIVEN restored active session with restore pending state WHEN AccountViewModel initializes THEN ui state shows authenticated restore pending account`() =
        runTest {
            val restoredSession = accountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.RestoreRemoteDataset,
                dataMode = DataMode.AccountBackedPendingSync,
                syncState = SyncState(
                    status = SyncStatus.RestorePending,
                    pendingOperationCount = 1,
                ),
            ).ok()

            val sut = createSut(testScope = this)
            collectUiState(sut = sut)

            advanceUntilIdle()

            assertEquals(expected = restoredSession, actual = fakeGetSyncStageUseCase.lastInput?.session)
            assertEquals(expected = true, actual = sut.uiState.value.isAuthenticated)
            assertEquals(expected = DataMode.AccountBackedPendingSync, actual = sut.uiState.value.dataMode)
            assertEquals(expected = SyncStatus.RestorePending, actual = sut.uiState.value.syncStatus)
            assertEquals(expected = true, actual = sut.uiState.value.canRetryRestore)
        }

    @Test
    fun `GIVEN sign in credentials WHEN submit sign in THEN authenticates restores account data and refreshes sync state`() =
        runTest {
            val restoredSession = accountSession()
            val signedInSession = accountSession(lastAuthenticatedAt = 20L)
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeSignInAccountUseCase.result = signedInSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.None,
                dataMode = DataMode.AccountBackedSynchronized,
                syncState = SyncState(
                    status = SyncStatus.Synchronized,
                    pendingOperationCount = 0,
                ),
            ).ok()

            val sut = createSut(testScope = this)
            collectUiState(sut = sut)

            sut.selectSignInMode()
            sut.onEmailInputChanged(email = "user@example.com")
            sut.onPasswordInputChanged(password = "passw0rd")
            sut.submitCredentials()
            advanceUntilIdle()

            assertEquals(expected = "user@example.com", actual = fakeSignInAccountUseCase.lastCredentials?.email)
            assertEquals(expected = "passw0rd", actual = fakeSignInAccountUseCase.lastCredentials?.password)
            assertEquals(expected = signedInSession, actual = fakeRestoreAccountDataUseCase.lastSession)
            assertEquals(expected = signedInSession, actual = fakeGetSyncStageUseCase.lastInput?.session)
            assertEquals(expected = false, actual = sut.uiState.value.isSubmitting)
            assertEquals(expected = true, actual = sut.uiState.value.isAuthenticated)
            assertEquals(expected = SyncStatus.Synchronized, actual = sut.uiState.value.syncStatus)
            assertEquals(expected = null, actual = sut.uiState.value.failureMessage)
        }

    @Test
    fun `GIVEN sign up mode and credentials WHEN submit credentials THEN creates account before restore`() =
        runTest {
            val signedUpSession = accountSession(lastAuthenticatedAt = 30L)
            fakeSignUpAccountUseCase.result = signedUpSession.ok()

            val sut = createSut(testScope = this)
            collectUiState(sut = sut)

            sut.selectSignUpMode()
            sut.onEmailInputChanged(email = "new@example.com")
            sut.onPasswordInputChanged(password = "new-pass")
            sut.submitCredentials()
            advanceUntilIdle()

            assertEquals(expected = "new@example.com", actual = fakeSignUpAccountUseCase.lastCredentials?.email)
            assertEquals(expected = "new-pass", actual = fakeSignUpAccountUseCase.lastCredentials?.password)
            assertEquals(expected = signedUpSession, actual = fakeRestoreAccountDataUseCase.lastSession)
            assertEquals(expected = true, actual = sut.uiState.value.isAuthenticated)
        }

    @Test
    fun `GIVEN authentication failure WHEN submit credentials THEN keeps local state and exposes failure`() =
        runTest {
            fakeSignInAccountUseCase.result = DomainError.ValidationError(
                field = "email",
                reason = "Invalid email",
            ).err()

            val sut = createSut(testScope = this)
            collectUiState(sut = sut)

            sut.onEmailInputChanged(email = "bad")
            sut.onPasswordInputChanged(password = "passw0rd")
            sut.submitCredentials()
            advanceUntilIdle()

            assertEquals(expected = false, actual = sut.uiState.value.isSubmitting)
            assertEquals(expected = false, actual = sut.uiState.value.isAuthenticated)
            assertEquals(expected = DataMode.LocalOnly, actual = sut.uiState.value.dataMode)
            assertEquals(expected = "Invalid email", actual = sut.uiState.value.failureMessage)
            assertEquals(expected = null, actual = fakeRestoreAccountDataUseCase.lastSession)
        }

    private fun createSut(testScope: TestScope): AccountViewModel = AccountViewModel(
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher(testScope.testScheduler)),
        signUpAccountUseCase = fakeSignUpAccountUseCase,
        signInAccountUseCase = fakeSignInAccountUseCase,
        restoreAccountSessionUseCase = fakeRestoreAccountSessionUseCase,
        restoreAccountDataUseCase = fakeRestoreAccountDataUseCase,
        getSyncStageUseCase = fakeGetSyncStageUseCase,
    )

    private fun TestScope.collectUiState(sut: AccountViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.uiState.collect {}
        }
    }
}

private fun accountSession(lastAuthenticatedAt: Long? = 10L): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = lastAuthenticatedAt,
)

private class FakeSignUpAccountUseCase : SignUpAccountUseCaseType {
    var result: Result<DomainError, AccountSession> = accountSession().ok()
    var lastCredentials: EmailPasswordCredentials? = null

    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        lastCredentials = input
        return result
    }
}

private class FakeSignInAccountUseCase : SignInAccountUseCaseType {
    var result: Result<DomainError, AccountSession> = accountSession().ok()
    var lastCredentials: EmailPasswordCredentials? = null

    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        lastCredentials = input
        return result
    }
}

private class FakeRestoreAccountSessionUseCase : RestoreAccountSessionUseCaseType {
    var result: Result<DomainError, AccountSession?> = null.ok()
    var wasInvoked: Boolean = false

    override suspend fun invoke(input: Unit): Result<DomainError, AccountSession?> {
        wasInvoked = true
        return result
    }
}

private class FakeRestoreAccountDataUseCase : RestoreAccountDataUseCaseType {
    var result: Result<DomainError, Unit> = Unit.ok()
    var lastSession: AccountSession? = null

    override suspend fun invoke(input: AccountSession): Result<DomainError, Unit> {
        lastSession = input
        return result
    }
}

private class FakeGetSyncStageUseCase : GetSyncStageUseCaseType {
    var result: Result<DomainError, SyncStageResult> = SyncStageResult(
        nextAction = SyncStageAction.None,
        dataMode = DataMode.LocalOnly,
        syncState = SyncState(
            status = SyncStatus.Idle,
            pendingOperationCount = 0,
        ),
    ).ok()

    var lastInput: GetSyncStageUseCaseInput? = null

    override suspend fun invoke(input: GetSyncStageUseCaseInput): Result<DomainError, SyncStageResult> {
        lastInput = input
        return result
    }

    override fun mapFailure(
        error: DomainError,
        pendingOperationCount: Int,
        attemptedAt: Long,
    ): SyncState = SyncState(
        status = SyncStatus.Failed,
        failureReason = (error as? DomainError.Unknown)?.message ?: "failed",
        lastAttemptAt = attemptedAt,
        pendingOperationCount = pendingOperationCount,
    )
}
