package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.ItemDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.RackDataSource
import org.deafsapps.storeit.data.datasource.SlotDataSource
import org.deafsapps.storeit.data.datasource.SyncOperationDataSource
import org.deafsapps.storeit.data.datasource.SyncStateDataSource
import org.deafsapps.storeit.data.datasource.SyncTelemetryDataSource
import org.deafsapps.storeit.data.datasource.SyncTelemetryEvent
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationStatus
import org.deafsapps.storeit.domain.model.SyncOperationType
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.deafsapps.storeit.domain.repository.SyncRepository

class SyncAwareOrganizerRepositoriesTest {
    private lateinit var rackDataSource: PersistentRackDataSource
    private lateinit var slotDataSource: PersistentSlotDataSource
    private lateinit var itemDataSource: PersistentItemDataSource
    private lateinit var syncOperationDataSource: PersistentSyncOperationDataSource
    private lateinit var localDatasetStateDataSource: SyncAwareLocalDatasetStateDataSource
    private lateinit var operationIdGenerator: IncrementingSyncOperationIdGenerator

    @BeforeTest
    fun setUp() {
        rackDataSource = PersistentRackDataSource()
        slotDataSource = PersistentSlotDataSource()
        itemDataSource = PersistentItemDataSource()
        syncOperationDataSource = PersistentSyncOperationDataSource()
        localDatasetStateDataSource = SyncAwareLocalDatasetStateDataSource()
        operationIdGenerator = IncrementingSyncOperationIdGenerator()
    }

    @Test
    fun `GIVEN offline rack create WHEN app restarts THEN local rack and pending operation are preserved`() = runTest {
        val sut: RackRepository = rackRepository()
        val rack = Rack(id = "rack-1", name = "Rack 1")

        val result = sut.saveRack(rack = rack)

        assertTrue(actual = result.isOk)

        val restartedRackRepository: RackRepository = rackRepository()
        val restartedSyncRepository: SyncRepository = syncRepository()
        val persistedOperations = restartedSyncRepository.observePendingOperations().first().getOrNull().orEmpty()

        assertEquals(expected = rack, actual = restartedRackRepository.getRackById(id = rack.id).getOrNull())
        assertEquals(expected = 1, actual = persistedOperations.size)
        assertPersistedOperation(
            syncOperation = persistedOperations.single(),
            expectedId = "operation-1",
            expectedEntityType = SyncEntityType.Rack,
            expectedEntityId = "rack-1",
            expectedOperationType = SyncOperationType.Create,
        )
    }

    @Test
    fun `GIVEN offline slot update and item delete WHEN app restarts THEN queued mutations survive in recorded order`() = runTest {
        val existingSlot = ShelfSlot(
            id = "slot-1",
            rackId = "rack-1",
            position = SlotPosition(x = 0f, y = 0f, xRel = 0f, yRel = 0f),
        )
        val existingItem = Item(
            id = "item-1",
            name = "Item 1",
            rackId = "rack-1",
            slotId = "slot-1",
        )
        slotDataSource.saveSlot(slot = existingSlot)
        itemDataSource.saveItem(item = existingItem)

        val slotRepository = slotRepository()
        val itemRepository = itemRepository()

        val updatedSlot = ShelfSlot(
            id = existingSlot.id,
            rackId = existingSlot.rackId,
            position = SlotPosition(x = 10f, y = existingSlot.position.y, xRel = existingSlot.position.xRel, yRel = existingSlot.position.yRel),
        )

        val saveSlotResult = slotRepository.saveSlot(slot = updatedSlot)
        val deleteItemResult = itemRepository.deleteItem(id = existingItem.id)

        assertTrue(actual = saveSlotResult.isOk)
        assertTrue(actual = deleteItemResult.isOk)

        val restartedSlotRepository: SlotRepository = slotRepository()
        val restartedItemRepository: ItemRepository = itemRepository()
        val restartedSyncRepository: SyncRepository = syncRepository()
        val persistedOperations = restartedSyncRepository.observePendingOperations().first().getOrNull().orEmpty()

        assertEquals(
            expected = 10f,
            actual = restartedSlotRepository.getSlotsByRack(rackId = "rack-1").getOrNull()?.single()?.position?.x,
        )
        assertTrue(actual = restartedItemRepository.getItemById(id = existingItem.id).isErr)
        assertEquals(expected = 2, actual = persistedOperations.size)
        assertPersistedOperation(
            syncOperation = persistedOperations[0],
            expectedId = "operation-1",
            expectedEntityType = SyncEntityType.ShelfSlot,
            expectedEntityId = "slot-1",
            expectedOperationType = SyncOperationType.Update,
        )
        assertPersistedOperation(
            syncOperation = persistedOperations[1],
            expectedId = "operation-2",
            expectedEntityType = SyncEntityType.Item,
            expectedEntityId = "item-1",
            expectedOperationType = SyncOperationType.Delete,
        )
    }

