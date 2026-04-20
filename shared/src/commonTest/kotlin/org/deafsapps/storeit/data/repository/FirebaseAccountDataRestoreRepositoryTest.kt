package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountRemoteDataSource
import org.deafsapps.storeit.data.datasource.RemoteAccountSnapshot
import org.deafsapps.storeit.data.datasource.RemoteDatasetMutation
import org.deafsapps.storeit.data.datasource.RemotePhotoAsset
import org.deafsapps.storeit.data.datasource.RemotePhotoReference
import org.deafsapps.storeit.data.datasource.RemoteSyncCheckpoint
import org.deafsapps.storeit.domain.gateway.AccountRestoreMetadataGateway
import org.deafsapps.storeit.domain.gateway.ItemRestoreGateway
import org.deafsapps.storeit.domain.gateway.PhotoRestoreGateway
import org.deafsapps.storeit.domain.gateway.RackRestoreGateway
import org.deafsapps.storeit.domain.gateway.SlotRestoreGateway
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

class FirebaseAccountDataRestoreRepositoryTest {
    private lateinit var sut: FirebaseAccountDataRestoreRepository
    private lateinit var fakeAccountRemoteDataSource: FakeAccountRemoteDataSource
    private lateinit var fakeAccountRestoreMetadataGateway: FakeAccountRestoreMetadataGateway
    private lateinit var fakeRackRestoreGateway: FakeRackRestoreGateway
    private lateinit var fakeSlotRestoreGateway: FakeSlotRestoreGateway
    private lateinit var fakeItemRestoreGateway: FakeItemRestoreGateway
    private lateinit var fakePhotoRestoreGateway: FakePhotoRestoreGateway

    @BeforeTest
    fun setUp() {
        fakeAccountRemoteDataSource = FakeAccountRemoteDataSource()
        fakeAccountRestoreMetadataGateway = FakeAccountRestoreMetadataGateway()
        fakeRackRestoreGateway = FakeRackRestoreGateway()
        fakeSlotRestoreGateway = FakeSlotRestoreGateway()
        fakeItemRestoreGateway = FakeItemRestoreGateway()
        fakePhotoRestoreGateway = FakePhotoRestoreGateway()
        sut = FirebaseAccountDataRestoreRepository(
            accountRemoteDataSource = fakeAccountRemoteDataSource,
            accountRestoreMetadataGateway = fakeAccountRestoreMetadataGateway,
            rackRestoreGateway = fakeRackRestoreGateway,
            slotRestoreGateway = fakeSlotRestoreGateway,
            itemRestoreGateway = fakeItemRestoreGateway,
            photoRestoreGateway = fakePhotoRestoreGateway,
        )
    }

    @Test
    fun `GIVEN first account restore snapshot with photos WHEN restoreAccountData THEN replaces dataset through gateways and records synchronized metadata`() =
        runTest {
            val session = accountSession()
            val snapshot = remoteAccountSnapshot()
            fakeAccountRemoteDataSource.fetchSnapshotResult = snapshot.ok()
            fakeAccountRestoreMetadataGateway.getLocalDatasetStateResult = LocalDatasetState(
                mode = DataMode.LocalOnly,
                accountId = null,
                hasPendingChanges = false,
            ).ok()

            val result: Result<DomainError, Unit> = sut.restoreAccountData(session = session)

            assertTrue(actual = result.isOk)
            assertEquals(expected = snapshot.racks, actual = fakeRackRestoreGateway.restoredRacks)
            assertEquals(expected = snapshot.slots, actual = fakeSlotRestoreGateway.restoredSlots)
            assertEquals(expected = snapshot.items, actual = fakeItemRestoreGateway.restoredItems)
            assertEquals(expected = 1, actual = fakePhotoRestoreGateway.restoredPhotoSyncScope.size)
            assertEquals(
                expected = snapshot.syncCheckpoint.value,
                actual = fakeAccountRestoreMetadataGateway.synchronizedAccountDataset?.datasetVersion,
            )
            assertEquals(
                expected = DataMode.AccountBackedSynchronized,
                actual = fakeAccountRestoreMetadataGateway.synchronizedLocalDatasetState?.mode,
            )
            assertEquals(
                expected = SyncStatus.Synchronized,
                actual = fakeAccountRestoreMetadataGateway.synchronizedSyncState?.status,
            )
        }

