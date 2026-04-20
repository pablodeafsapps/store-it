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
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.AccountRemoteDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.PhotoSyncScopeDataSource
import org.deafsapps.storeit.data.datasource.RemoteAccountSnapshot
import org.deafsapps.storeit.data.datasource.RemoteDatasetMutation
import org.deafsapps.storeit.data.datasource.RemotePhotoAsset
import org.deafsapps.storeit.data.datasource.RemotePhotoReference
import org.deafsapps.storeit.data.datasource.RemoteSyncCheckpoint
import org.deafsapps.storeit.data.datasource.SyncStateDataSource
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository

class RestoreAccountDataUseCaseTest {
    private lateinit var sut: RestoreAccountDataUseCase
    private lateinit var fakeAccountRemoteDataSource: FakeAccountRemoteDataSource
    private lateinit var fakeAccountDatasetDataSource: FakeAccountDatasetDataSource
    private lateinit var fakeLocalDatasetStateDataSource: FakeLocalDatasetStateDataSource
    private lateinit var fakeSyncStateDataSource: FakeSyncStateDataSource
    private lateinit var fakePhotoSyncScopeDataSource: FakePhotoSyncScopeDataSource
    private lateinit var fakeRackRepository: FakeRestoreRackRepository
    private lateinit var fakeSlotRepository: FakeRestoreSlotRepository
    private lateinit var fakeItemRepository: FakeRestoreItemRepository

    @BeforeTest
    fun setUp() {
        fakeAccountRemoteDataSource = FakeAccountRemoteDataSource()
        fakeAccountDatasetDataSource = FakeAccountDatasetDataSource()
        fakeLocalDatasetStateDataSource = FakeLocalDatasetStateDataSource()
        fakeSyncStateDataSource = FakeSyncStateDataSource()
        fakePhotoSyncScopeDataSource = FakePhotoSyncScopeDataSource()
        fakeRackRepository = FakeRestoreRackRepository()
        fakeSlotRepository = FakeRestoreSlotRepository()
        fakeItemRepository = FakeRestoreItemRepository()
        sut = RestoreAccountDataUseCase(
            accountRemoteDataSource = fakeAccountRemoteDataSource,
            accountDatasetDataSource = fakeAccountDatasetDataSource,
            localDatasetStateDataSource = fakeLocalDatasetStateDataSource,
            syncStateDataSource = fakeSyncStateDataSource,
            photoSyncScopeDataSource = fakePhotoSyncScopeDataSource,
            rackRepository = fakeRackRepository,
            slotRepository = fakeSlotRepository,
            itemRepository = fakeItemRepository,
        )
    }

    @Test
    fun `GIVEN first account restore snapshot with photos WHEN invoke THEN applies remote dataset locally and records synchronized metadata`() =
        runTest {
            val session = accountSession()
            val snapshot = remoteAccountSnapshot()
            fakeAccountRemoteDataSource.fetchSnapshotResult = snapshot.ok()
            fakeLocalDatasetStateDataSource.getLocalDatasetStateResult = LocalDatasetState(
                mode = DataMode.LocalOnly,
                accountId = null,
                hasPendingChanges = false,
            ).ok()

            val result: Result<DomainError, Unit> = sut.invoke(session)

            assertTrue(actual = result.isOk)
            assertEquals(expected = listOf(snapshot.racks.single()), actual = fakeRackRepository.savedRacks)
            assertEquals(expected = listOf(snapshot.slots.single()), actual = fakeSlotRepository.savedSlots)
            assertEquals(expected = listOf(snapshot.items.single()), actual = fakeItemRepository.savedItems)
            assertEquals(expected = 1, actual = fakePhotoSyncScopeDataSource.savedPhotoSyncScopes.size)
            assertEquals(
                expected = snapshot.syncCheckpoint.value,
                actual = fakeAccountDatasetDataSource.savedAccountDataset?.datasetVersion,
            )
            assertEquals(
                expected = DataMode.AccountBackedSynchronized,
                actual = fakeLocalDatasetStateDataSource.savedLocalDatasetState?.mode,
            )
            assertEquals(expected = SyncStatus.Synchronized, actual = fakeSyncStateDataSource.savedSyncState?.status)
        }

    @Test
    fun `GIVEN stale local account-backed data WHEN invoke succeeds THEN clears local repositories before applying authoritative remote snapshot`() =
        runTest {
            val session = accountSession()
            val snapshot = remoteAccountSnapshot()
            fakeAccountRemoteDataSource.fetchSnapshotResult = snapshot.ok()
            fakeLocalDatasetStateDataSource.getLocalDatasetStateResult = LocalDatasetState(
                mode = DataMode.AccountBackedPendingSync,
                accountId = session.accountId,
                lastRemoteSyncAt = 5L,
                hasPendingChanges = false,
            ).ok()

            val result: Result<DomainError, Unit> = sut.invoke(session)

            assertTrue(actual = result.isOk)
            assertTrue(actual = fakeRackRepository.clearCalled)
            assertTrue(actual = fakeSlotRepository.clearCalled)
            assertTrue(actual = fakeItemRepository.clearCalled)
            assertTrue(actual = fakePhotoSyncScopeDataSource.clearPhotoSyncScopeCalled)
        }
}

private fun accountSession(): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = 20L,
)

