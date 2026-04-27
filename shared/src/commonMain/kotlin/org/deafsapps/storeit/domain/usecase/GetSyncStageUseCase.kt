package org.deafsapps.storeit.domain.usecase

import kotlin.time.Clock
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationType
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.koin.core.annotation.Factory

internal data class GetSyncStageUseCaseInput(
    val session: AccountSession? = null,
    val localDatasetState: LocalDatasetState? = null,
    val remoteDataset: AccountDataset? = null,
    val persistedSyncState: SyncState? = null,
    val pendingOperations: List<SyncOperation> = emptyList(),
    val requiresReconciliation: Boolean = false,
    val syncError: DomainError? = null,
)

internal data class SyncStageResult(
    val nextAction: SyncStageAction,
    val dataMode: DataMode,
    val syncState: SyncState,
)

internal enum class SyncStageAction {
    None,
    UploadPendingChanges,
    RestoreRemoteDataset,
    RetryFailedSync,
    AwaitReconciliation,
}

internal interface GetSyncStageUseCaseType :
    UseCase<GetSyncStageUseCaseInput, Result<DomainError, SyncStageResult>> {
    fun mapFailure(
        error: DomainError,
        pendingOperationCount: Int,
        attemptedAt: Long = Clock.System.now().toEpochMilliseconds(),
    ): SyncState
}

@Factory(binds = [GetSyncStageUseCaseType::class])
internal class GetSyncStageUseCase : GetSyncStageUseCaseType {
    override suspend fun invoke(input: GetSyncStageUseCaseInput): Result<DomainError, SyncStageResult> {
        val validationError = validate(input = input)
        if (validationError != null) {
            return validationError
        }

        val pendingOperationCount = input.pendingOperations.size

        return when {
            input.requiresReconciliation || input.localDatasetState?.mode == DataMode.ReconciliationRequired ->
                SyncStageResult(
                    nextAction = SyncStageAction.AwaitReconciliation,
                    dataMode = DataMode.ReconciliationRequired,
                    syncState = SyncState(
                        status = SyncStatus.BlockedByReconciliation,
                        pendingOperationCount = pendingOperationCount,
                    ),
                ).ok()

            input.syncError != null ->
                SyncStageResult(
                    nextAction = SyncStageAction.RetryFailedSync,
                    dataMode = resolvePendingDataMode(input = input),
                    syncState = mapFailure(
                        error = input.syncError,
                        pendingOperationCount = pendingOperationCount,
                    ),
                ).ok()

            input.persistedSyncState?.status == SyncStatus.Failed ->
                SyncStageResult(
                    nextAction = SyncStageAction.RetryFailedSync,
                    dataMode = resolvePendingDataMode(input = input),
                    syncState = input.persistedSyncState,
                ).ok()

            hasPendingRestore(input = input) ->
                SyncStageResult(
                    nextAction = SyncStageAction.RestoreRemoteDataset,
                    dataMode = DataMode.AccountBackedPendingSync,
                    syncState = SyncState(
                        status = SyncStatus.RestorePending,
                        pendingOperationCount = pendingOperationCount,
                    ),
                ).ok()

            hasPendingUpload(input = input) ->
                SyncStageResult(
                    nextAction = SyncStageAction.UploadPendingChanges,
                    dataMode = DataMode.AccountBackedPendingSync,
                    syncState = SyncState(
                        status = SyncStatus.PendingUpload,
                        pendingOperationCount = pendingOperationCount,
                    ),
                ).ok()

            isSynchronized(input = input) ->
                SyncStageResult(
                    nextAction = SyncStageAction.None,
                    dataMode = DataMode.AccountBackedSynchronized,
                    syncState = SyncState(
                        status = SyncStatus.Synchronized,
                        pendingOperationCount = 0,
                    ),
                ).ok()

            else ->
                SyncStageResult(
                    nextAction = SyncStageAction.None,
                    dataMode = input.localDatasetState?.mode ?: DataMode.LocalOnly,
                    syncState = input.persistedSyncState ?: SyncState(
                        status = SyncStatus.Idle,
                        pendingOperationCount = pendingOperationCount,
                    ),
                ).ok()
        }
    }

