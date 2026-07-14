package org.deafsapps.storeit.presentation.sync.model

import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus

data class SyncStatusUiState(
    val isAuthenticated: Boolean,
    val dataMode: DataMode,
    val syncStatus: SyncStatus,
    val pendingOperationCount: Int,
    val failureMessage: String?,
) {
    val accountStatusState: AccountSyncStatusState
        get() = when {
            requiresReconciliation -> AccountSyncStatusState.ReconciliationRequired
            syncStatus == SyncStatus.RestorePending -> AccountSyncStatusState.RestorePending
            syncStatus == SyncStatus.Failed -> AccountSyncStatusState.Failed
            isDataBackedUp -> AccountSyncStatusState.BackedUp
            hasPendingWork -> AccountSyncStatusState.Pending
            isAuthenticated -> AccountSyncStatusState.SignedIn
            else -> AccountSyncStatusState.LocalOnly
        }

    val accountHeaderState: AccountSyncHeaderState
        get() = when {
            requiresReconciliation -> AccountSyncHeaderState.Reconciliation
            syncStatus == SyncStatus.Failed -> AccountSyncHeaderState.Attention
            isRestoreInProgress -> AccountSyncHeaderState.Restoring
            isDataBackedUp -> AccountSyncHeaderState.Ready
            hasPendingWork -> AccountSyncHeaderState.Pending
            isAuthenticated -> AccountSyncHeaderState.Connected
            else -> AccountSyncHeaderState.LocalOnly
        }

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

    val isRestoreInProgress: Boolean
        get() = syncStatus == SyncStatus.RestorePending ||
            syncStatus == SyncStatus.PendingDownload

    val requiresReconciliation: Boolean
        get() = dataMode == DataMode.ReconciliationRequired ||
            syncStatus == SyncStatus.BlockedByReconciliation

    val hasAttentionState: Boolean
        get() = requiresReconciliation || syncStatus == SyncStatus.Failed

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

enum class AccountSyncStatusState {
    LocalOnly,
    SignedIn,
    Pending,
    BackedUp,
    Failed,
    RestorePending,
    ReconciliationRequired,
}

enum class AccountSyncHeaderState {
    LocalOnly,
    Connected,
    Pending,
    Ready,
    Restoring,
    Attention,
    Reconciliation,
}
