package org.deafsapps.storeit.presentation.account

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
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseInput
import org.deafsapps.storeit.domain.usecase.GetSyncStageUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageAction
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.account.viewmodel.AccountViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private lateinit var fakeRestoreAccountSessionUseCase: FakeRestoreAccountSessionUseCase
    private lateinit var fakeGetSyncStageUseCase: FakeGetSyncStageUseCase

    @BeforeTest
    fun setUp() {
        fakeRestoreAccountSessionUseCase = FakeRestoreAccountSessionUseCase()
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

    private fun createSut(testScope: TestScope): AccountViewModel = AccountViewModel(
        coroutineScope = testScope.backgroundScope,
        restoreAccountSessionUseCase = fakeRestoreAccountSessionUseCase,
        getSyncStageUseCase = fakeGetSyncStageUseCase,
    )

    private fun TestScope.collectUiState(sut: AccountViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.uiState.collect {}
        }
    }
}

private fun accountSession(): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = 10L,
)

private class FakeRestoreAccountSessionUseCase : RestoreAccountSessionUseCaseType {
    var result: Result<DomainError, AccountSession?> = null.ok()
    var wasInvoked: Boolean = false

    override suspend fun invoke(input: Unit): Result<DomainError, AccountSession?> {
        wasInvoked = true
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
