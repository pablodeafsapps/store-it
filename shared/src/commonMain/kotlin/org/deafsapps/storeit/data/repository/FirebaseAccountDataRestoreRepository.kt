package org.deafsapps.storeit.data.repository

import kotlin.time.Clock
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.suspendFlatMap
import org.deafsapps.storeit.data.datasource.AccountRemoteDataSource
import org.deafsapps.storeit.data.datasource.RemoteAccountSnapshot
import org.deafsapps.storeit.data.datasource.RemotePhotoReference
import org.deafsapps.storeit.domain.gateway.AccountRestoreMetadataGateway
import org.deafsapps.storeit.domain.gateway.ItemRestoreGateway
import org.deafsapps.storeit.domain.gateway.PhotoRestoreGateway
import org.deafsapps.storeit.domain.gateway.RackRestoreGateway
import org.deafsapps.storeit.domain.gateway.SlotRestoreGateway
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
import org.koin.core.annotation.Single

@Single(binds = [AccountDataRestoreRepository::class])
@Suppress("LongParameterList")
internal class FirebaseAccountDataRestoreRepository(
    private val accountRemoteDataSource: AccountRemoteDataSource,
    private val accountRestoreMetadataGateway: AccountRestoreMetadataGateway,
    private val rackRestoreGateway: RackRestoreGateway,
    private val slotRestoreGateway: SlotRestoreGateway,
    private val itemRestoreGateway: ItemRestoreGateway,
    private val photoRestoreGateway: PhotoRestoreGateway,
) : AccountDataRestoreRepository {

    override suspend fun restoreAccountData(session: AccountSession): Result<DomainError, Unit> {
        val localDatasetStateResult = accountRestoreMetadataGateway.getLocalDatasetState()
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
        replaceLocalAccountDataset(snapshot = snapshot)
            .suspendFlatMap {
                saveSynchronizedMetadata(
                    session = session,
                    snapshot = snapshot,
                    previousState = previousState,
                )
            }

    private suspend fun replaceLocalAccountDataset(snapshot: RemoteAccountSnapshot): Result<DomainError, Unit> =
        rackRestoreGateway.replaceRestoredRacks(racks = snapshot.racks)
            .suspendFlatMap {
                slotRestoreGateway.replaceRestoredSlots(slots = snapshot.slots)
            }
            .suspendFlatMap {
                itemRestoreGateway.replaceRestoredItems(items = snapshot.items)
            }
            .suspendFlatMap {
                photoRestoreGateway.replaceRestoredPhotoSyncScope(
                    photoSyncScope = snapshot.photos.map { photo ->
                        photo.toPhotoSyncScope(syncedAt = snapshot.syncCheckpoint.updatedAt)
                    },
                )
            }
            .map { Unit }

    private suspend fun saveSynchronizedMetadata(
        session: AccountSession,
        snapshot: RemoteAccountSnapshot,
        previousState: LocalDatasetState?,
    ): Result<DomainError, Unit> =
        accountRestoreMetadataGateway.markRestoreSynchronized(
            accountDataset = AccountDataset(
                accountId = session.accountId,
                datasetVersion = snapshot.syncCheckpoint.value,
                lastSyncedAt = snapshot.syncCheckpoint.updatedAt,
            ),
            localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedSynchronized,
                accountId = session.accountId,
                lastLocalChangeAt = previousState?.lastLocalChangeAt,
                lastRemoteSyncAt = snapshot.syncCheckpoint.updatedAt,
                hasPendingChanges = false,
            ),
            syncState = SyncState(
                status = SyncStatus.Synchronized,
                failureReason = null,
                lastAttemptAt = snapshot.syncCheckpoint.updatedAt,
                pendingOperationCount = 0,
            ),
        )

    private suspend fun markRestorePending(
        session: AccountSession,
        previousState: LocalDatasetState?,
        error: DomainError,
    ) {
        val attemptedAt = Clock.System.now().toEpochMilliseconds()
        accountRestoreMetadataGateway.markRestorePending(
            localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedPendingSync,
                accountId = session.accountId,
                lastLocalChangeAt = previousState?.lastLocalChangeAt,
                lastRemoteSyncAt = previousState?.lastRemoteSyncAt,
                hasPendingChanges = previousState?.hasPendingChanges ?: false,
            ),
            syncState = SyncState(
                status = SyncStatus.RestorePending,
                failureReason = error.toRestoreFailureMessage(),
                lastAttemptAt = attemptedAt,
                pendingOperationCount = 0,
            ),
        )
    }
}

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
    is DomainError.AuthenticationFailed,
    is DomainError.ServiceUnavailable,
    is DomainError.ConfigurationError -> message
    is DomainError.NotFound -> "Restore failed because required $resource data could not be found."
    is DomainError.Unknown -> message
    is DomainError.ValidationError -> "Restore failed: $reason"
}
