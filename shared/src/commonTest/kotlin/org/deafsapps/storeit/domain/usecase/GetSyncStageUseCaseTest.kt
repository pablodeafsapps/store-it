package org.deafsapps.storeit.domain.usecase

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.deafsapps.storeit.base.Err
import org.deafsapps.storeit.base.Ok
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationStatus
import org.deafsapps.storeit.domain.model.SyncOperationType
import org.deafsapps.storeit.domain.model.SyncStatus

class GetSyncStageUseCaseTest {
    private lateinit var sut: GetSyncStageUseCase

    @BeforeTest
    fun setUp() {
        sut = GetSyncStageUseCase()
    }

    @Test
    fun invoke_returns_synchronized_state_when_account_backed_data_is_aligned() {
        val result = sut.invoke(
            input = GetSyncStageUseCaseInput(
                session = activeSession(),
                localDatasetState = synchronizedLocalDatasetState(),
                remoteDataset = AccountDataset(
                    accountId = "account-1",
                    datasetVersion = "v1",
                    lastSyncedAt = 20L,
                ),
            ),
        )

        val stage = assertIs<Ok<SyncStageResult>>(result).value

        assertEquals(expected = SyncStageAction.None, actual = stage.nextAction)
        assertEquals(expected = DataMode.AccountBackedSynchronized, actual = stage.dataMode)
        assertEquals(expected = SyncStatus.Synchronized, actual = stage.syncState.status)
    }

    @Test
    fun invoke_returns_pending_upload_state_when_local_changes_exist() {
        val result = sut.invoke(
            input = GetSyncStageUseCaseInput(
                session = activeSession(),
                localDatasetState = LocalDatasetState(
                    mode = DataMode.AccountBackedPendingSync,
                    accountId = "account-1",
                    hasPendingChanges = true,
                ),
            ),
        )

        val stage = assertIs<Ok<SyncStageResult>>(result).value

        assertEquals(expected = SyncStageAction.UploadPendingChanges, actual = stage.nextAction)
        assertEquals(expected = SyncStatus.PendingUpload, actual = stage.syncState.status)
    }

    @Test
    fun invoke_returns_restore_pending_state_when_restore_operation_exists() {
        val result = sut.invoke(
            input = GetSyncStageUseCaseInput(
                session = activeSession(),
                localDatasetState = LocalDatasetState(
                    mode = DataMode.AccountBackedPendingSync,
                    accountId = "account-1",
                    hasPendingChanges = false,
                ),
                pendingOperations = listOf(
                    SyncOperation(
                        id = "restore-1",
                        accountId = "account-1",
                        entityType = SyncEntityType.Dataset,
                        entityId = "dataset-1",
                        operationType = SyncOperationType.Restore,
                        syncStatus = SyncOperationStatus.Pending,
                    ),
                ),
            ),
        )

        val stage = assertIs<Ok<SyncStageResult>>(result).value

        assertEquals(expected = SyncStageAction.RestoreRemoteDataset, actual = stage.nextAction)
        assertEquals(expected = SyncStatus.RestorePending, actual = stage.syncState.status)
    }

    @Test
    fun invoke_returns_failed_state_when_sync_error_is_present() {
        val result = sut.invoke(
            input = GetSyncStageUseCaseInput(
                session = activeSession(),
                localDatasetState = LocalDatasetState(
                    mode = DataMode.AccountBackedPendingSync,
                    accountId = "account-1",
                    hasPendingChanges = true,
                ),
                syncError = DomainError.Unknown(),
            ),
        )

        val stage = assertIs<Ok<SyncStageResult>>(result).value

        assertEquals(expected = SyncStageAction.RetryFailedSync, actual = stage.nextAction)
        assertEquals(expected = SyncStatus.Failed, actual = stage.syncState.status)
        assertEquals(
            expected = "Synchronization failed. Retry is required.",
            actual = stage.syncState.failureReason,
        )
    }

    @Test
    fun invoke_returns_reconciliation_required_state_when_flagged() {
        val result = sut.invoke(
            input = GetSyncStageUseCaseInput(
                session = activeSession(),
                localDatasetState = LocalDatasetState(
                    mode = DataMode.ReconciliationRequired,
                    accountId = "account-1",
                ),
                requiresReconciliation = true,
            ),
        )

        val stage = assertIs<Ok<SyncStageResult>>(result).value

        assertEquals(expected = SyncStageAction.AwaitReconciliation, actual = stage.nextAction)
        assertEquals(expected = DataMode.ReconciliationRequired, actual = stage.dataMode)
        assertEquals(expected = SyncStatus.BlockedByReconciliation, actual = stage.syncState.status)
    }

    @Test
    fun invoke_returns_validation_error_when_account_backed_state_has_no_account_id() {
        val result = sut.invoke(
            input = GetSyncStageUseCaseInput(
                session = activeSession(),
                localDatasetState = LocalDatasetState(
                    mode = DataMode.AccountBackedPendingSync,
                    accountId = null,
                ),
            ),
        )

        val error = assertIs<Err<DomainError>>(result).error

        assertIs<DomainError.ValidationError>(error)
    }

    @Test
    fun mapFailure_returns_failed_sync_state_with_reason_and_pending_count() {
        val result = sut.mapFailure(
            error = DomainError.ValidationError(
                field = "sync",
                reason = "Remote checkpoint mismatch",
            ),
            pendingOperationCount = 2,
            attemptedAt = 55L,
        )

        assertEquals(expected = SyncStatus.Failed, actual = result.status)
        assertEquals(expected = 2, actual = result.pendingOperationCount)
        assertEquals(expected = 55L, actual = result.lastAttemptAt)
        assertEquals(
            expected = "Synchronization failed: Remote checkpoint mismatch",
            actual = result.failureReason,
        )
    }

    private fun activeSession() = AccountSession(
        accountId = "account-1",
        email = "user@example.com",
        sessionState = SessionState.Active,
        lastAuthenticatedAt = 10L,
    )

    private fun synchronizedLocalDatasetState() = LocalDatasetState(
        mode = DataMode.AccountBackedSynchronized,
        accountId = "account-1",
        lastLocalChangeAt = 15L,
        lastRemoteSyncAt = 20L,
        hasPendingChanges = false,
    )
}
