package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationType
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.repository.AccountDataRestoreRepository
import org.deafsapps.storeit.domain.repository.SyncRepository
import org.koin.core.annotation.Factory

internal interface UploadPendingAccountDataUseCaseType : UseCase<String, Result<DomainError, Unit>>

internal interface ResolveAccountSyncStageUseCaseType :
    UseCase<AccountSession, Result<DomainError, SyncStageResult>>

internal interface RetryPendingSyncUseCaseType :
    UseCase<AccountSession, Result<DomainError, SyncStageResult>>

internal interface CatchUpSignedInSyncUseCaseType :
    UseCase<AccountSession, Result<DomainError, SyncStageResult>>

@Factory(binds = [UploadPendingAccountDataUseCaseType::class])
internal class UploadPendingAccountDataUseCase(
    private val syncRepository: SyncRepository,
) : UploadPendingAccountDataUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, Unit> {
        if (input.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Account ID cannot be blank",
            ).err()
        }

        val pendingOperationCount = firstResultOrUnknown(
            flow = syncRepository.observePendingOperations(),
            missingEmissionMessage = "Pending operations flow emitted no values.",
        ).map { operations -> operations.size }.getOrNull() ?: 0

        return syncRepository.saveSyncState(
            syncState = SyncState(
                status = if (pendingOperationCount > 0) SyncStatus.PendingUpload else SyncStatus.Synchronized,
                pendingOperationCount = pendingOperationCount,
            ),
        ).map { Unit }
    }
}

@Factory(binds = [ResolveAccountSyncStageUseCaseType::class])
internal class ResolveAccountSyncStageUseCase(
    private val syncRepository: SyncRepository,
    private val getSyncStageUseCase: GetSyncStageUseCaseType,
) : ResolveAccountSyncStageUseCaseType {
    override suspend fun invoke(input: AccountSession): Result<DomainError, SyncStageResult> =
        loadSyncSnapshot(
            session = input,
            syncRepository = syncRepository,
        ).flatMap { snapshot -> getSyncStageUseCase(input = snapshot.toStageInput(session = input)) }
}

@Factory(binds = [RetryPendingSyncUseCaseType::class])
internal class RetryPendingSyncUseCase(
    private val syncRepository: SyncRepository,
    private val accountDataRestoreRepository: AccountDataRestoreRepository,
    private val uploadPendingAccountDataUseCase: UploadPendingAccountDataUseCaseType,
    private val getSyncStageUseCase: GetSyncStageUseCaseType,
) : RetryPendingSyncUseCaseType {
    override suspend fun invoke(input: AccountSession): Result<DomainError, SyncStageResult> =
        loadSyncSnapshot(
            session = input,
            syncRepository = syncRepository,
        ).flatMap { snapshot ->
            getSyncStageUseCase(
                input = snapshot.toStageInput(session = input),
            ).flatMap { stage ->
                executeRetryAction(
                    session = input,
                    snapshot = snapshot,
                    stage = stage,
                    failedStagePolicy = FailedStagePolicy.TriggerRetryAction,
                    accountDataRestoreRepository = accountDataRestoreRepository,
                    uploadPendingAccountDataUseCase = uploadPendingAccountDataUseCase,
                ).map { stage }
            }
        }
}

@Factory(binds = [CatchUpSignedInSyncUseCaseType::class])
internal class CatchUpSignedInSyncUseCase(
    private val syncRepository: SyncRepository,
    private val accountDataRestoreRepository: AccountDataRestoreRepository,
    private val uploadPendingAccountDataUseCase: UploadPendingAccountDataUseCaseType,
    private val getSyncStageUseCase: GetSyncStageUseCaseType,
) : CatchUpSignedInSyncUseCaseType {
    override suspend fun invoke(input: AccountSession): Result<DomainError, SyncStageResult> =
        loadSyncSnapshot(
            session = input,
            syncRepository = syncRepository,
        ).flatMap { snapshot ->
            getSyncStageUseCase(
                input = snapshot.toStageInput(session = input),
            ).flatMap { stage ->
                executeRetryAction(
                    session = input,
                    snapshot = snapshot,
                    stage = stage,
                    failedStagePolicy = FailedStagePolicy.SkipRetryAction,
                    accountDataRestoreRepository = accountDataRestoreRepository,
                    uploadPendingAccountDataUseCase = uploadPendingAccountDataUseCase,
                ).map { stage }
            }
        }
}