    @Test
    fun `GIVEN persisted offline mutations WHEN later sync consumes them THEN queue can be drained after restart`() = runTest {
        val rackRepository = rackRepository()
        val slotRepository = slotRepository()
        val rack = Rack(id = "rack-1", name = "Rack 1")
        val slot = ShelfSlot(
            id = "slot-1",
            rackId = rack.id,
            position = SlotPosition(x = 1f, y = 2f, xRel = 0.1f, yRel = 0.2f),
        )

        rackRepository.saveRack(rack = rack)
        slotRepository.saveSlot(slot = slot)

        val sut: SyncRepository = syncRepository()
        val persistedOperations = sut.observePendingOperations().first().getOrNull().orEmpty()

        val deleteFirstResult = sut.deleteSyncOperation(operationId = persistedOperations.first().id)
        val clearRemainingResult = sut.clearSyncOperations()

        assertTrue(actual = deleteFirstResult.isOk)
        assertTrue(actual = clearRemainingResult.isOk)
        assertEquals(expected = emptyList(), actual = sut.observePendingOperations().first().getOrNull())
    }

    private fun rackRepository(
    ): RackRepository = SqlDelightRackRepository(
        rackDataSource = rackDataSource,
        syncOperationRepository = syncOperationRepository(),
        localDatasetStateDataSource = localDatasetStateDataSource,
    )

    private fun slotRepository(
    ): SlotRepository = SqlDelightSlotRepository(
        slotDataSource = slotDataSource,
        syncOperationRepository = syncOperationRepository(),
        localDatasetStateDataSource = localDatasetStateDataSource,
    )

    private fun itemRepository(
    ): ItemRepository = SqlDelightItemRepository(
        itemDataSource = itemDataSource,
        syncOperationRepository = syncOperationRepository(),
        localDatasetStateDataSource = localDatasetStateDataSource,
    )

    private fun syncOperationRepository(): SyncOperationRepository = DefaultSyncOperationRepository(
        syncOperationDataSource = syncOperationDataSource,
        operationIdGenerator = operationIdGenerator,
    )

    private fun syncRepository(): SyncRepository = DefaultSyncRepository(
        accountDatasetDataSource = SyncAwareFakeAccountDatasetDataSource(),
        localDatasetStateDataSource = localDatasetStateDataSource,
        syncStateDataSource = SyncAwareFakeSyncStateDataSource(),
        syncOperationDataSource = syncOperationDataSource,
        syncTelemetryDataSource = SyncAwareFakeSyncTelemetryDataSource(),
    )
}

private class IncrementingSyncOperationIdGenerator : SyncOperationIdGenerator {
    private var nextId: Int = 1

    override fun generate(): String = "operation-${nextId++}"
}

private class SyncAwareFakeSyncTelemetryDataSource : SyncTelemetryDataSource {
    override fun onEvent(event: SyncTelemetryEvent) = Unit
}

private class SyncAwareFakeAccountDatasetDataSource : AccountDatasetDataSource {
    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> = null.ok()

    override suspend fun saveAccountDataset(
        accountDataset: AccountDataset,
    ): Result<DomainError, AccountDataset> = accountDataset.ok()

    override suspend fun deleteAccountDataset(accountId: String): Result<DomainError, Long> = 0L.ok()
}

private class SyncAwareFakeSyncStateDataSource : SyncStateDataSource {
    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> = flowOf(null.ok())

    override suspend fun getSyncState(): Result<DomainError, SyncState?> = null.ok()

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> = syncState.ok()

    override suspend fun deleteSyncState(): Result<DomainError, Long> = 0L.ok()
}

private class PersistentSyncOperationDataSource : SyncOperationDataSource {
    private val pendingOperations: MutableList<SyncOperation> = mutableListOf()

