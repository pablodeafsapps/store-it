package org.deafsapps.storeit.presentation.sync.model

import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus

internal data class SyncStatusUiState(
    val isAuthenticated: Boolean,
    val dataMode: DataMode,
    val syncStatus: SyncStatus,
    val pendingOperationCount: Int,
    val failureMessage: String?,
) {
    val isLocalOnly: Boolean
        get() = dataMode == DataMode.LocalOnly

    val isDataBackedUp: Boolean
        get() = isAuthenticated &&
            dataMode == DataMode.AccountBackedSynchronized &&
            syncStatus == SyncStatus.Synchronized

    val hasPendingWork: Boolean
        get() = pendingOperationCount > 0 ||
            dataMode == DataMode.AccountBackedPendingSync ||
            syncStatus in pendingStatuses

    val requiresReconciliation: Boolean
        get() = dataMode == DataMode.ReconciliationRequired ||
            syncStatus == SyncStatus.BlockedByReconciliation

    val canRetry: Boolean
        get() = isAuthenticated && syncStatus in retryableStatuses

    val userMessage: String?
        get() = when {
            requiresReconciliation -> "Your local and backup data need reconciliation before syncing can continue."
            syncStatus == SyncStatus.Failed -> failureMessage ?: "Sync failed. Retry to continue backing up your data."
            syncStatus == SyncStatus.RestorePending -> "Restoring your backup is pending. Retry when ready."
            syncStatus == SyncStatus.PendingUpload -> "Backup pending. Local changes will upload when sync resumes."
            syncStatus == SyncStatus.PendingDownload -> "Backup restore pending. Remote updates will download when sync resumes."
            else -> null
        }

    companion object {
        fun getDefault(): SyncStatusUiState = SyncStatusUiState(
            isAuthenticated = false,
            dataMode = DataMode.LocalOnly,
            syncStatus = SyncStatus.Idle,
            pendingOperationCount = 0,
            failureMessage = null,
        )

        private val pendingStatuses: Set<SyncStatus> = setOf(
            SyncStatus.Syncing,
            SyncStatus.PendingUpload,
            SyncStatus.PendingDownload,
            SyncStatus.RestorePending,
        )

        private val retryableStatuses: Set<SyncStatus> = setOf(
            SyncStatus.Failed,
            SyncStatus.RestorePending,
        )
    }
}
