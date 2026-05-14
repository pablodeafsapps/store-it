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
import org.deafsapps.storeit.data.datasource.ItemDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.RackDataSource
import org.deafsapps.storeit.data.datasource.SlotDataSource
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository

class SyncAwareOrganizerRepositoriesTest {
    private lateinit var fakeSyncOperationRepository: FakeSyncOperationRepository
    private lateinit var fakeLocalDatasetStateDataSource: LocalDatasetStateDataSource

    @BeforeTest
    fun setUp() {
        fakeSyncOperationRepository = FakeSyncOperationRepository()
        fakeLocalDatasetStateDataSource = SyncAwareLocalDatasetStateDataSource()
    }

    @Test
    fun `GIVEN rack does not exist WHEN save rack THEN enqueue create operation`() = runTest {
        val rackDataSource = FakeRackDataSource(existingRack = null)
        val sut: RackRepository = SqlDelightRackRepository(
            rackDataSource = rackDataSource,
            syncOperationRepository = fakeSyncOperationRepository,
            localDatasetStateDataSource = fakeLocalDatasetStateDataSource,
        )
        val rack = Rack(id = "rack-1", name = "Rack 1")

        val result = sut.saveRack(rack = rack)

        assertTrue(actual = result.isOk)
        assertEquals(expected = 1, actual = fakeSyncOperationRepository.createCalls.size)
        assertEquals(expected = SyncEntityType.Rack, actual = fakeSyncOperationRepository.createCalls.first().entityType)
        assertEquals(expected = "rack-1", actual = fakeSyncOperationRepository.createCalls.first().entityId)
    }

    @Test
    fun `GIVEN slot already exists WHEN save slot THEN enqueue update operation`() = runTest {
        val existingSlot = ShelfSlot(
            id = "slot-1",
            rackId = "rack-1",
            position = SlotPosition(x = 0f, y = 0f, xRel = 0f, yRel = 0f),
        )
        val slotDataSource = FakeSlotDataSource(existingSlots = listOf(existingSlot))
        val sut: SlotRepository = SqlDelightSlotRepository(
            slotDataSource = slotDataSource,
            syncOperationRepository = fakeSyncOperationRepository,
            localDatasetStateDataSource = fakeLocalDatasetStateDataSource,
        )

        val result = sut.saveSlot(slot = existingSlot)

        assertTrue(actual = result.isOk)
        assertEquals(expected = 1, actual = fakeSyncOperationRepository.updateCalls.size)
        assertEquals(expected = SyncEntityType.ShelfSlot, actual = fakeSyncOperationRepository.updateCalls.first().entityType)
        assertEquals(expected = "slot-1", actual = fakeSyncOperationRepository.updateCalls.first().entityId)
    }

    @Test
    fun `GIVEN item deletion succeeds WHEN delete item THEN enqueue delete operation`() = runTest {
        val itemDataSource = FakeItemDataSource()
        val sut: ItemRepository = SqlDelightItemRepository(
            itemDataSource = itemDataSource,
            syncOperationRepository = fakeSyncOperationRepository,
            localDatasetStateDataSource = fakeLocalDatasetStateDataSource,
        )

        val result = sut.deleteItem(id = "item-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = 1, actual = fakeSyncOperationRepository.deleteCalls.size)
        assertEquals(expected = SyncEntityType.Item, actual = fakeSyncOperationRepository.deleteCalls.first().entityType)
        assertEquals(expected = "item-1", actual = fakeSyncOperationRepository.deleteCalls.first().entityId)
    }
}

private data class SyncCall(
    val entityType: SyncEntityType,
    val entityId: String,
)

private class FakeSyncOperationRepository : SyncOperationRepository {
    val createCalls: MutableList<SyncCall> = mutableListOf()
    val updateCalls: MutableList<SyncCall> = mutableListOf()
    val deleteCalls: MutableList<SyncCall> = mutableListOf()

    override suspend fun enqueueCreate(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String?,
    ): Result<DomainError, SyncOperation> {
        createCalls += SyncCall(entityType = entityType, entityId = entityId)
        return fakeOperation().ok()
    }

    override suspend fun enqueueUpdate(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String?,
    ): Result<DomainError, SyncOperation> {
        updateCalls += SyncCall(entityType = entityType, entityId = entityId)
        return fakeOperation().ok()
    }

    override suspend fun enqueueDelete(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String?,
    ): Result<DomainError, SyncOperation> {
        deleteCalls += SyncCall(entityType = entityType, entityId = entityId)
        return fakeOperation().ok()
    }

    private fun fakeOperation(): SyncOperation = SyncOperation(
        id = "operation-1",
        entityType = SyncEntityType.Item,
        entityId = "item-1",
        operationType = org.deafsapps.storeit.domain.model.SyncOperationType.Update,
        syncStatus = org.deafsapps.storeit.domain.model.SyncOperationStatus.Pending,
    )
}

private class SyncAwareLocalDatasetStateDataSource : LocalDatasetStateDataSource {
    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
        ).ok())

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> =
        LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
        ).ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> = localDatasetState.ok()

    override suspend fun deleteLocalDatasetState(): Result<DomainError, Long> = 0L.ok()
}

private class FakeRackDataSource(
    private val existingRack: Rack?,
) : RackDataSource {
    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> = flowOf(emptyList<Rack>().ok())

    override suspend fun getRackById(id: String): Result<DomainError, Rack?> = existingRack.ok()

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> = rack.ok()

    override suspend fun deleteRack(id: String): Result<DomainError, Boolean> = true.ok()

    override suspend fun clear() = Unit
}

private class FakeSlotDataSource(
    private val existingSlots: List<ShelfSlot>,
) : SlotDataSource {
    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> = existingSlots.ok()

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> = slot.ok()

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Long> = 0L.ok()

    override suspend fun clear() = Unit
}

private class FakeItemDataSource : ItemDataSource {
    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> = emptyList<Item>().ok()

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> =
        emptyList<Item>().ok()

    override suspend fun getItemById(id: String): Result<DomainError, Item?> = null.ok()

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> = emptyList<Item>().ok()

    override suspend fun saveItem(item: Item): Result<DomainError, Item> = item.ok()

    override suspend fun deleteItem(id: String): Result<DomainError, Boolean> = true.ok()

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Long> = 1L.ok()

    override suspend fun clear() = Unit
}
