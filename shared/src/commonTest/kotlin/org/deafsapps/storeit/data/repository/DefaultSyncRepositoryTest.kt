package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.SyncOperationDataSource
import org.deafsapps.storeit.data.datasource.SyncStateDataSource
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationStatus
import org.deafsapps.storeit.domain.model.SyncOperationType
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.repository.SyncRepository

class DefaultSyncRepositoryTest {
    private lateinit var fakeAccountDatasetDataSource: FakeAccountDatasetDataSource
    private lateinit var fakeLocalDatasetStateDataSource: FakeLocalDatasetStateDataSource
    private lateinit var fakeSyncStateDataSource: FakeSyncStateDataSource
    private lateinit var fakeSyncOperationMetadataDataSource: FakeSyncOperationMetadataDataSource
    private lateinit var sut: SyncRepository

    @BeforeTest
    fun setUp() {
        fakeAccountDatasetDataSource = FakeAccountDatasetDataSource()
        fakeLocalDatasetStateDataSource = FakeLocalDatasetStateDataSource()
        fakeSyncStateDataSource = FakeSyncStateDataSource()
        fakeSyncOperationMetadataDataSource = FakeSyncOperationMetadataDataSource()
        sut = DefaultSyncRepository(
            accountDatasetDataSource = fakeAccountDatasetDataSource,
            localDatasetStateDataSource = fakeLocalDatasetStateDataSource,
            syncStateDataSource = fakeSyncStateDataSource,
            syncOperationDataSource = fakeSyncOperationMetadataDataSource,
        )
    }

    @Test
    fun `GIVEN pending upload local state WHEN save local dataset state THEN persists pending changes`() = runTest {
        val localState = LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
        )

        val result = sut.saveLocalDatasetState(localDatasetState = localState)

        assertTrue(actual = result.isOk)
        assertEquals(expected = localState, actual = fakeLocalDatasetStateDataSource.savedState)
    }

    @Test
    fun `GIVEN failed sync state WHEN save sync state THEN persists failure and retry metadata`() = runTest {
        val failedState = SyncState(
            status = SyncStatus.Failed,
            failureReason = "Upload failed",
            lastAttemptAt = 123L,
            pendingOperationCount = 2,
        )

        val result = sut.saveSyncState(syncState = failedState)

        assertTrue(actual = result.isOk)
        assertEquals(expected = failedState, actual = fakeSyncStateDataSource.savedSyncState)
    }

    @Test
    fun `GIVEN retry pending upload WHEN save sync state THEN persists pending upload state`() = runTest {
        val pendingState = SyncState(
            status = SyncStatus.PendingUpload,
            pendingOperationCount = 1,
        )

        val result = sut.saveSyncState(syncState = pendingState)

        assertTrue(actual = result.isOk)
        assertEquals(expected = SyncStatus.PendingUpload, actual = fakeSyncStateDataSource.savedSyncState?.status)
        assertEquals(expected = 1, actual = fakeSyncStateDataSource.savedSyncState?.pendingOperationCount)
    }

    @Test
    fun `GIVEN queued sync operation WHEN save sync operation THEN operation is persisted`() = runTest {
        val operation = syncOperation(id = "operation-1")

        val result = sut.saveSyncOperation(syncOperation = operation)

        assertTrue(actual = result.isOk)
        assertEquals(expected = operation, actual = fakeSyncOperationMetadataDataSource.savedOperation)
    }

    @Test
    fun `GIVEN existing operation id WHEN delete sync operation THEN no error is returned`() = runTest {
        val result = sut.deleteSyncOperation(operationId = "operation-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = "operation-1", actual = fakeSyncOperationMetadataDataSource.deletedOperationId)
    }

    @Test
    fun `GIVEN persisted operations WHEN clear sync operations THEN no error is returned`() = runTest {
        val result = sut.clearSyncOperations()

        assertTrue(actual = result.isOk)
        assertEquals(expected = true, actual = fakeSyncOperationMetadataDataSource.clearInvoked)
    }
}

private class FakeAccountDatasetDataSource : AccountDatasetDataSource {
    var savedDataset: AccountDataset? = null

    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> = null.ok()

    override suspend fun saveAccountDataset(
        accountDataset: AccountDataset,
    ): Result<DomainError, AccountDataset> = accountDataset.ok().also {
        savedDataset = accountDataset
    }

    override suspend fun deleteAccountDataset(accountId: String): Result<DomainError, Long> = 0L.ok()
}

private class FakeLocalDatasetStateDataSource : LocalDatasetStateDataSource {
    var savedState: LocalDatasetState? = null

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> = flowOf(null.ok())

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> = null.ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> = localDatasetState.ok().also {
        savedState = localDatasetState
    }

    override suspend fun deleteLocalDatasetState(): Result<DomainError, Long> = 0L.ok()
}

private class FakeSyncStateDataSource : SyncStateDataSource {
    var savedSyncState: SyncState? = null

    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> = flowOf(null.ok())

    override suspend fun getSyncState(): Result<DomainError, SyncState?> = null.ok()

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> =
        syncState.ok().also {
            savedSyncState = syncState
        }

    override suspend fun deleteSyncState(): Result<DomainError, Long> = 0L.ok()
}

private class FakeSyncOperationMetadataDataSource : SyncOperationDataSource {
    var savedOperation: SyncOperation? = null
    var deletedOperationId: String? = null
    var clearInvoked: Boolean = false

    override fun observePendingSyncOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
        flowOf(emptyList<SyncOperation>().ok())

    override suspend fun getPendingSyncOperations(): Result<DomainError, List<SyncOperation>> =
        emptyList<SyncOperation>().ok()

    override suspend fun saveSyncOperation(
        syncOperation: SyncOperation,
    ): Result<DomainError, SyncOperation> = syncOperation.ok().also {
        savedOperation = syncOperation
    }

    override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Long> = 1L.ok().also {
        deletedOperationId = operationId
    }

    override suspend fun clearSyncOperations(): Result<DomainError, Long> = 1L.ok().also {
        clearInvoked = true
    }
}

private fun syncOperation(id: String): SyncOperation = SyncOperation(
    id = id,
    accountId = "account-1",
    entityType = SyncEntityType.Item,
    entityId = "item-1",
    operationType = SyncOperationType.Update,
    syncStatus = SyncOperationStatus.Pending,
)
