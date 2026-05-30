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
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.usecase.KeepLocalReconciliationUseCaseType
import org.deafsapps.storeit.domain.usecase.KeepRemoteReconciliationUseCaseType
import org.deafsapps.storeit.domain.usecase.ResolveAccountSyncStageUseCaseType
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.SyncStageAction
import org.deafsapps.storeit.domain.usecase.SyncStageResult
import org.deafsapps.storeit.presentation.collectUiState

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalModeViewModelTest {
    private lateinit var fakeRestoreAccountSessionUseCase: FakeLocalModeRestoreAccountSessionUseCase
    private lateinit var fakeResolveAccountSyncStageUseCase: FakeLocalModeResolveAccountSyncStageUseCase
    private lateinit var fakeKeepLocalReconciliationUseCase: FakeLocalModeKeepLocalReconciliationUseCase
    private lateinit var fakeKeepRemoteReconciliationUseCase: FakeLocalModeKeepRemoteReconciliationUseCase

    @BeforeTest
    fun setUp() {
        fakeRestoreAccountSessionUseCase = FakeLocalModeRestoreAccountSessionUseCase()
        fakeResolveAccountSyncStageUseCase = FakeLocalModeResolveAccountSyncStageUseCase()
        fakeKeepLocalReconciliationUseCase = FakeLocalModeKeepLocalReconciliationUseCase()
        fakeKeepRemoteReconciliationUseCase = FakeLocalModeKeepRemoteReconciliationUseCase()
    }

    @Test
    fun `GIVEN no restored session WHEN view-model loads THEN emits signed-out-with-local-copy state`() = runTest {
        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = DataMode.SignedOutWithLocalCopy, actual = state?.dataMode)
        assertEquals(expected = false, actual = state?.isAuthenticated)
    }

    @Test
    fun `GIVEN reconciliation required stage WHEN view-model loads THEN enables keep-local and keep-remote`() =
        runTest {
            fakeRestoreAccountSessionUseCase.result = accountSession().ok()
            fakeResolveAccountSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.AwaitReconciliation,
                dataMode = DataMode.ReconciliationRequired,
                syncState = SyncState(
                    status = SyncStatus.BlockedByReconciliation,
                    pendingOperationCount = 2,
                ),
            ).ok()

            val sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = true, actual = state?.requiresReconciliation)
            assertEquals(expected = true, actual = state?.canChooseKeepLocal)
            assertEquals(expected = true, actual = state?.canChooseKeepRemote)
        }

    @Test
    fun `GIVEN reconciliation-required state WHEN keepLocal succeeds THEN emits pending-upload account-backed mode`() =
        runTest {
            fakeRestoreAccountSessionUseCase.result = accountSession().ok()
            fakeResolveAccountSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.AwaitReconciliation,
                dataMode = DataMode.ReconciliationRequired,
                syncState = SyncState(status = SyncStatus.BlockedByReconciliation),
            ).ok()
            fakeKeepLocalReconciliationUseCase.result = DataMode.AccountBackedPendingSync.ok()

            val sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)
            advanceUntilIdle()

            sut.onKeepLocalSelected()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertTrue(actual = fakeKeepLocalReconciliationUseCase.wasInvoked)
            assertEquals(expected = DataMode.AccountBackedPendingSync, actual = state?.dataMode)
            assertEquals(expected = SyncStatus.PendingUpload, actual = state?.syncStatus)
        }

    private fun createSut(testScope: TestScope): LocalModeViewModel = LocalModeViewModel(
        coroutineScope = CoroutineScope(context = UnconfinedTestDispatcher(testScope.testScheduler)),
        restoreAccountSessionUseCase = fakeRestoreAccountSessionUseCase,
        resolveAccountSyncStageUseCase = fakeResolveAccountSyncStageUseCase,
        keepLocalReconciliationUseCase = fakeKeepLocalReconciliationUseCase,
        keepRemoteReconciliationUseCase = fakeKeepRemoteReconciliationUseCase,
    )
}

private class FakeLocalModeRestoreAccountSessionUseCase : RestoreAccountSessionUseCaseType {
    var result: Result<DomainError, AccountSession?> = null.ok()

    override suspend fun invoke(input: Unit): Result<DomainError, AccountSession?> = result
}

private class FakeLocalModeResolveAccountSyncStageUseCase : ResolveAccountSyncStageUseCaseType {
    var result: Result<DomainError, SyncStageResult> = DomainError.Unknown().err()

    override suspend fun invoke(input: AccountSession): Result<DomainError, SyncStageResult> = result
}

private class FakeLocalModeKeepLocalReconciliationUseCase : KeepLocalReconciliationUseCaseType {
    var result: Result<DomainError, DataMode> = DataMode.AccountBackedPendingSync.ok()
    var wasInvoked: Boolean = false

    override suspend fun invoke(input: String): Result<DomainError, DataMode> {
        wasInvoked = true
        return result
    }
}

private class FakeLocalModeKeepRemoteReconciliationUseCase : KeepRemoteReconciliationUseCaseType {
    var result: Result<DomainError, DataMode> = DataMode.AccountBackedPendingSync.ok()

    override suspend fun invoke(input: String): Result<DomainError, DataMode> = result
}

private fun accountSession(): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = 10L,
)
