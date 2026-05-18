package org.deafsapps.storeit.presentation.sync.viewmodel

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
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.CatchUpSignedInSyncUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.RetryPendingSyncUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageAction
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.collectUiState

@OptIn(ExperimentalCoroutinesApi::class)
internal class SyncStatusViewModelTest {
    private lateinit var fakeRestoreAccountSessionUseCase: FakeRestoreAccountSessionUseCase
    private lateinit var fakeCatchUpSignedInSyncUseCase: FakeCatchUpSignedInSyncUseCase
    private lateinit var fakeRetryPendingSyncUseCase: FakeRetryPendingSyncUseCase

    @BeforeTest
    fun setUp() {
        fakeRestoreAccountSessionUseCase = FakeRestoreAccountSessionUseCase()
        fakeCatchUpSignedInSyncUseCase = FakeCatchUpSignedInSyncUseCase()
        fakeRetryPendingSyncUseCase = FakeRetryPendingSyncUseCase()
    }

    @Test
    fun `GIVEN no restored session WHEN view-model loads THEN emits local-only state`() = runTest {
        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = false, actual = state?.isAuthenticated)
        assertEquals(expected = DataMode.LocalOnly, actual = state?.dataMode)
        assertEquals(expected = SyncStatus.Idle, actual = state?.syncStatus)
    }

    @Test
    fun `GIVEN pending upload stage WHEN view-model loads THEN emits pending upload status`() = runTest {
        fakeRestoreAccountSessionUseCase.result = accountSession().ok()
        fakeCatchUpSignedInSyncUseCase.result = SyncStageResult(
            nextAction = SyncStageAction.UploadPendingChanges,
            dataMode = DataMode.AccountBackedPendingSync,
            syncState = SyncState(
                status = SyncStatus.PendingUpload,
                pendingOperationCount = 3,
            ),
        ).ok()

        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = true, actual = state?.isAuthenticated)
        assertEquals(expected = DataMode.AccountBackedPendingSync, actual = state?.dataMode)
        assertEquals(expected = SyncStatus.PendingUpload, actual = state?.syncStatus)
        assertEquals(expected = 3, actual = state?.pendingOperationCount)
    }

    @Test
    fun `GIVEN restore pending stage WHEN view-model loads THEN emits restore pending status`() = runTest {
        fakeRestoreAccountSessionUseCase.result = accountSession().ok()
        fakeCatchUpSignedInSyncUseCase.result = SyncStageResult(
            nextAction = SyncStageAction.RestoreRemoteDataset,
            dataMode = DataMode.AccountBackedPendingSync,
            syncState = SyncState(
                status = SyncStatus.RestorePending,
                pendingOperationCount = 1,
            ),
        ).ok()

        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = SyncStatus.RestorePending, actual = state?.syncStatus)
        assertEquals(expected = true, actual = state?.canRetry)
    }

    @Test
    fun `GIVEN synchronized stage WHEN view-model loads THEN emits synchronized status`() = runTest {
        fakeRestoreAccountSessionUseCase.result = accountSession().ok()
        fakeCatchUpSignedInSyncUseCase.result = SyncStageResult(
            nextAction = SyncStageAction.None,
            dataMode = DataMode.AccountBackedSynchronized,
            syncState = SyncState(
                status = SyncStatus.Synchronized,
                pendingOperationCount = 0,
            ),
        ).ok()

        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = SyncStatus.Synchronized, actual = state?.syncStatus)
        assertEquals(expected = true, actual = state?.isDataBackedUp)
    }

    @Test
    fun `GIVEN failed stage WHEN retry is called THEN emits failed status with recoverable message`() = runTest {
        fakeRestoreAccountSessionUseCase.result = accountSession().ok()
        fakeCatchUpSignedInSyncUseCase.result = SyncStageResult(
            nextAction = SyncStageAction.RetryFailedSync,
            dataMode = DataMode.AccountBackedPendingSync,
            syncState = SyncState(
                status = SyncStatus.Failed,
                failureReason = "Upload failed due to timeout.",
                pendingOperationCount = 2,
            ),
        ).ok()
        fakeRetryPendingSyncUseCase.result = DomainError.ServiceUnavailable(
            message = "Retry failed due to network.",
        ).err()

        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)
        advanceUntilIdle()

        sut.retry()
        advanceUntilIdle()

        val state = states.lastOrNull()
        assertTrue(actual = fakeRetryPendingSyncUseCase.wasInvoked)
        assertEquals(expected = SyncStatus.Failed, actual = state?.syncStatus)
        assertEquals(expected = "Retry failed due to network.", actual = state?.failureMessage)
    }

    private fun createSut(testScope: TestScope): SyncStatusViewModel = SyncStatusViewModel(
        coroutineScope = CoroutineScope(context = UnconfinedTestDispatcher(testScope.testScheduler)),
        restoreAccountSessionUseCase = fakeRestoreAccountSessionUseCase,
        catchUpSignedInSyncUseCase = fakeCatchUpSignedInSyncUseCase,
        retryPendingSyncUseCase = fakeRetryPendingSyncUseCase,
    )
}

private class FakeRestoreAccountSessionUseCase : RestoreAccountSessionUseCaseType {
    var result: Result<DomainError, AccountSession?> = null.ok()

    override suspend fun invoke(input: Unit): Result<DomainError, AccountSession?> = result
}

private class FakeCatchUpSignedInSyncUseCase : CatchUpSignedInSyncUseCaseType {
    var result: Result<DomainError, SyncStageResult> = DomainError.Unknown().err()

    override suspend fun invoke(input: AccountSession): Result<DomainError, SyncStageResult> = result
}

private class FakeRetryPendingSyncUseCase : RetryPendingSyncUseCaseType {
    var result: Result<DomainError, SyncStageResult> = DomainError.Unknown().err()
    var wasInvoked: Boolean = false

    override suspend fun invoke(input: AccountSession): Result<DomainError, SyncStageResult> {
        wasInvoked = true
        return result
    }
}

private fun accountSession(): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = 10L,
)
