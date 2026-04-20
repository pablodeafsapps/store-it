package org.deafsapps.storeit.data.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.ItemDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.PhotoSyncScopeDataSource
import org.deafsapps.storeit.data.datasource.RackDataSource
import org.deafsapps.storeit.data.datasource.SlotDataSource
import org.deafsapps.storeit.data.datasource.SyncStateDataSource
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.PhotoSyncStatus
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus

class FeatureAccountRestoreGatewaysTest {

    @Test
    fun `GIVEN restored racks WHEN replaceRestoredRacks THEN clears existing rack slice and returns written count`() =
        runTest {
            val rackDataSource = FakeRackDataSource()
            val sut = RackFeatureRestoreGateway(rackDataSource = rackDataSource)
            val racks = listOf(rack())

            val result = sut.replaceRestoredRacks(racks = racks)

            assertTrue(actual = result.isOk)
            assertEquals(expected = 1L, actual = result.getOrNullForTest())
            assertTrue(actual = rackDataSource.clearCalled)
            assertEquals(expected = racks, actual = rackDataSource.savedRacks)
        }

    @Test
    fun `GIVEN restored slots WHEN replaceRestoredSlots THEN clears existing slot slice and returns written count`() =
        runTest {
            val slotDataSource = FakeSlotDataSource()
            val sut = SlotFeatureRestoreGateway(slotDataSource = slotDataSource)
            val slots = listOf(shelfSlot())

            val result = sut.replaceRestoredSlots(slots = slots)

            assertTrue(actual = result.isOk)
            assertEquals(expected = 1L, actual = result.getOrNullForTest())
            assertTrue(actual = slotDataSource.clearCalled)
            assertEquals(expected = slots, actual = slotDataSource.savedSlots)
        }

    @Test
    fun `GIVEN restored items WHEN replaceRestoredItems THEN clears existing item slice and returns written count`() =
        runTest {
            val itemDataSource = FakeItemDataSource()
            val sut = ItemFeatureRestoreGateway(itemDataSource = itemDataSource)
            val items = listOf(item())

            val result = sut.replaceRestoredItems(items = items)

            assertTrue(actual = result.isOk)
            assertEquals(expected = 1L, actual = result.getOrNullForTest())
            assertTrue(actual = itemDataSource.clearCalled)
            assertEquals(expected = items, actual = itemDataSource.savedItems)
        }

    @Test
    fun `GIVEN restored photo scope WHEN replaceRestoredPhotoSyncScope THEN clears existing photo slice and returns written count`() =
        runTest {
            val photoSyncScopeDataSource = FakePhotoSyncScopeDataSource()
            val sut = PhotoSyncFeatureRestoreGateway(photoSyncScopeDataSource = photoSyncScopeDataSource)
            val photoSyncScope = listOf(photoSyncScope())

            val result = sut.replaceRestoredPhotoSyncScope(photoSyncScope = photoSyncScope)

            assertTrue(actual = result.isOk)
            assertEquals(expected = 1L, actual = result.getOrNullForTest())
            assertTrue(actual = photoSyncScopeDataSource.clearCalled)
            assertEquals(expected = photoSyncScope, actual = photoSyncScopeDataSource.savedPhotoSyncScope)
        }

    @Test
    fun `GIVEN item save failure WHEN replaceRestoredItems THEN returns the datasource error`() =
        runTest {
            val expectedError = DomainError.Unknown(message = "Unable to save item")
            val itemDataSource = FakeItemDataSource(saveItemResult = expectedError.err())
            val sut = ItemFeatureRestoreGateway(itemDataSource = itemDataSource)

            val result = sut.replaceRestoredItems(items = listOf(item()))

            assertEquals(expected = expectedError, actual = result.failureOrNullForTest())
        }

    @Test
    fun `GIVEN synchronized restore metadata WHEN markRestoreSynchronized THEN saves account dataset local state and sync state`() =
        runTest {
            val accountDatasetDataSource = FakeAccountDatasetDataSource()
            val localDatasetStateDataSource = FakeLocalDatasetStateDataSource()
            val syncStateDataSource = FakeSyncStateDataSource()
            val sut = AccountSyncFeatureRestoreMetadataGateway(
                accountDatasetDataSource = accountDatasetDataSource,
                localDatasetStateDataSource = localDatasetStateDataSource,
                syncStateDataSource = syncStateDataSource,
            )
            val accountDataset = AccountDataset(
                accountId = "account-1",
                datasetVersion = "checkpoint-1",
                lastSyncedAt = 100L,
            )
            val localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedSynchronized,
                accountId = "account-1",
            )
            val syncState = SyncState(status = SyncStatus.Synchronized)

            val result = sut.markRestoreSynchronized(
                accountDataset = accountDataset,
                localDatasetState = localDatasetState,
                syncState = syncState,
            )

            assertTrue(actual = result.isOk)
            assertEquals(expected = accountDataset, actual = accountDatasetDataSource.savedAccountDataset)
            assertEquals(expected = localDatasetState, actual = localDatasetStateDataSource.savedLocalDatasetState)
            assertEquals(expected = syncState, actual = syncStateDataSource.savedSyncState)
        }
}

private fun rack(): Rack = Rack(
    id = "rack-1",
    name = "Rack",
)

private fun shelfSlot(): ShelfSlot = ShelfSlot(
    id = "slot-1",
    rackId = "rack-1",
    position = SlotPosition(
        x = 1f,
        y = 1f,
        xRel = 0.1f,
        yRel = 0.1f,
    ),
)

