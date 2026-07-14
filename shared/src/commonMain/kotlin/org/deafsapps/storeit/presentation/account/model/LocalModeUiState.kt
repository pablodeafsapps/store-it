package org.deafsapps.storeit.presentation.account.model

import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus

data class LocalModeUiState(
    val isLoading: Boolean,
    val dataMode: DataMode,
    val syncStatus: SyncStatus,
    val isAuthenticated: Boolean,
    val failureMessage: String?,
) {
    val isLocalOnly: Boolean
        get() = dataMode == DataMode.LocalOnly

    val isSignedOutWithLocalCopy: Boolean
        get() = dataMode == DataMode.SignedOutWithLocalCopy

    val requiresReconciliation: Boolean
        get() = dataMode == DataMode.ReconciliationRequired ||
            syncStatus == SyncStatus.BlockedByReconciliation

    val canChooseKeepLocal: Boolean
        get() = !isLoading && isAuthenticated && requiresReconciliation

    val canChooseKeepRemote: Boolean
        get() = !isLoading && isAuthenticated && requiresReconciliation

    companion object {
        fun getDefault(): LocalModeUiState = LocalModeUiState(
            isLoading = false,
            dataMode = DataMode.LocalOnly,
            syncStatus = SyncStatus.Idle,
            isAuthenticated = false,
            failureMessage = null,
        )
    }
}