    override fun mapFailure(
        error: DomainError,
        pendingOperationCount: Int,
        attemptedAt: Long,
    ): SyncState = SyncState(
        status = SyncStatus.Failed,
        failureReason = error.toSyncFailureMessage(),
        lastAttemptAt = attemptedAt,
        pendingOperationCount = pendingOperationCount,
    )

    private fun validate(input: GetSyncStageUseCaseInput): Result<DomainError, SyncStageResult>? {
        val session = input.session
        if (session != null && session.accountId.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Active session must include an account identifier",
            ).err()
        }

        val requiresAccountId =
            input.localDatasetState?.mode in setOf(
                DataMode.AccountBackedSynchronized,
                DataMode.AccountBackedPendingSync,
                DataMode.ReconciliationRequired,
            )

        if (requiresAccountId && input.localDatasetState?.accountId.isNullOrBlank()) {
            return DomainError.ValidationError(
                field = "localDatasetState.accountId",
                reason = "Account-backed dataset state must include an account identifier",
            ).err()
        }

        return null
    }

    private fun hasPendingRestore(input: GetSyncStageUseCaseInput): Boolean {
        val session = input.session ?: return false
        if (session.sessionState != SessionState.Active) {
            return false
        }

        val localDatasetState = input.localDatasetState

        val hasRestoreOperation = input.pendingOperations.any { operation ->
            operation.operationType == SyncOperationType.Restore
        }
        val persistedRestorePending =
            input.persistedSyncState?.status in setOf(
                SyncStatus.RestorePending,
                SyncStatus.PendingDownload,
            )
        val remoteDatasetNeedsRestore =
            input.remoteDataset != null &&
                localDatasetState?.lastRemoteSyncAt == null &&
                localDatasetState?.hasPendingChanges == false

        return hasRestoreOperation || persistedRestorePending || remoteDatasetNeedsRestore
    }

    private fun hasPendingUpload(input: GetSyncStageUseCaseInput): Boolean {
        val session = input.session ?: return false
        if (session.sessionState != SessionState.Active) {
            return false
        }

        val hasPendingOperations = input.pendingOperations.any { operation ->
            operation.operationType != SyncOperationType.Restore
        }
        val hasPendingLocalChanges = input.localDatasetState?.hasPendingChanges == true

        return hasPendingOperations || hasPendingLocalChanges
    }

    private fun isSynchronized(input: GetSyncStageUseCaseInput): Boolean {
        val session = input.session ?: return false
        if (session.sessionState != SessionState.Active) {
            return false
        }

        val hasRemoteCheckpoint = input.remoteDataset != null || input.localDatasetState?.lastRemoteSyncAt != null
        val hasPendingChanges = input.localDatasetState?.hasPendingChanges == true || input.pendingOperations.isNotEmpty()
        val isAccountBackedMode =
            input.localDatasetState?.mode in setOf(
                DataMode.AccountBackedSynchronized,
                DataMode.AccountBackedPendingSync,
            )

        return hasRemoteCheckpoint && !hasPendingChanges && isAccountBackedMode
    }

    private fun resolvePendingDataMode(input: GetSyncStageUseCaseInput): DataMode =
        when (input.localDatasetState?.mode) {
            DataMode.ReconciliationRequired -> DataMode.ReconciliationRequired
            DataMode.SignedOutWithLocalCopy -> DataMode.SignedOutWithLocalCopy
            else -> DataMode.AccountBackedPendingSync
        }
}

private fun DomainError.toSyncFailureMessage(): String = when (this) {
    is DomainError.Unknown ->
        message.takeUnless { it == "Unknown error" } ?: "Synchronization failed. Retry is required."
    is DomainError.NotFound -> "Synchronization failed because required $resource data could not be found."
    is DomainError.ValidationError -> "Synchronization failed: $reason"
}