    override fun observePendingSyncOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
        flowOf(value = pendingOperations.toList().ok())

    override suspend fun getPendingSyncOperations(): Result<DomainError, List<SyncOperation>> =
        pendingOperations.toList().ok()

    override suspend fun saveSyncOperation(
        syncOperation: SyncOperation,
    ): Result<DomainError, SyncOperation> = syncOperation.ok().also {
        pendingOperations.removeAll { existing -> existing.id == syncOperation.id }
        pendingOperations += syncOperation
    }

    override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Long> =
        pendingOperations.removeAll { operation -> operation.id == operationId }
            .let { removed -> if (removed) 1L else 0L }
            .ok()

    override suspend fun clearSyncOperations(): Result<DomainError, Long> =
        pendingOperations.size.toLong().ok().also {
            pendingOperations.clear()
        }
}

private class SyncAwareLocalDatasetStateDataSource : LocalDatasetStateDataSource {
    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(localDatasetState().ok())

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> =
        localDatasetState().ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> = localDatasetState.ok()

    override suspend fun deleteLocalDatasetState(): Result<DomainError, Long> = 0L.ok()

    private fun localDatasetState(): LocalDatasetState = LocalDatasetState(
        mode = DataMode.AccountBackedPendingSync,
        accountId = "account-1",
        hasPendingChanges = true,
    )
}

private class PersistentRackDataSource : RackDataSource {
    private val racksById: MutableMap<String, Rack> = linkedMapOf()

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> =
        flowOf(racksById.values.toList().ok())

    override suspend fun getRackById(id: String): Result<DomainError, Rack?> = racksById[id].ok()

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> = rack.ok().also {
        racksById[rack.id] = rack
    }

    override suspend fun deleteRack(id: String): Result<DomainError, Boolean> =
        (racksById.remove(key = id) != null).ok()

    override suspend fun clear() {
        racksById.clear()
    }
}

private class PersistentSlotDataSource : SlotDataSource {
    private val slotsById: MutableMap<String, ShelfSlot> = linkedMapOf()

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> =
        slotsById.values.filter { slot -> slot.rackId == rackId }.ok()

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> = slot.ok().also {
        slotsById[slot.id] = slot
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Long> =
        slotsById.values
            .filter { slot -> slot.rackId == rackId }
            .map { slot -> slot.id }
            .also { ids -> ids.forEach { id -> slotsById.remove(key = id) } }
            .size
            .toLong()
            .ok()

    override suspend fun clear() {
        slotsById.clear()
    }
}

private class PersistentItemDataSource : ItemDataSource {
    private val itemsById: MutableMap<String, Item> = linkedMapOf()

    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> =
        itemsById.values.filter { item -> item.rackId == rackId }.ok()

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> =
        itemsById.values.filter { item -> item.rackId == rackId && item.slotId == slotId }.ok()

    override suspend fun getItemById(id: String): Result<DomainError, Item?> = itemsById[id].ok()

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> =
        itemsById.values.filter { item -> item.name.contains(other = query, ignoreCase = true) }.ok()

    override suspend fun saveItem(item: Item): Result<DomainError, Item> = item.ok().also {
        itemsById[item.id] = item
    }

    override suspend fun deleteItem(id: String): Result<DomainError, Boolean> =
        (itemsById.remove(key = id) != null).ok()

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Long> =
        itemsById.values
            .filter { item -> item.rackId == rackId }
            .map { item -> item.id }
            .also { ids -> ids.forEach { id -> itemsById.remove(key = id) } }
            .size
            .toLong()
            .ok()

    override suspend fun clear() {
        itemsById.clear()
    }
}

private fun assertPersistedOperation(
    syncOperation: SyncOperation,
    expectedId: String,
    expectedEntityType: SyncEntityType,
    expectedEntityId: String,
    expectedOperationType: SyncOperationType,
) {
    assertEquals(expected = expectedId, actual = syncOperation.id)
    assertEquals(expected = "account-1", actual = syncOperation.accountId)
    assertEquals(expected = expectedEntityType, actual = syncOperation.entityType)
    assertEquals(expected = expectedEntityId, actual = syncOperation.entityId)
    assertEquals(expected = expectedOperationType, actual = syncOperation.operationType)
    assertEquals(expected = SyncOperationStatus.Pending, actual = syncOperation.syncStatus)
}
