package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.SyncOperationDataSource
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationStatus
import org.deafsapps.storeit.domain.model.SyncOperationType

class SyncOperationRepositoryTest {
    private lateinit var fakeSyncOperationDataSource: FakeSyncOperationDataSource
    private lateinit var sut: SyncOperationRepository

    @BeforeTest
    fun setUp() {
        fakeSyncOperationDataSource = FakeSyncOperationDataSource()
        sut = DefaultSyncOperationRepository(
            syncOperationDataSource = fakeSyncOperationDataSource,
            operationIdGenerator = SyncOperationIdGenerator { "operation-1" },
        )
    }

    @Test
    fun `GIVEN rack create write WHEN enqueue create THEN persists pending create operation`() = runTest {
        val result = sut.enqueueCreate(
            accountId = "account-1",
            entityType = SyncEntityType.Rack,
            entityId = "rack-1",
            payloadJson = """{"name":"Rack 1"}""",
        )

        assertTrue(actual = result.isOk)
        assertEquals(expected = 1, actual = fakeSyncOperationDataSource.savedOperations.size)
        assertEquals(expected = SyncOperationType.Create, actual = result.getOrNull()?.operationType)
        assertEquals(expected = SyncOperationStatus.Pending, actual = result.getOrNull()?.syncStatus)
    }

    @Test
    fun `GIVEN slot update write WHEN enqueue update THEN persists pending update operation`() = runTest {
        val result = sut.enqueueUpdate(
            accountId = "account-1",
            entityType = SyncEntityType.ShelfSlot,
            entityId = "slot-1",
            payloadJson = """{"name":"Slot 1"}""",
        )

        assertTrue(actual = result.isOk)
        assertEquals(expected = 1, actual = fakeSyncOperationDataSource.savedOperations.size)
        assertEquals(expected = SyncOperationType.Update, actual = result.getOrNull()?.operationType)
        assertEquals(expected = SyncEntityType.ShelfSlot, actual = result.getOrNull()?.entityType)
    }

    @Test
    fun `GIVEN item delete write WHEN enqueue delete THEN persists pending delete operation`() = runTest {
        val result = sut.enqueueDelete(
            accountId = "account-1",
            entityType = SyncEntityType.Item,
            entityId = "item-1",
            payloadJson = null,
        )

        assertTrue(actual = result.isOk)
        assertEquals(expected = 1, actual = fakeSyncOperationDataSource.savedOperations.size)
        assertEquals(expected = SyncOperationType.Delete, actual = result.getOrNull()?.operationType)
        assertEquals(expected = "operation-1", actual = result.getOrNull()?.id)
    }

    @Test
    fun `GIVEN blank entity id WHEN enqueue operation THEN returns validation error`() = runTest {
        val result = sut.enqueueCreate(
            accountId = "account-1",
            entityType = SyncEntityType.Rack,
            entityId = "",
            payloadJson = null,
        )

        assertTrue(actual = result.isErr)
        assertEquals(expected = 0, actual = fakeSyncOperationDataSource.savedOperations.size)
        assertTrue(actual = result.failureOrNull() is DomainError.ValidationError)
    }
}

private class FakeSyncOperationDataSource : SyncOperationDataSource {
    val savedOperations: MutableList<SyncOperation> = mutableListOf()

    override fun observePendingSyncOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
        flowOf(value = savedOperations.toList().ok())

    override suspend fun getPendingSyncOperations(): Result<DomainError, List<SyncOperation>> =
        savedOperations.toList().ok()

    override suspend fun saveSyncOperation(
        syncOperation: SyncOperation,
    ): Result<DomainError, SyncOperation> = syncOperation.ok().also {
        savedOperations += syncOperation
    }

    override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Long> = 0L.ok()

    override suspend fun clearSyncOperations(): Result<DomainError, Long> = 0L.ok()
}