private fun remoteAccountSnapshot(): RemoteAccountSnapshot = RemoteAccountSnapshot(
    accountId = "account-1",
    syncCheckpoint = RemoteSyncCheckpoint(
        value = "checkpoint-1",
        updatedAt = 100L,
    ),
    racks = listOf(
        Rack(
            id = "rack-1",
            name = "Remote Rack",
            photoUri = "remote://racks/rack-1.jpg",
        ),
    ),
    slots = listOf(
        ShelfSlot(
            id = "slot-1",
            rackId = "rack-1",
            position = SlotPosition(
                x = 1f,
                y = 2f,
                xRel = 0.1f,
                yRel = 0.2f,
            ),
        ),
    ),
    items = listOf(
        Item(
            id = "item-1",
            rackId = "rack-1",
            slotId = "slot-1",
            name = "Remote Item",
            photoUri = "remote://items/item-1.jpg",
        ),
    ),
    photos = listOf(
        RemotePhotoReference(
            photoId = "photo-1",
            remoteUrl = "remote://photos/photo-1.jpg",
            checksum = "checksum-1",
        ),
    ),
)

private class FakeAccountRemoteDataSource : AccountRemoteDataSource {
    var fetchSnapshotResult: Result<DomainError, RemoteAccountSnapshot> = DomainError.Unknown(
        message = "fetchSnapshotResult not configured",
    ).err()

    override suspend fun fetchSnapshot(accountId: String): Result<DomainError, RemoteAccountSnapshot> =
        fetchSnapshotResult

    override suspend fun applyMutations(
        accountId: String,
        mutations: List<RemoteDatasetMutation>,
    ): Result<DomainError, RemoteSyncCheckpoint> = DomainError.Unknown(
        message = "Not required for this test",
    ).err()

    override suspend fun uploadPhoto(asset: RemotePhotoAsset): Result<DomainError, RemotePhotoReference> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun deletePhoto(photoId: String): Result<DomainError, Long> = 0L.ok()
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
    var getLocalDatasetStateResult: Result<DomainError, LocalDatasetState?> = null.ok()
    var savedLocalDatasetState: LocalDatasetState? = null

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(value = getLocalDatasetStateResult)

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> = getLocalDatasetStateResult

    override suspend fun saveLocalDatasetState(localDatasetState: LocalDatasetState): Result<DomainError, LocalDatasetState> {
        savedLocalDatasetState = localDatasetState
        return localDatasetState.ok()
    }

    override suspend fun deleteLocalDatasetState(): Result<DomainError, Long> = 0L.ok()
}

private class FakeSyncStateDataSource : SyncStateDataSource {
    var savedSyncState: SyncState? = null

    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> = flowOf(value = null.ok())

    override suspend fun getSyncState(): Result<DomainError, SyncState?> = null.ok()

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> {
        savedSyncState = syncState
        return syncState.ok()
    }

    override suspend fun deleteSyncState(): Result<DomainError, Long> = 0L.ok()
}

private class FakePhotoSyncScopeDataSource : PhotoSyncScopeDataSource {
    val savedPhotoSyncScopes = mutableListOf<PhotoSyncScope>()
    var clearPhotoSyncScopeCalled: Boolean = false

    override fun observePhotoSyncScope(): Flow<Result<DomainError, List<PhotoSyncScope>>> =
        flowOf(value = savedPhotoSyncScopes.toList().ok())

    override suspend fun getPendingPhotoSyncScope(): Result<DomainError, List<PhotoSyncScope>> =
        savedPhotoSyncScopes.toList().ok()

    override suspend fun savePhotoSyncScope(photoSyncScope: PhotoSyncScope): Result<DomainError, PhotoSyncScope> {
        savedPhotoSyncScopes += photoSyncScope
        return photoSyncScope.ok()
    }

    override suspend fun deletePhotoSyncScope(photoId: String): Result<DomainError, Long> = 0L.ok()

    override suspend fun clearPhotoSyncScope(): Result<DomainError, Long> {
        clearPhotoSyncScopeCalled = true
        savedPhotoSyncScopes.clear()
        return 0L.ok()
    }
}

private class FakeRestoreRackRepository : RackRepository {
    val savedRacks = mutableListOf<Rack>()
    var clearCalled: Boolean = false

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> = flowOf(value = savedRacks.toList().ok())

    override suspend fun getRackById(id: String): Result<DomainError, Rack> = DomainError.NotFound(
        resource = "Rack",
        id = id,
    ).err()

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> {
        savedRacks += rack
        return rack.ok()
    }

    override suspend fun deleteRack(id: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clear() {
        clearCalled = true
        savedRacks.clear()
    }
}

private class FakeRestoreSlotRepository : SlotRepository {
    val savedSlots = mutableListOf<ShelfSlot>()
    var clearCalled: Boolean = false

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> =
        savedSlots.filter { slot -> slot.rackId == rackId }.ok()

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> {
        savedSlots += slot
        return slot.ok()
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clear() {
        clearCalled = true
        savedSlots.clear()
    }
}

private class FakeRestoreItemRepository : ItemRepository {
    val savedItems = mutableListOf<Item>()
    var clearCalled: Boolean = false

    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> =
        savedItems.filter { item -> item.rackId == rackId }.ok()

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> =
        savedItems.filter { item -> item.rackId == rackId && item.slotId == slotId }.ok()

    override suspend fun getItemById(id: String): Result<DomainError, Item> = DomainError.NotFound(
        resource = "Item",
        id = id,
    ).err()

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> = savedItems.toList().ok()

    override suspend fun saveItem(item: Item): Result<DomainError, Item> {
        savedItems += item
        return item.ok()
    }

    override suspend fun deleteItem(id: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clear() {
        clearCalled = true
        savedItems.clear()
    }
}
