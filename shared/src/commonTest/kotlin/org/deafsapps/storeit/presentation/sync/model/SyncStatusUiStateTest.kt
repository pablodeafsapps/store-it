package org.deafsapps.storeit.presentation.sync.model

import kotlin.test.Test
import kotlin.test.assertEquals
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus

class SyncStatusUiStateTest {
    @Test
    fun `GIVEN default state WHEN read THEN exposes local idle status`() {
        val sut = SyncStatusUiState.getDefault()

        assertEquals(expected = true, actual = sut.isLocalOnly)
        assertEquals(expected = AccountSyncStatusState.LocalOnly, actual = sut.accountStatusState)
        assertEquals(expected = AccountSyncHeaderState.LocalOnly, actual = sut.accountHeaderState)
        assertEquals(expected = false, actual = sut.isDataBackedUp)
        assertEquals(expected = false, actual = sut.hasPendingWork)
        assertEquals(expected = false, actual = sut.isRestoreInProgress)
        assertEquals(expected = false, actual = sut.hasAttentionState)
        assertEquals(expected = false, actual = sut.canRetry)
        assertEquals(expected = null, actual = sut.userMessage)
    }

    @Test
    fun `GIVEN synchronized account status WHEN read THEN exposes data backed up`() {
        val sut = SyncStatusUiState(
            isAuthenticated = true,
            dataMode = DataMode.AccountBackedSynchronized,
            syncStatus = SyncStatus.Synchronized,
            pendingOperationCount = 0,
            failureMessage = null,
        )

        assertEquals(expected = true, actual = sut.isDataBackedUp)
        assertEquals(expected = AccountSyncStatusState.BackedUp, actual = sut.accountStatusState)
        assertEquals(expected = AccountSyncHeaderState.Ready, actual = sut.accountHeaderState)
        assertEquals(expected = false, actual = sut.hasPendingWork)
        assertEquals(expected = false, actual = sut.canRetry)
        assertEquals(expected = null, actual = sut.userMessage)
    }

    @Test
    fun `GIVEN pending upload status WHEN read THEN exposes pending messaging`() {
        val sut = SyncStatusUiState(
            isAuthenticated = true,
            dataMode = DataMode.AccountBackedPendingSync,
            syncStatus = SyncStatus.PendingUpload,
            pendingOperationCount = 2,
            failureMessage = null,
        )

        assertEquals(expected = true, actual = sut.hasPendingWork)
        assertEquals(expected = AccountSyncStatusState.Pending, actual = sut.accountStatusState)
        assertEquals(expected = AccountSyncHeaderState.Pending, actual = sut.accountHeaderState)
        assertEquals(expected = false, actual = sut.canRetry)
        assertEquals(
            expected = "Backup pending. Local changes will upload when sync resumes.",
            actual = sut.userMessage,
        )
    }

    @Test
    fun `GIVEN failed status WHEN read THEN exposes retry and failure messaging`() {
        val sut = SyncStatusUiState(
            isAuthenticated = true,
            dataMode = DataMode.AccountBackedPendingSync,
            syncStatus = SyncStatus.Failed,
            pendingOperationCount = 1,
            failureMessage = "Upload failed due to timeout.",
        )

        assertEquals(expected = true, actual = sut.canRetry)
        assertEquals(expected = true, actual = sut.hasAttentionState)
        assertEquals(expected = AccountSyncStatusState.Failed, actual = sut.accountStatusState)
        assertEquals(expected = AccountSyncHeaderState.Attention, actual = sut.accountHeaderState)
        assertEquals(expected = "Upload failed due to timeout.", actual = sut.userMessage)
    }

    @Test
    fun `GIVEN reconciliation required status WHEN read THEN exposes reconciliation messaging`() {
        val sut = SyncStatusUiState(
            isAuthenticated = true,
            dataMode = DataMode.ReconciliationRequired,
            syncStatus = SyncStatus.BlockedByReconciliation,
            pendingOperationCount = 0,
            failureMessage = null,
        )

        assertEquals(expected = true, actual = sut.requiresReconciliation)
        assertEquals(expected = true, actual = sut.hasAttentionState)
        assertEquals(expected = AccountSyncStatusState.ReconciliationRequired, actual = sut.accountStatusState)
        assertEquals(expected = AccountSyncHeaderState.Reconciliation, actual = sut.accountHeaderState)
        assertEquals(expected = false, actual = sut.canRetry)
        assertEquals(
            expected = "Your local and backup data need reconciliation before syncing can continue.",
            actual = sut.userMessage,
        )
    }

    @Test
    fun `GIVEN restore pending status WHEN read THEN exposes restore in progress indicator`() {
        val sut = SyncStatusUiState(
            isAuthenticated = true,
            dataMode = DataMode.AccountBackedPendingSync,
            syncStatus = SyncStatus.RestorePending,
            pendingOperationCount = 0,
            failureMessage = null,
        )

        assertEquals(expected = true, actual = sut.isRestoreInProgress)
        assertEquals(expected = AccountSyncStatusState.RestorePending, actual = sut.accountStatusState)
        assertEquals(expected = AccountSyncHeaderState.Restoring, actual = sut.accountHeaderState)
    }
}
