package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.SyncOperationDataSource
import org.deafsapps.storeit.data.datasource.SyncStateDataSource
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.repository.SyncRepository
import org.koin.core.annotation.Single

@Single(binds = [SyncRepository::class])
internal class DefaultSyncRepository(
    private val accountDatasetDataSource: AccountDatasetDataSource,
    private val localDatasetStateDataSource: LocalDatasetStateDataSource,
    private val syncStateDataSource: SyncStateDataSource,
    private val syncOperationDataSource: SyncOperationDataSource,
) : SyncRepository {
    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        localDatasetStateDataSource.observeLocalDatasetState()

    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> =
        syncStateDataSource.observeSyncState()

    override fun observePendingOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
        syncOperationDataSource.observePendingSyncOperations()

    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> =
        accountDatasetDataSource.getAccountDataset(accountId = accountId)

    override suspend fun saveAccountDataset(
        accountDataset: AccountDataset,
    ): Result<DomainError, AccountDataset> =
        accountDatasetDataSource.saveAccountDataset(accountDataset = accountDataset)

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> =
        localDatasetStateDataSource.saveLocalDatasetState(localDatasetState = localDatasetState)

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> =
        syncStateDataSource.saveSyncState(syncState = syncState)

    override suspend fun saveSyncOperation(
        syncOperation: SyncOperation,
    ): Result<DomainError, SyncOperation> =
        syncOperationDataSource.saveSyncOperation(syncOperation = syncOperation)

    override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Unit> =
        syncOperationDataSource.deleteSyncOperation(operationId = operationId).map { Unit }

    override suspend fun clearSyncOperations(): Result<DomainError, Unit> =
        syncOperationDataSource.clearSyncOperations().map { Unit }
}
