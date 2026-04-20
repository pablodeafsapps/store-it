package org.deafsapps.storeit.presentation.account.model

import androidx.compose.runtime.Immutable
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus

@Immutable
data class AccountUiState(
    val isLoading: Boolean,
    val isAuthenticated: Boolean,
    val accountEmail: String?,
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

    val hasPendingSyncWork: Boolean
        get() = pendingOperationCount > 0 ||
            dataMode == DataMode.AccountBackedPendingSync ||
            syncStatus in pendingSyncStatuses

    val canRetryRestore: Boolean
        get() = isAuthenticated && syncStatus == SyncStatus.RestorePending

    val canRetrySync: Boolean
        get() = isAuthenticated && syncStatus == SyncStatus.Failed

    val requiresReconciliation: Boolean
        get() = dataMode == DataMode.ReconciliationRequired ||
            syncStatus == SyncStatus.BlockedByReconciliation

    companion object {
        fun getDefault(): AccountUiState = AccountUiState(
            isLoading = false,
            isAuthenticated = false,
            accountEmail = null,
            dataMode = DataMode.LocalOnly,
            syncStatus = SyncStatus.Idle,
            pendingOperationCount = 0,
            failureMessage = null,
        )

        private val pendingSyncStatuses: Set<SyncStatus> = setOf(
            SyncStatus.Syncing,
            SyncStatus.PendingUpload,
            SyncStatus.PendingDownload,
            SyncStatus.RestorePending,
        )
    }
}
