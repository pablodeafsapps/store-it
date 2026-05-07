package org.deafsapps.storeit.data.gateway

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.base.suspendFlatMap
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.ItemDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.PhotoSyncScopeDataSource
import org.deafsapps.storeit.data.datasource.RackDataSource
import org.deafsapps.storeit.data.datasource.SlotDataSource
import org.deafsapps.storeit.data.datasource.SyncStateDataSource
import org.deafsapps.storeit.domain.gateway.AccountRestoreMetadataGateway
import org.deafsapps.storeit.domain.gateway.ItemRestoreGateway
import org.deafsapps.storeit.domain.gateway.PhotoRestoreGateway
import org.deafsapps.storeit.domain.gateway.RackRestoreGateway
import org.deafsapps.storeit.domain.gateway.SlotRestoreGateway
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SyncState
import org.koin.core.annotation.Single

@Single(binds = [RackRestoreGateway::class])
internal class RackFeatureRestoreGateway(
    private val rackDataSource: RackDataSource,
) : RackRestoreGateway {

    override suspend fun replaceRestoredRacks(racks: List<Rack>): Result<DomainError, Long> {
        rackDataSource.clear()
        return racks.saveRestoredEntries(save = rackDataSource::saveRack)
    }
}

@Single(binds = [SlotRestoreGateway::class])
internal class SlotFeatureRestoreGateway(
    private val slotDataSource: SlotDataSource,
) : SlotRestoreGateway {

    override suspend fun replaceRestoredSlots(slots: List<ShelfSlot>): Result<DomainError, Long> {
        slotDataSource.clear()
        return slots.saveRestoredEntries(save = slotDataSource::saveSlot)
    }
}

@Single(binds = [ItemRestoreGateway::class])
internal class ItemFeatureRestoreGateway(
    private val itemDataSource: ItemDataSource,
) : ItemRestoreGateway {

    override suspend fun replaceRestoredItems(items: List<Item>): Result<DomainError, Long> {
        itemDataSource.clear()
        return items.saveRestoredEntries(save = itemDataSource::saveItem)
    }
}

@Single(binds = [PhotoRestoreGateway::class])
internal class PhotoSyncFeatureRestoreGateway(
    private val photoSyncScopeDataSource: PhotoSyncScopeDataSource,
) : PhotoRestoreGateway {

    override suspend fun replaceRestoredPhotoSyncScope(
        photoSyncScope: List<PhotoSyncScope>,
    ): Result<DomainError, Long> =
        photoSyncScopeDataSource.clearPhotoSyncScope()
            .suspendFlatMap {
                photoSyncScope.saveRestoredEntries(save = photoSyncScopeDataSource::savePhotoSyncScope)
            }
}

@Single(binds = [AccountRestoreMetadataGateway::class])
internal class AccountSyncFeatureRestoreMetadataGateway(
    private val accountDatasetDataSource: AccountDatasetDataSource,
    private val localDatasetStateDataSource: LocalDatasetStateDataSource,
    private val syncStateDataSource: SyncStateDataSource,
) : AccountRestoreMetadataGateway {

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> =
        localDatasetStateDataSource.getLocalDatasetState()

    override suspend fun markRestoreSynchronized(
        accountDataset: AccountDataset,
        localDatasetState: LocalDatasetState,
        syncState: SyncState,
    ): Result<DomainError, Unit> =
        accountDatasetDataSource.saveAccountDataset(accountDataset = accountDataset)
            .suspendFlatMap {
                localDatasetStateDataSource.saveLocalDatasetState(localDatasetState = localDatasetState)
            }
            .suspendFlatMap {
                syncStateDataSource.saveSyncState(syncState = syncState)
            }
            .map { Unit }

    override suspend fun markRestorePending(
        localDatasetState: LocalDatasetState,
        syncState: SyncState,
    ): Result<DomainError, Unit> =
        localDatasetStateDataSource.saveLocalDatasetState(localDatasetState = localDatasetState)
            .suspendFlatMap {
                syncStateDataSource.saveSyncState(syncState = syncState)
            }
            .map { Unit }
}

private suspend fun <T, Saved> List<T>.saveRestoredEntries(
    save: suspend (T) -> Result<DomainError, Saved>,
): Result<DomainError, Long> {
    var savedCount = 0L
    for (entry in this) {
        save(entry).failureOrNull()?.let { error -> return error.err() }
        savedCount += 1L
    }
    return savedCount.ok()
}
