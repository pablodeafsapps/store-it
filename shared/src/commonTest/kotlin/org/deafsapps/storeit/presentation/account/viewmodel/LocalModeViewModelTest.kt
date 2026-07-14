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

    private lateinit var sut: LocalModeViewModel
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
        sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = DataMode.SignedOutWithLocalCopy, actual = state?.dataMode)
        assertEquals(expected = false, actual = state?.isAuthenticated)
    }

    @Test
    fun `GIVEN authenticated local-only stage WHEN view-model loads THEN emits local-only authenticated presentation state`() =
        runTest {
            fakeRestoreAccountSessionUseCase.result = accountSession().ok()
            fakeResolveAccountSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.None,
                dataMode = DataMode.LocalOnly,
                syncState = SyncState(status = SyncStatus.Idle),
            ).ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)

            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = DataMode.LocalOnly, actual = state?.dataMode)
            assertEquals(expected = true, actual = state?.isLocalOnly)
            assertEquals(expected = true, actual = state?.isAuthenticated)
            assertEquals(expected = false, actual = state?.requiresReconciliation)
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

            sut = createSut(testScope = this)
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

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)
            advanceUntilIdle()

            sut.onKeepLocalSelected()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertTrue(actual = fakeKeepLocalReconciliationUseCase.wasInvoked)
            assertEquals(expected = DataMode.AccountBackedPendingSync, actual = state?.dataMode)
            assertEquals(expected = SyncStatus.PendingUpload, actual = state?.syncStatus)
        }

    @Test
    fun `GIVEN reconciliation-required state WHEN keepRemote succeeds THEN emits pending-upload account-backed mode with remote message`() =
        runTest {
            fakeRestoreAccountSessionUseCase.result = accountSession().ok()
            fakeResolveAccountSyncStageUseCase.result = SyncStageResult(
                nextAction = SyncStageAction.AwaitReconciliation,
                dataMode = DataMode.ReconciliationRequired,
                syncState = SyncState(status = SyncStatus.BlockedByReconciliation),
            ).ok()
            fakeKeepRemoteReconciliationUseCase.result = DataMode.AccountBackedPendingSync.ok()

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)
            advanceUntilIdle()

            sut.onKeepRemoteSelected()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertTrue(actual = fakeKeepRemoteReconciliationUseCase.wasInvoked)
            assertEquals(expected = DataMode.AccountBackedPendingSync, actual = state?.dataMode)
            assertEquals(expected = SyncStatus.PendingUpload, actual = state?.syncStatus)
            assertEquals(expected = "Keeping remote data. Sync is resuming.", actual = state?.failureMessage)
        }

    @Test
    fun `GIVEN stage resolution fails WHEN view-model loads THEN emits operation failure message`() = runTest {
        fakeRestoreAccountSessionUseCase.result = accountSession().ok()
        fakeResolveAccountSyncStageUseCase.result = DomainError.Unknown(message = "stage failed").err()

        sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = "stage failed", actual = state?.failureMessage)
        assertEquals(expected = false, actual = state?.isLoading)
    }

    @Test
    fun `GIVEN no restored session WHEN keep local is selected THEN emits authentication failure message`() = runTest {
        sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)
        advanceUntilIdle()

        sut.onKeepLocalSelected()
        advanceUntilIdle()

        val state = states.lastOrNull()
        assertEquals(expected = "Authentication failed", actual = state?.failureMessage)
    }

    @Test
    fun `GIVEN refreshed view-model WHEN requested refresh resolves a different stage THEN ui state updates to the new presentation branch`() =
        runTest {
            fakeRestoreAccountSessionUseCase.result = accountSession().ok()
            fakeResolveAccountSyncStageUseCase.results += listOf(
                SyncStageResult(
                    nextAction = SyncStageAction.None,
                    dataMode = DataMode.LocalOnly,
                    syncState = SyncState(status = SyncStatus.Idle),
                ).ok(),
                SyncStageResult(
                    nextAction = SyncStageAction.AwaitReconciliation,
                    dataMode = DataMode.ReconciliationRequired,
                    syncState = SyncState(status = SyncStatus.BlockedByReconciliation),
                ).ok(),
            )

            sut = createSut(testScope = this)
            val states = collectUiState(uiState = sut.uiState)
            advanceUntilIdle()

            sut.onRefreshRequested()
            advanceUntilIdle()

            val state = states.lastOrNull()
            assertEquals(expected = DataMode.ReconciliationRequired, actual = state?.dataMode)
            assertEquals(expected = true, actual = state?.requiresReconciliation)
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
    val results = mutableListOf<Result<DomainError, SyncStageResult>>()

    override suspend fun invoke(input: AccountSession): Result<DomainError, SyncStageResult> =
        if (results.isNotEmpty()) {
            results.removeAt(index = 0)
        } else {
            result
        }
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
    var wasInvoked: Boolean = false

    override suspend fun invoke(input: String): Result<DomainError, DataMode> {
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
