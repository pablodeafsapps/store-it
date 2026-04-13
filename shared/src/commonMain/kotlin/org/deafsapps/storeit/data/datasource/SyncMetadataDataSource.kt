package org.deafsapps.storeit.data.datasource

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncState

internal interface AccountSessionDataSource {
    fun observeActiveAccountSession(): Flow<Result<DomainError, AccountSession?>>

    suspend fun getAccountSession(accountId: String): Result<DomainError, AccountSession?>

    suspend fun saveAccountSession(accountSession: AccountSession): Result<DomainError, AccountSession>

    suspend fun clearActiveAccountSessions(): Result<DomainError, Long>

    suspend fun deleteAccountSession(accountId: String): Result<DomainError, Long>
}

internal interface AccountDatasetDataSource {
    suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?>

    suspend fun saveAccountDataset(accountDataset: AccountDataset): Result<DomainError, AccountDataset>

    suspend fun deleteAccountDataset(accountId: String): Result<DomainError, Long>
}

internal interface LocalDatasetStateDataSource {
    fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>>

    suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?>

    suspend fun saveLocalDatasetState(localDatasetState: LocalDatasetState): Result<DomainError, LocalDatasetState>

    suspend fun deleteLocalDatasetState(): Result<DomainError, Long>
}

internal interface SyncStateDataSource {
    fun observeSyncState(): Flow<Result<DomainError, SyncState?>>

    suspend fun getSyncState(): Result<DomainError, SyncState?>

    suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState>

    suspend fun deleteSyncState(): Result<DomainError, Long>
}

internal interface SyncOperationDataSource {
    fun observePendingSyncOperations(): Flow<Result<DomainError, List<SyncOperation>>>

    suspend fun getPendingSyncOperations(): Result<DomainError, List<SyncOperation>>

    suspend fun saveSyncOperation(syncOperation: SyncOperation): Result<DomainError, SyncOperation>

    suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Long>

    suspend fun clearSyncOperations(): Result<DomainError, Long>
}

internal interface PhotoSyncScopeDataSource {
    fun observePhotoSyncScope(): Flow<Result<DomainError, List<PhotoSyncScope>>>

    suspend fun getPendingPhotoSyncScope(): Result<DomainError, List<PhotoSyncScope>>

    suspend fun savePhotoSyncScope(photoSyncScope: PhotoSyncScope): Result<DomainError, PhotoSyncScope>

    suspend fun deletePhotoSyncScope(photoId: String): Result<DomainError, Long>

    suspend fun clearPhotoSyncScope(): Result<DomainError, Long>
}
