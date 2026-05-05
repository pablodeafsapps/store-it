package org.deafsapps.storeit.presentation.account.model

import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus

data class AccountUiState(
    val isLoading: Boolean,
    val isSubmitting: Boolean = false,
    val authMode: AccountAuthMode = AccountAuthMode.SignIn,
    val emailInput: String = "",
    val passwordInput: String = "",
    val isAuthenticated: Boolean,
    val accountEmail: String?,
    val dataMode: DataMode,
    val syncStatus: SyncStatus,
    val pendingOperationCount: Int,
    val failureMessage: String?,
) {
    val isLocalOnly: Boolean
        get() = dataMode == DataMode.LocalOnly

    val isAccountReady: Boolean
        get() = isAuthenticated &&
            dataMode == DataMode.AccountBackedSynchronized &&
            syncStatus == SyncStatus.Synchronized

    val isRestoreInProgress: Boolean
        get() = isAuthenticated &&
            (isLoading ||
                syncStatus == SyncStatus.RestorePending ||
                syncStatus == SyncStatus.PendingDownload)

    val isDataBackedUp: Boolean
        get() = isAccountReady

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

    val isSignInMode: Boolean
        get() = authMode == AccountAuthMode.SignIn

    val isSignUpMode: Boolean
        get() = authMode == AccountAuthMode.SignUp

    val canSubmitCredentials: Boolean
        get() = !isLoading &&
            !isSubmitting &&
            emailInput.isNotBlank() &&
            passwordInput.isNotBlank()

    val canSignOut: Boolean
        get() = isAuthenticated && !isLoading && !isSubmitting

    companion object {
        fun getDefault(): AccountUiState = AccountUiState(
            isLoading = false,
            isSubmitting = false,
            authMode = AccountAuthMode.SignIn,
            emailInput = "",
            passwordInput = "",
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

enum class AccountAuthMode {
    SignIn,
    SignUp,
}