    @Test
    fun `GIVEN stale local account-backed data WHEN restore succeeds THEN delegates authoritative replacement to every restore gateway`() =
        runTest {
            val session = accountSession()
            val snapshot = remoteAccountSnapshot()
            fakeAccountRemoteDataSource.fetchSnapshotResult = snapshot.ok()
            fakeAccountRestoreMetadataGateway.getLocalDatasetStateResult = LocalDatasetState(
                mode = DataMode.AccountBackedPendingSync,
                accountId = session.accountId,
                lastRemoteSyncAt = 5L,
                hasPendingChanges = false,
            ).ok()

            val result: Result<DomainError, Unit> = sut.restoreAccountData(session = session)

            assertTrue(actual = result.isOk)
            assertTrue(actual = fakeRackRestoreGateway.replaceCalled)
            assertTrue(actual = fakeSlotRestoreGateway.replaceCalled)
            assertTrue(actual = fakeItemRestoreGateway.replaceCalled)
            assertTrue(actual = fakePhotoRestoreGateway.replaceCalled)
        }

    @Test
    fun `GIVEN remote snapshot fetch fails WHEN restoreAccountData THEN records restore pending metadata`() =
        runTest {
            val session = accountSession()
            val expectedError = DomainError.Unknown(message = "Remote unavailable")
            fakeAccountRemoteDataSource.fetchSnapshotResult = expectedError.err()
            fakeAccountRestoreMetadataGateway.getLocalDatasetStateResult = LocalDatasetState(
                mode = DataMode.AccountBackedSynchronized,
                accountId = session.accountId,
                lastRemoteSyncAt = 50L,
                hasPendingChanges = false,
            ).ok()

            val result: Result<DomainError, Unit> = sut.restoreAccountData(session = session)

            assertTrue(actual = result.isErr)
            assertEquals(
                expected = DataMode.AccountBackedPendingSync,
                actual = fakeAccountRestoreMetadataGateway.pendingLocalDatasetState?.mode,
            )
            assertEquals(
                expected = SyncStatus.RestorePending,
                actual = fakeAccountRestoreMetadataGateway.pendingSyncState?.status,
            )
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

private class FakeAccountRestoreMetadataGateway : AccountRestoreMetadataGateway {
    var getLocalDatasetStateResult: Result<DomainError, LocalDatasetState?> = null.ok()
    var synchronizedAccountDataset: AccountDataset? = null
    var synchronizedLocalDatasetState: LocalDatasetState? = null
    var synchronizedSyncState: SyncState? = null
    var pendingLocalDatasetState: LocalDatasetState? = null
    var pendingSyncState: SyncState? = null

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> =
        getLocalDatasetStateResult

    override suspend fun markRestoreSynchronized(
        accountDataset: AccountDataset,
        localDatasetState: LocalDatasetState,
        syncState: SyncState,
    ): Result<DomainError, Unit> {
        synchronizedAccountDataset = accountDataset
        synchronizedLocalDatasetState = localDatasetState
        synchronizedSyncState = syncState
        return Unit.ok()
    }

    override suspend fun markRestorePending(
        localDatasetState: LocalDatasetState,
        syncState: SyncState,
    ): Result<DomainError, Unit> {
        pendingLocalDatasetState = localDatasetState
        pendingSyncState = syncState
        return Unit.ok()
    }
}

private class FakeRackRestoreGateway : RackRestoreGateway {
    var replaceCalled: Boolean = false
    var restoredRacks: List<Rack> = emptyList()

    override suspend fun replaceRestoredRacks(racks: List<Rack>): Result<DomainError, Long> {
        replaceCalled = true
        restoredRacks = racks
        return racks.size.toLong().ok()
    }
}

private class FakeSlotRestoreGateway : SlotRestoreGateway {
    var replaceCalled: Boolean = false
    var restoredSlots: List<ShelfSlot> = emptyList()

    override suspend fun replaceRestoredSlots(slots: List<ShelfSlot>): Result<DomainError, Long> {
        replaceCalled = true
        restoredSlots = slots
        return slots.size.toLong().ok()
    }
}

private class FakeItemRestoreGateway : ItemRestoreGateway {
    var replaceCalled: Boolean = false
    var restoredItems: List<Item> = emptyList()

    override suspend fun replaceRestoredItems(items: List<Item>): Result<DomainError, Long> {
        replaceCalled = true
        restoredItems = items
        return items.size.toLong().ok()
    }
}

private class FakePhotoRestoreGateway : PhotoRestoreGateway {
    var replaceCalled: Boolean = false
    var restoredPhotoSyncScope: List<PhotoSyncScope> = emptyList()

    override suspend fun replaceRestoredPhotoSyncScope(
        photoSyncScope: List<PhotoSyncScope>,
    ): Result<DomainError, Long> {
        replaceCalled = true
        restoredPhotoSyncScope = photoSyncScope
        return photoSyncScope.size.toLong().ok()
    }
}
