package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.repository.SyncRepository

 internal class UploadPendingAccountDataUseCaseTest {

    private lateinit var sut: UploadPendingAccountDataUseCaseType
    private lateinit var fakeSyncRepository: FakeUploadSyncRepository

    @BeforeTest
    fun setUp() {
        fakeSyncRepository = FakeUploadSyncRepository()
        sut = UploadPendingAccountDataUseCase(syncRepository = fakeSyncRepository)
    }

    @Test
    fun `GIVEN pending operations flow fails WHEN uploading pending account data THEN returns error and skips state write`() =
        runTest {
            val expectedError = DomainError.ServiceUnavailable(message = "Pending operations unavailable")
            fakeSyncRepository.pendingOperationsResult = expectedError.err()

            val result = sut(input = "account-1")

            assertTrue(actual = result.isErr)
            assertEquals(expected = expectedError, actual = result.failureOrNull())
            assertEquals(expected = 0, actual = fakeSyncRepository.savedSyncStates.size)
        }

    @Test
    fun `GIVEN pending operations exist WHEN uploading pending account data THEN writes pending-upload sync state`() =
        runTest {
            fakeSyncRepository.pendingOperationsResult = listOf(
                SyncOperation(
                    id = "operation-1",
                    accountId = "account-1",
                    entityType = org.deafsapps.storeit.domain.model.SyncEntityType.Item,
                    entityId = "item-1",
                    operationType = org.deafsapps.storeit.domain.model.SyncOperationType.Update,
                    syncStatus = org.deafsapps.storeit.domain.model.SyncOperationStatus.Pending,
                ),
            ).ok()

            val result = sut(input = "account-1")

            assertTrue(actual = result.isOk)
            assertEquals(expected = 1, actual = fakeSyncRepository.savedSyncStates.size)
            assertEquals(expected = SyncStatus.PendingUpload, actual = fakeSyncRepository.savedSyncStates.first().status)
            assertEquals(expected = 1, actual = fakeSyncRepository.savedSyncStates.first().pendingOperationCount)
        }
}

private class FakeUploadSyncRepository : SyncRepository {
    var pendingOperationsResult: Result<DomainError, List<SyncOperation>> = emptyList<SyncOperation>().ok()
    val savedSyncStates: MutableList<SyncState> = mutableListOf()

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(value = null.ok())

    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> =
        flowOf(value = null.ok())

    override fun observePendingOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
        flowOf(value = pendingOperationsResult)

    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> =
        null.ok()

    override suspend fun saveAccountDataset(
        accountDataset: AccountDataset,
    ): Result<DomainError, AccountDataset> = accountDataset.ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> = localDatasetState.ok()

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> {
        savedSyncStates += syncState
        return syncState.ok()
    }

    override suspend fun saveSyncOperation(syncOperation: SyncOperation): Result<DomainError, SyncOperation> =
        syncOperation.ok()

    override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clearSyncOperations(): Result<DomainError, Unit> = Unit.ok()
}