private fun item(): Item = Item(
    id = "item-1",
    rackId = "rack-1",
    slotId = "slot-1",
    name = "Item",
)

private fun photoSyncScope(): PhotoSyncScope = PhotoSyncScope(
    photoId = "photo-1",
    ownerType = SyncEntityType.Photo,
    ownerId = "photo-1",
    localUri = "remote://photo-1.jpg",
    remoteUrl = "remote://photo-1.jpg",
    checksum = "checksum-1",
    syncStatus = PhotoSyncStatus.Synced,
    lastSyncedAt = 100L,
)

private class FakeRackDataSource(
    private val saveRackResult: Result<DomainError, Rack>? = null,
) : RackDataSource {
    val savedRacks = mutableListOf<Rack>()
    var clearCalled: Boolean = false

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> =
        flowOf(value = savedRacks.toList().ok())

    override suspend fun getRackById(id: String): Result<DomainError, Rack?> = null.ok()

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> =
        saveRackResult ?: rack.also { savedRacks += it }.ok()

    override suspend fun deleteRack(id: String): Result<DomainError, Boolean> = false.ok()

    override suspend fun clear() {
        clearCalled = true
        savedRacks.clear()
    }
}

private class FakeSlotDataSource(
    private val saveSlotResult: Result<DomainError, ShelfSlot>? = null,
) : SlotDataSource {
    val savedSlots = mutableListOf<ShelfSlot>()
    var clearCalled: Boolean = false

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> =
        savedSlots.filter { slot -> slot.rackId == rackId }.ok()

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> =
        saveSlotResult ?: slot.also { savedSlots += it }.ok()

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clear() {
        clearCalled = true
        savedSlots.clear()
    }
}

private class FakeItemDataSource(
    private val saveItemResult: Result<DomainError, Item>? = null,
) : ItemDataSource {
    val savedItems = mutableListOf<Item>()
    var clearCalled: Boolean = false

    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> =
        savedItems.filter { item -> item.rackId == rackId }.ok()

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> =
        savedItems.filter { item -> item.rackId == rackId && item.slotId == slotId }.ok()

    override suspend fun getItemById(id: String): Result<DomainError, Item?> = null.ok()

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> =
        savedItems.toList().ok()

    override suspend fun saveItem(item: Item): Result<DomainError, Item> =
        saveItemResult ?: item.also { savedItems += it }.ok()

    override suspend fun deleteItem(id: String): Result<DomainError, Boolean> = false.ok()

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Long> = 0L.ok()

    override suspend fun clear() {
        clearCalled = true
        savedItems.clear()
    }
}

private class FakePhotoSyncScopeDataSource(
    private val savePhotoSyncScopeResult: Result<DomainError, PhotoSyncScope>? = null,
) : PhotoSyncScopeDataSource {
    val savedPhotoSyncScope = mutableListOf<PhotoSyncScope>()
    var clearCalled: Boolean = false

    override fun observePhotoSyncScope(): Flow<Result<DomainError, List<PhotoSyncScope>>> =
        flowOf(value = savedPhotoSyncScope.toList().ok())

    override suspend fun getPendingPhotoSyncScope(): Result<DomainError, List<PhotoSyncScope>> =
        savedPhotoSyncScope.toList().ok()

    override suspend fun savePhotoSyncScope(photoSyncScope: PhotoSyncScope): Result<DomainError, PhotoSyncScope> =
        savePhotoSyncScopeResult ?: photoSyncScope.also { savedPhotoSyncScope += it }.ok()

    override suspend fun deletePhotoSyncScope(photoId: String): Result<DomainError, Long> = 0L.ok()

    override suspend fun clearPhotoSyncScope(): Result<DomainError, Long> {
        clearCalled = true
        savedPhotoSyncScope.clear()
        return 0L.ok()
    }
}

private class FakeAccountDatasetDataSource : AccountDatasetDataSource {
    var savedAccountDataset: AccountDataset? = null

    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> = null.ok()

    override suspend fun saveAccountDataset(accountDataset: AccountDataset): Result<DomainError, AccountDataset> {
        savedAccountDataset = accountDataset
        return accountDataset.ok()
    }

    override suspend fun deleteAccountDataset(accountId: String): Result<DomainError, Long> = 0L.ok()
}

private class FakeLocalDatasetStateDataSource : LocalDatasetStateDataSource {
    var savedLocalDatasetState: LocalDatasetState? = null

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(value = savedLocalDatasetState.ok())

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> =
        savedLocalDatasetState.ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> {
        savedLocalDatasetState = localDatasetState
        return localDatasetState.ok()
    }

    override suspend fun deleteLocalDatasetState(): Result<DomainError, Long> = 0L.ok()
}

private class FakeSyncStateDataSource : SyncStateDataSource {
    var savedSyncState: SyncState? = null

    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> =
        flowOf(value = savedSyncState.ok())

    override suspend fun getSyncState(): Result<DomainError, SyncState?> =
        savedSyncState.ok()

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> {
        savedSyncState = syncState
        return syncState.ok()
    }

    override suspend fun deleteSyncState(): Result<DomainError, Long> = 0L.ok()
}

private fun <E, V> Result<E, V>.getOrNullForTest(): V? =
    getOrNull()

private fun <E, V> Result<E, V>.failureOrNullForTest(): E? =
    failureOrNull()
