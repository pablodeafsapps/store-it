package org.deafsapps.storeit.data.repository

import kotlin.time.Clock
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.base.suspendFlatMap
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.AccountRemoteDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.PhotoSyncScopeDataSource
import org.deafsapps.storeit.data.datasource.RemoteAccountSnapshot
import org.deafsapps.storeit.data.datasource.RemotePhotoReference
import org.deafsapps.storeit.data.datasource.SyncStateDataSource
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.PhotoSyncStatus
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.repository.AccountDataRestoreRepository
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Single

@Single(binds = [AccountDataRestoreRepository::class])
@Suppress("LongParameterList")
internal class FirebaseAccountDataRestoreRepository(
    private val accountRemoteDataSource: AccountRemoteDataSource,
    private val accountDatasetDataSource: AccountDatasetDataSource,
    private val localDatasetStateDataSource: LocalDatasetStateDataSource,
    private val syncStateDataSource: SyncStateDataSource,
    private val photoSyncScopeDataSource: PhotoSyncScopeDataSource,
    private val rackRepository: RackRepository,
    private val slotRepository: SlotRepository,
    private val itemRepository: ItemRepository,
) : AccountDataRestoreRepository {

    override suspend fun restoreAccountData(session: AccountSession): Result<DomainError, Unit> {
        val localDatasetStateResult = localDatasetStateDataSource.getLocalDatasetState()
        val previousState = localDatasetStateResult.getOrNull()
        val restoreResult = localDatasetStateResult.suspendFlatMap { localDatasetState ->
            accountRemoteDataSource.fetchSnapshot(accountId = session.accountId)
                .suspendFlatMap { snapshot ->
                    validateSnapshot(
                        session = session,
                        snapshot = snapshot,
                    )?.err() ?: applySnapshot(
                        session = session,
                        snapshot = snapshot,
                        previousState = localDatasetState,
                    )
                }
        }

        restoreResult.failureOrNull()?.let { error ->
            markRestorePending(
                session = session,
                previousState = previousState,
                error = error,
            )
        }
        return restoreResult
    }

    private fun validateSnapshot(
        session: AccountSession,
        snapshot: RemoteAccountSnapshot,
    ): DomainError? = when {
        snapshot.accountId != session.accountId -> DomainError.ValidationError(
            field = "snapshot.accountId",
            reason = "Remote snapshot account does not match the active session",
        )

        else -> null
    }

    private suspend fun applySnapshot(
        session: AccountSession,
        snapshot: RemoteAccountSnapshot,
        previousState: LocalDatasetState?,
    ): Result<DomainError, Unit> =
        replaceLocalAccountDatasetIfNeeded(
            session = session,
            previousState = previousState,
        ).suspendFlatMap {
            applyEntities(snapshot = snapshot)
        }.suspendFlatMap {
            savePhotoSyncScopes(snapshot = snapshot)
        }.suspendFlatMap {
            saveSynchronizedMetadata(
                session = session,
                snapshot = snapshot,
                previousState = previousState,
            )
        }

    private suspend fun replaceLocalAccountDatasetIfNeeded(
        session: AccountSession,
        previousState: LocalDatasetState?,
    ): Result<DomainError, Unit> {
        if (!previousState.shouldReplaceLocalAccountDataset(session = session)) {
            return Unit.ok()
        }

        itemRepository.clear()
        slotRepository.clear()
        rackRepository.clear()
        return photoSyncScopeDataSource.clearPhotoSyncScope()
            .map { Unit }
    }

    private suspend fun applyEntities(snapshot: RemoteAccountSnapshot): Result<DomainError, Unit> {
        snapshot.racks.forEach { rack ->
            rackRepository.saveRack(rack = rack)
                .failureOrNull()
                ?.let { error -> return error.err() }
        }
        snapshot.slots.forEach { slot ->
            slotRepository.saveSlot(slot = slot)
                .failureOrNull()
                ?.let { error -> return error.err() }
        }
        snapshot.items.forEach { item ->
            itemRepository.saveItem(item = item)
                .failureOrNull()
                ?.let { error -> return error.err() }
        }

        return Unit.ok()
    }

    private suspend fun savePhotoSyncScopes(snapshot: RemoteAccountSnapshot): Result<DomainError, Unit> {
        snapshot.photos.forEach { photo ->
            photoSyncScopeDataSource.savePhotoSyncScope(
                photoSyncScope = photo.toPhotoSyncScope(
                    syncedAt = snapshot.syncCheckpoint.updatedAt,
                ),
            ).failureOrNull()
                ?.let { error -> return error.err() }
        }

        return Unit.ok()
    }

    private suspend fun saveSynchronizedMetadata(
        session: AccountSession,
        snapshot: RemoteAccountSnapshot,
        previousState: LocalDatasetState?,
    ): Result<DomainError, Unit> =
        accountDatasetDataSource.saveAccountDataset(
            accountDataset = AccountDataset(
                accountId = session.accountId,
                datasetVersion = snapshot.syncCheckpoint.value,
                lastSyncedAt = snapshot.syncCheckpoint.updatedAt,
            ),
        ).suspendFlatMap {
            localDatasetStateDataSource.saveLocalDatasetState(
                localDatasetState = LocalDatasetState(
                    mode = DataMode.AccountBackedSynchronized,
                    accountId = session.accountId,
                    lastLocalChangeAt = previousState?.lastLocalChangeAt,
                    lastRemoteSyncAt = snapshot.syncCheckpoint.updatedAt,
                    hasPendingChanges = false,
                ),
            )
        }.suspendFlatMap {
            syncStateDataSource.saveSyncState(
                syncState = SyncState(
                    status = SyncStatus.Synchronized,
                    failureReason = null,
                    lastAttemptAt = snapshot.syncCheckpoint.updatedAt,
                    pendingOperationCount = 0,
                ),
            )
        }.map { Unit }

    private suspend fun markRestorePending(
        session: AccountSession,
        previousState: LocalDatasetState?,
        error: DomainError,
    ) {
        val attemptedAt = Clock.System.now().toEpochMilliseconds()
        localDatasetStateDataSource.saveLocalDatasetState(
            localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedPendingSync,
                accountId = session.accountId,
                lastLocalChangeAt = previousState?.lastLocalChangeAt,
                lastRemoteSyncAt = previousState?.lastRemoteSyncAt,
                hasPendingChanges = previousState?.hasPendingChanges ?: false,
            ),
        )
        syncStateDataSource.saveSyncState(
            syncState = SyncState(
                status = SyncStatus.RestorePending,
                failureReason = error.toRestoreFailureMessage(),
                lastAttemptAt = attemptedAt,
                pendingOperationCount = 0,
            ),
        )
    }
}

private fun LocalDatasetState?.shouldReplaceLocalAccountDataset(session: AccountSession): Boolean =
    this?.accountId == session.accountId &&
        mode in setOf(
            DataMode.AccountBackedSynchronized,
            DataMode.AccountBackedPendingSync,
            DataMode.SignedOutWithLocalCopy,
        )

private fun RemotePhotoReference.toPhotoSyncScope(syncedAt: Long?): PhotoSyncScope = PhotoSyncScope(
    photoId = photoId,
    ownerType = SyncEntityType.Photo,
    ownerId = photoId,
    localUri = remoteUrl,
    remoteUrl = remoteUrl,
    checksum = checksum,
    syncStatus = PhotoSyncStatus.Synced,
    lastSyncedAt = syncedAt,
)

private fun DomainError.toRestoreFailureMessage(): String = when (this) {
    is DomainError.NotFound -> "Restore failed because required $resource data could not be found."
    is DomainError.Unknown -> message
    is DomainError.ValidationError -> "Restore failed: $reason"
}