private suspend fun executeRetryAction(
    session: AccountSession,
    snapshot: SyncRepositorySnapshot,
    stage: SyncStageResult,
    failedStagePolicy: FailedStagePolicy,
    accountDataRestoreRepository: AccountDataRestoreRepository,
    uploadPendingAccountDataUseCase: UploadPendingAccountDataUseCaseType,
): Result<DomainError, Unit> = when (stage.nextAction) {
        SyncStageAction.None,
        SyncStageAction.AwaitReconciliation -> Unit.ok()
        SyncStageAction.RestoreRemoteDataset ->
            accountDataRestoreRepository.restoreAccountData(session = session)
        SyncStageAction.UploadPendingChanges ->
            uploadPendingAccountDataUseCase(input = session.accountId)
        SyncStageAction.RetryFailedSync ->
            when (failedStagePolicy) {
                FailedStagePolicy.SkipRetryAction -> Unit.ok()
                FailedStagePolicy.TriggerRetryAction ->
                    if (snapshot.shouldRetryRestore()) {
                        accountDataRestoreRepository.restoreAccountData(session = session)
                    } else {
                        uploadPendingAccountDataUseCase(input = session.accountId)
                    }
            }
}

private enum class FailedStagePolicy {
    TriggerRetryAction,
    SkipRetryAction,
}

private suspend fun loadSyncSnapshot(
    session: AccountSession,
    syncRepository: SyncRepository,
): Result<DomainError, SyncRepositorySnapshot> {
    val localDatasetStateResult = firstResultOrUnknown(
        flow = syncRepository.observeLocalDatasetState(),
        missingEmissionMessage = "Local dataset state flow emitted no values.",
    )
    val syncStateResult = firstResultOrUnknown(
        flow = syncRepository.observeSyncState(),
        missingEmissionMessage = "Sync state flow emitted no values.",
    )
    val pendingOperationsResult = firstResultOrUnknown(
        flow = syncRepository.observePendingOperations(),
        missingEmissionMessage = "Pending operations flow emitted no values.",
    )
    val remoteDatasetResult = syncRepository.getAccountDataset(accountId = session.accountId)

    // if any 'Err' is found, it's returned immediately
    listOf(
        localDatasetStateResult,
        syncStateResult,
        pendingOperationsResult,
        remoteDatasetResult,
    ).firstNotNullOfOrNull { result -> result.failureOrNull() }?.let { firstError -> return firstError.err() }

    return SyncRepositorySnapshot(
        localDatasetState = localDatasetStateResult.getOrNull(),
        persistedSyncState = syncStateResult.getOrNull(),
        pendingOperations = pendingOperationsResult.getOrNull().orEmpty(),
        remoteDataset = remoteDatasetResult.getOrNull(),
    ).ok()
}

private suspend fun <T> firstResultOrUnknown(
    flow: Flow<Result<DomainError, T>>,
    missingEmissionMessage: String,
): Result<DomainError, T> =
    flow.firstOrNull() ?: DomainError.Unknown(message = missingEmissionMessage).err()

private data class SyncRepositorySnapshot(
    val localDatasetState: LocalDatasetState?,
    val persistedSyncState: SyncState?,
    val pendingOperations: List<SyncOperation>,
    val remoteDataset: AccountDataset?,
) {
    fun toStageInput(session: AccountSession): GetSyncStageUseCaseInput = GetSyncStageUseCaseInput(
        session = session,
        localDatasetState = localDatasetState,
        remoteDataset = remoteDataset,
        persistedSyncState = persistedSyncState,
        pendingOperations = pendingOperations,
    )

    fun shouldRetryRestore(): Boolean =
        pendingOperations.any { operation -> operation.operationType == SyncOperationType.Restore } ||
            persistedSyncState?.status in setOf(SyncStatus.RestorePending, SyncStatus.PendingDownload)
}
