package org.deafsapps.storeit.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.database.StoreItDatabaseProvider
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.PhotoSyncStatus
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationStatus
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.model.toUnknownDomainError
import org.koin.core.annotation.Single

@Single(
    binds =
        [
            AccountSessionDataSource::class,
            AccountDatasetDataSource::class,
            LocalDatasetStateDataSource::class,
            SyncStateDataSource::class,
            SyncOperationDataSource::class,
            PhotoSyncScopeDataSource::class,
        ],
)
internal class SqlDelightSyncMetadataDataSource(
    private val databaseProvider: StoreItDatabaseProvider,
) :
    AccountSessionDataSource,
    AccountDatasetDataSource,
    LocalDatasetStateDataSource,
    SyncStateDataSource,
    SyncOperationDataSource,
    PhotoSyncScopeDataSource {

  override fun observeActiveAccountSession(): Flow<Result<DomainError, AccountSession?>> =
      databaseProvider.database.storeItDatabaseQueries
          .selectActiveAccountSession(mapper = ::mapAccountSession)
          .asFlow()
          .mapToOneOrNull(context = Dispatchers.IO)
          .map { data -> data.ok() as Result<DomainError, AccountSession?> }
          .catch { throwable -> emit(throwable.toUnknownDomainError().err()) }

  override suspend fun getAccountSession(accountId: String): Result<DomainError, AccountSession?> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .selectAccountSessionByAccountId(
                account_id = accountId,
                mapper = ::mapAccountSession,
            )
            .executeAsOneOrNull()
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun saveAccountSession(
      accountSession: AccountSession
  ): Result<DomainError, AccountSession> =
      try {
        databaseProvider.database.storeItDatabaseQueries.upsertAccountSession(
            account_id = accountSession.accountId,
            email = accountSession.email,
            session_state = accountSession.sessionState.name,
            access_token = null,
            refresh_token = null,
            is_active = if (accountSession.sessionState == SessionState.SignedOut) 0 else 1,
            created_at = accountSession.lastAuthenticatedAt,
            last_authenticated_at = accountSession.lastAuthenticatedAt,
        )
        accountSession.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun clearActiveAccountSessions(): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries.clearActiveAccountSessions().value.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun deleteAccountSession(accountId: String): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .deleteAccountSessionByAccountId(account_id = accountId)
            .value
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .selectRemoteAccountDatasetByAccountId(
                account_id = accountId,
                mapper = ::mapAccountDataset,
            )
            .executeAsOneOrNull()
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun saveAccountDataset(
      accountDataset: AccountDataset
  ): Result<DomainError, AccountDataset> =
      try {
        databaseProvider.database.storeItDatabaseQueries.upsertRemoteAccountDataset(
            account_id = accountDataset.accountId,
            dataset_version = accountDataset.datasetVersion,
            last_synced_at = accountDataset.lastSyncedAt,
        )
        accountDataset.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun deleteAccountDataset(accountId: String): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .deleteRemoteAccountDatasetByAccountId(account_id = accountId)
            .value
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
      databaseProvider.database.storeItDatabaseQueries
          .selectLocalDatasetState(mapper = ::mapLocalDatasetState)
          .asFlow()
          .mapToOneOrNull(context = Dispatchers.IO)
          .map { data -> data.ok() as Result<DomainError, LocalDatasetState?> }
          .catch { throwable -> emit(throwable.toUnknownDomainError().err()) }

  override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .selectLocalDatasetState(mapper = ::mapLocalDatasetState)
            .executeAsOneOrNull()
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState
    ): Result<DomainError, LocalDatasetState> = try {
        databaseProvider.database.storeItDatabaseQueries.upsertLocalDatasetState(
            mode = localDatasetState.mode.name,
            account_id = localDatasetState.accountId,
            last_local_change_at = localDatasetState.lastLocalChangeAt,
            last_remote_sync_at = localDatasetState.lastRemoteSyncAt,
            has_pending_changes = if (localDatasetState.hasPendingChanges) 1 else 0,
        )
        localDatasetState.ok()
    } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
    }

  override suspend fun deleteLocalDatasetState(): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries.deleteLocalDatasetState().value.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> =
      databaseProvider.database.storeItDatabaseQueries
          .selectSyncState(mapper = ::mapSyncState)
          .asFlow()
          .mapToOneOrNull(context = Dispatchers.IO)
          .map { data -> data.ok() as Result<DomainError, SyncState?> }
          .catch { throwable -> emit(throwable.toUnknownDomainError().err()) }

  override suspend fun getSyncState(): Result<DomainError, SyncState?> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .selectSyncState(mapper = ::mapSyncState)
            .executeAsOneOrNull()
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> =
      try {
        databaseProvider.database.storeItDatabaseQueries.upsertSyncState(
            status = syncState.status.name,
            failure_reason = syncState.failureReason,
            last_attempt_at = syncState.lastAttemptAt,
            pending_operation_count = syncState.pendingOperationCount.toLong(),
        )
        syncState.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun deleteSyncState(): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries.deleteSyncState().value.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override fun observePendingSyncOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
      databaseProvider.database.storeItDatabaseQueries
          .selectPendingSyncOperations(mapper = ::mapSyncOperation)
          .asFlow()
          .mapToList(context = Dispatchers.IO)
          .map { data -> data.ok() as Result<DomainError, List<SyncOperation>> }
          .catch { throwable -> emit(throwable.toUnknownDomainError().err()) }

  override suspend fun getPendingSyncOperations(): Result<DomainError, List<SyncOperation>> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .selectPendingSyncOperations(mapper = ::mapSyncOperation)
            .executeAsList()
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun saveSyncOperation(
      syncOperation: SyncOperation
  ): Result<DomainError, SyncOperation> =
      try {
        databaseProvider.database.storeItDatabaseQueries.upsertSyncOperation(
            id = syncOperation.id,
            account_id = syncOperation.accountId,
            entity_type = syncOperation.entityType.name,
            entity_id = syncOperation.entityId,
            operation_type = syncOperation.operationType.name,
            payload_json = syncOperation.payloadJson,
            sync_status = syncOperation.syncStatus.name.lowercase(),
            recorded_at = syncOperation.recordedAt,
            last_attempt_at = syncOperation.lastAttemptAt,
            failure_reason = syncOperation.failureReason,
        )
        syncOperation.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .deleteSyncOperationById(id = operationId)
            .value
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun clearSyncOperations(): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries.deleteAllSyncOperations().value.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override fun observePhotoSyncScope(): Flow<Result<DomainError, List<PhotoSyncScope>>> =
      databaseProvider.database.storeItDatabaseQueries
          .selectPhotoSyncScope(mapper = ::mapPhotoSyncScope)
          .asFlow()
          .mapToList(context = Dispatchers.IO)
          .map { data -> data.ok() as Result<DomainError, List<PhotoSyncScope>> }
          .catch { throwable -> emit(throwable.toUnknownDomainError().err()) }

  override suspend fun getPendingPhotoSyncScope(): Result<DomainError, List<PhotoSyncScope>> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .selectPendingPhotoSyncScope(mapper = ::mapPhotoSyncScope)
            .executeAsList()
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun savePhotoSyncScope(
      photoSyncScope: PhotoSyncScope
  ): Result<DomainError, PhotoSyncScope> =
      try {
        databaseProvider.database.storeItDatabaseQueries.upsertPhotoSyncScope(
            photo_id = photoSyncScope.photoId,
            owner_type = photoSyncScope.ownerType.name,
            owner_id = photoSyncScope.ownerId,
            local_uri = photoSyncScope.localUri,
            remote_url = photoSyncScope.remoteUrl,
            checksum = photoSyncScope.checksum,
            sync_status =
                when (photoSyncScope.syncStatus) {
                  PhotoSyncStatus.PendingUpload -> "pending_upload"
                  PhotoSyncStatus.PendingDelete -> "pending_delete"
                  PhotoSyncStatus.Synced -> "synced"
                  PhotoSyncStatus.Failed -> "failed"
                },
            last_synced_at = photoSyncScope.lastSyncedAt,
        )
        photoSyncScope.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun deletePhotoSyncScope(photoId: String): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries
            .deletePhotoSyncScopeById(photo_id = photoId)
            .value
            .ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  override suspend fun clearPhotoSyncScope(): Result<DomainError, Long> =
      try {
        databaseProvider.database.storeItDatabaseQueries.deleteAllPhotoSyncScope().value.ok()
      } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
      }

  private fun mapAccountSession(
      accountId: String,
      email: String,
      sessionState: String,
      createdAt: Long?,
      lastAuthenticatedAt: Long?,
  ): AccountSession =
      AccountSession(
          accountId = accountId,
          email = email,
          sessionState = sessionState.toSessionState(),
          lastAuthenticatedAt = lastAuthenticatedAt ?: createdAt,
      )

  private fun mapAccountDataset(
      accountId: String,
      datasetVersion: String,
      lastSyncedAt: Long?,
  ): AccountDataset =
      AccountDataset(
          accountId = accountId,
          datasetVersion = datasetVersion,
          lastSyncedAt = lastSyncedAt,
      )

  private fun mapLocalDatasetState(
      mode: String,
      accountId: String?,
      lastLocalChangeAt: Long?,
      lastRemoteSyncAt: Long?,
      hasPendingChanges: Long,
  ): LocalDatasetState =
      LocalDatasetState(
          mode = mode.toDataMode(),
          accountId = accountId,
          lastLocalChangeAt = lastLocalChangeAt,
          lastRemoteSyncAt = lastRemoteSyncAt,
          hasPendingChanges = hasPendingChanges != 0L,
      )

  private fun mapSyncState(
      status: String,
      failureReason: String?,
      lastAttemptAt: Long?,
      pendingOperationCount: Long,
  ): SyncState =
      SyncState(
          status = status.toSyncStatus(),
          failureReason = failureReason,
          lastAttemptAt = lastAttemptAt,
          pendingOperationCount = pendingOperationCount.toInt(),
      )

  private fun mapSyncOperation(
      id: String,
      accountId: String?,
      entityType: String,
      entityId: String,
      operationType: String,
      payloadJson: String?,
      syncStatus: String,
      recordedAt: Long,
      lastAttemptAt: Long?,
      failureReason: String?,
  ): SyncOperation =
      SyncOperation(
          id = id,
          accountId = accountId,
          entityType = enumValueOf(entityType),
          entityId = entityId,
          operationType = enumValueOf(operationType),
          payloadJson = payloadJson,
          syncStatus = syncStatus.toSyncOperationStatus(),
          recordedAt = recordedAt,
          lastAttemptAt = lastAttemptAt,
          failureReason = failureReason,
      )

  private fun mapPhotoSyncScope(
      photoId: String,
      ownerType: String,
      ownerId: String,
      localUri: String,
      remoteUrl: String?,
      checksum: String?,
      syncStatus: String,
      lastSyncedAt: Long?,
  ): PhotoSyncScope =
      PhotoSyncScope(
          photoId = photoId,
          ownerType = enumValueOf(ownerType),
          ownerId = ownerId,
          localUri = localUri,
          remoteUrl = remoteUrl,
          checksum = checksum,
          syncStatus = syncStatus.toPhotoSyncStatus(),
          lastSyncedAt = lastSyncedAt,
      )

  private fun String.toSessionState(): SessionState =
      when (this) {
        SessionState.Active.name -> SessionState.Active
        SessionState.Expired.name -> SessionState.Expired
        SessionState.SignedOut.name -> SessionState.SignedOut
        SessionState.Unavailable.name -> SessionState.Unavailable
        else -> SessionState.Unavailable
      }

  private fun String.toDataMode(): DataMode =
      when (this) {
        DataMode.LocalOnly.name -> DataMode.LocalOnly
        DataMode.AccountBackedSynchronized.name -> DataMode.AccountBackedSynchronized
        DataMode.AccountBackedPendingSync.name -> DataMode.AccountBackedPendingSync
        DataMode.ReconciliationRequired.name -> DataMode.ReconciliationRequired
        DataMode.SignedOutWithLocalCopy.name -> DataMode.SignedOutWithLocalCopy
        else -> DataMode.LocalOnly
      }

  private fun String.toSyncStatus(): SyncStatus =
      when (this) {
        SyncStatus.Idle.name -> SyncStatus.Idle
        SyncStatus.Syncing.name -> SyncStatus.Syncing
        SyncStatus.Synchronized.name -> SyncStatus.Synchronized
        SyncStatus.PendingUpload.name -> SyncStatus.PendingUpload
        SyncStatus.PendingDownload.name -> SyncStatus.PendingDownload
        SyncStatus.Failed.name -> SyncStatus.Failed
        SyncStatus.RestorePending.name -> SyncStatus.RestorePending
        SyncStatus.BlockedByReconciliation.name -> SyncStatus.BlockedByReconciliation
        else -> SyncStatus.Idle
      }

  private fun String.toSyncOperationStatus(): SyncOperationStatus =
      when (lowercase()) {
        "pending" -> SyncOperationStatus.Pending
        "applied" -> SyncOperationStatus.Applied
        "failed" -> SyncOperationStatus.Failed
        else -> SyncOperationStatus.Pending
      }

  private fun String.toPhotoSyncStatus(): PhotoSyncStatus =
      when (lowercase()) {
        "pending_upload" -> PhotoSyncStatus.PendingUpload
        "pending_delete" -> PhotoSyncStatus.PendingDelete
        "synced" -> PhotoSyncStatus.Synced
        "failed" -> PhotoSyncStatus.Failed
        else -> PhotoSyncStatus.PendingUpload
      }
}
