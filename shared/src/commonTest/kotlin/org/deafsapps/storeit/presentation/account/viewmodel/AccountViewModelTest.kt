package org.deafsapps.storeit.presentation.account.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.deafsapps.storeit.domain.usecase.SignOutAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SignUpAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageAction
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.collectUiState

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountViewModelTest {
    private lateinit var sut: AccountViewModel
    private lateinit var fakeSignUpAccountUseCase: FakeSignUpAccountUseCase
    private lateinit var fakeSignInAccountUseCase: FakeSignInAccountUseCase
    private lateinit var fakeSignOutAccountUseCase: FakeSignOutAccountUseCase
    private lateinit var fakeRestoreAccountSessionUseCase: FakeRestoreAccountSessionUseCase
    private lateinit var fakeRestoreAccountDataUseCase: FakeRestoreAccountDataUseCase
    private lateinit var fakeGetSyncStageUseCase: FakeGetSyncStageUseCase

    @BeforeTest
    fun setUp() {
        fakeSignUpAccountUseCase = FakeSignUpAccountUseCase()
        fakeSignInAccountUseCase = FakeSignInAccountUseCase()
        fakeSignOutAccountUseCase = FakeSignOutAccountUseCase()
        fakeRestoreAccountSessionUseCase = FakeRestoreAccountSessionUseCase()
        fakeRestoreAccountDataUseCase = FakeRestoreAccountDataUseCase()
        fakeGetSyncStageUseCase = FakeGetSyncStageUseCase()
    }

    @Test
    fun `GIVEN restored active session with synchronized state WHEN AccountViewModel initializes THEN ui state shows authenticated synchronized account`() =
        runTest {
            val restoredSession = getAccountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.None,
                dataMode = DataMode.AccountBackedSynchronized,
                syncState = SyncState(
                    status = SyncStatus.Synchronized,
                    pendingOperationCount = 0,
                ),
            ).ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            advanceUntilIdle()

            val state = states.lastOrNull()
            assertTrue(actual = fakeRestoreAccountSessionUseCase.wasInvoked)
            assertEquals(expected = restoredSession, actual = fakeGetSyncStageUseCase.lastInput?.session)
            assertEquals(expected = false, actual = state?.isLoading)
            assertEquals(expected = true, actual = state?.isAuthenticated)
            assertEquals(expected = restoredSession.email, actual = state?.accountEmail)
            assertEquals(expected = DataMode.AccountBackedSynchronized, actual = state?.dataMode)
            assertEquals(expected = SyncStatus.Synchronized, actual = state?.syncStatus)
        }

    @Test
    fun `GIVEN restored active session with restore pending state WHEN AccountViewModel initializes THEN ui state shows authenticated restore pending account`() =
        runTest {
            val restoredSession = getAccountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.RestoreRemoteDataset,
                dataMode = DataMode.AccountBackedPendingSync,
                syncState = SyncState(
                    status = SyncStatus.RestorePending,
                    pendingOperationCount = 1,
                ),
            ).ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = restoredSession, actual = fakeGetSyncStageUseCase.lastInput?.session)
            assertEquals(expected = true, actual = state?.isAuthenticated)
            assertEquals(expected = DataMode.AccountBackedPendingSync, actual = state?.dataMode)
            assertEquals(expected = SyncStatus.RestorePending, actual = state?.syncStatus)
            assertEquals(expected = true, actual = state?.canRetryRestore)
        }

    @Test
    fun `GIVEN restored active session with pending upload sync state WHEN AccountViewModel initializes THEN ui state surfaces pending work`() =
        runTest {
            val restoredSession = getAccountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.UploadPendingChanges,
                dataMode = DataMode.AccountBackedPendingSync,
                syncState = SyncState(
                    status = SyncStatus.PendingUpload,
                    pendingOperationCount = 3,
                ),
            ).ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = true, actual = state?.isAuthenticated)
            assertEquals(expected = DataMode.AccountBackedPendingSync, actual = state?.dataMode)
            assertEquals(expected = SyncStatus.PendingUpload, actual = state?.syncStatus)
            assertEquals(expected = 3, actual = state?.pendingOperationCount)
            assertEquals(expected = null, actual = state?.failureMessage)
        }

    @Test
    fun `GIVEN restored active session with failed sync state WHEN AccountViewModel initializes THEN ui state surfaces recoverable failure message`() =
        runTest {
            val restoredSession = getAccountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.RetryFailedSync,
                dataMode = DataMode.AccountBackedPendingSync,
                syncState = SyncState(
                    status = SyncStatus.Failed,
                    failureReason = "Upload failed due to timeout.",
                    pendingOperationCount = 2,
                ),
            ).ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = true, actual = state?.isAuthenticated)
            assertEquals(expected = DataMode.AccountBackedPendingSync, actual = state?.dataMode)
            assertEquals(expected = SyncStatus.Failed, actual = state?.syncStatus)
            assertEquals(expected = 2, actual = state?.pendingOperationCount)
            assertEquals(expected = "Upload failed due to timeout.", actual = state?.failureMessage)
            assertEquals(expected = false, actual = state?.canRetryRestore)
        }

    @Test
    fun `GIVEN sign in credentials WHEN submit sign in THEN authenticates restores account data and refreshes sync state`() =
        runTest {
            val restoredSession = getAccountSession()
            val signedInSession = getAccountSession(lastAuthenticatedAt = 20L)
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

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            sut.selectSignInMode()
            sut.onEmailInputChanged(email = "user@example.com")
            sut.onPasswordInputChanged(password = "passw0rd")
            sut.submitCredentials()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = "user@example.com", actual = fakeSignInAccountUseCase.lastCredentials?.email)
            assertEquals(expected = "passw0rd", actual = fakeSignInAccountUseCase.lastCredentials?.password)
            assertEquals(expected = signedInSession, actual = fakeRestoreAccountDataUseCase.lastSession)
            assertEquals(expected = signedInSession, actual = fakeGetSyncStageUseCase.lastInput?.session)
            assertEquals(expected = false, actual = state?.isSubmitting)
            assertEquals(expected = true, actual = state?.isAuthenticated)
            assertEquals(expected = SyncStatus.Synchronized, actual = state?.syncStatus)
            assertEquals(expected = null, actual = state?.failureMessage)
        }

    @Test
    fun `GIVEN sign up mode and credentials WHEN submit credentials THEN creates account before restore`() =
        runTest {
            val signedUpSession = getAccountSession(lastAuthenticatedAt = 30L)
            fakeSignUpAccountUseCase.result = signedUpSession.ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            sut.selectSignUpMode()
            sut.onEmailInputChanged(email = "new@example.com")
            sut.onPasswordInputChanged(password = "new-pass")
            sut.submitCredentials()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = "new@example.com", actual = fakeSignUpAccountUseCase.lastCredentials?.email)
            assertEquals(expected = "new-pass", actual = fakeSignUpAccountUseCase.lastCredentials?.password)
            assertEquals(expected = signedUpSession, actual = fakeRestoreAccountDataUseCase.lastSession)
            assertEquals(expected = true, actual = state?.isAuthenticated)
        }

    @Test
    fun `GIVEN authentication failure WHEN submit credentials THEN keeps local state and exposes failure`() =
        runTest {
            fakeSignInAccountUseCase.result = DomainError.AuthenticationFailed(
                message = "The email or password is incorrect.",
            ).err()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            sut.onEmailInputChanged(email = "bad")
            sut.onPasswordInputChanged(password = "passw0rd")
            sut.submitCredentials()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = false, actual = state?.isSubmitting)
            assertEquals(expected = false, actual = state?.isAuthenticated)
            assertEquals(expected = DataMode.LocalOnly, actual = state?.dataMode)
            assertEquals(expected = "The email or password is incorrect.", actual = state?.failureMessage)
            assertEquals(expected = null, actual = fakeRestoreAccountDataUseCase.lastSession)
        }

    @Test
    fun `GIVEN Firebase configuration failure WHEN submit credentials THEN exposes gentle fallback message`() =
        runTest {
            fakeSignInAccountUseCase.result = DomainError.ConfigurationError(
                message = "Authentication is unavailable on this build. Check the Firebase configuration.",
            ).err()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            sut.onEmailInputChanged(email = "user@example.com")
            sut.onPasswordInputChanged(password = "passw0rd")
            sut.submitCredentials()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = false, actual = state?.isSubmitting)
            assertEquals(
                expected = "Authentication is unavailable on this build. Check the Firebase configuration.",
                actual = state?.failureMessage,
            )
        }

    @Test
    fun `GIVEN authenticated account WHEN sign out THEN clears authenticated state`() =
        runTest {
            val restoredSession = getAccountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.None,
                dataMode = DataMode.AccountBackedSynchronized,
                syncState = SyncState(
                    status = SyncStatus.Synchronized,
                    pendingOperationCount = 0,
                ),
            ).ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)
            advanceUntilIdle()

            sut.signOut()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = restoredSession.accountId, actual = fakeSignOutAccountUseCase.lastAccountId)
            assertEquals(expected = false, actual = state?.isAuthenticated)
            assertEquals(expected = DataMode.LocalOnly, actual = state?.dataMode)
            assertEquals(expected = null, actual = state?.accountEmail)
        }

    @Test
    fun `GIVEN authenticated account WHEN sign out fails THEN keeps session and exposes failure`() =
        runTest {
            val restoredSession = getAccountSession()
            fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
            fakeGetSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.None,
                dataMode = DataMode.AccountBackedSynchronized,
                syncState = SyncState(
                    status = SyncStatus.Synchronized,
                    pendingOperationCount = 0,
                ),
            ).ok()
            fakeSignOutAccountUseCase.result = DomainError.ServiceUnavailable(
                message = "Sign out is temporarily unavailable.",
            ).err()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)
            advanceUntilIdle()

            sut.signOut()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = true, actual = state?.isAuthenticated)
            assertEquals(expected = restoredSession.email, actual = state?.accountEmail)
            assertEquals(expected = "Sign out is temporarily unavailable.", actual = state?.failureMessage)
        }

    private fun createSut(testScope: TestScope): AccountViewModel = AccountViewModel(
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher(testScope.testScheduler)),
        signUpAccountUseCase = fakeSignUpAccountUseCase,
        signInAccountUseCase = fakeSignInAccountUseCase,
        signOutAccountUseCase = fakeSignOutAccountUseCase,
        restoreAccountSessionUseCase = fakeRestoreAccountSessionUseCase,
        restoreAccountDataUseCase = fakeRestoreAccountDataUseCase,
        getSyncStageUseCase = fakeGetSyncStageUseCase,
    )
}

private fun getAccountSession(lastAuthenticatedAt: Long? = 10L): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = lastAuthenticatedAt,
)

private class FakeSignUpAccountUseCase : SignUpAccountUseCaseType {
    var result: Result<DomainError, AccountSession> = getAccountSession().ok()
    var lastCredentials: EmailPasswordCredentials? = null

    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        lastCredentials = input
        return result
    }
}

private class FakeSignInAccountUseCase : SignInAccountUseCaseType {
    var result: Result<DomainError, AccountSession> = getAccountSession().ok()
    var lastCredentials: EmailPasswordCredentials? = null

    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        lastCredentials = input
        return result
    }
}

private class FakeSignOutAccountUseCase : SignOutAccountUseCaseType {
    var result: Result<DomainError, Unit> = Unit.ok()
    var lastAccountId: String? = null

    override suspend fun invoke(input: String): Result<DomainError, Unit> {
        lastAccountId = input
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
