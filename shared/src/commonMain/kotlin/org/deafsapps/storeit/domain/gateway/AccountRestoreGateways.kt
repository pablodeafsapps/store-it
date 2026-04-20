package org.deafsapps.storeit.domain.gateway

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SyncState

/**
 * Gateway owned by the rack feature for account-restore write operations.
 *
 * This keeps account restore orchestration independent from rack repositories,
 * data sources, and future rack module internals.
 */
interface RackRestoreGateway {
    /**
     * Replaces the locally restored rack slice with the authoritative remote account slice.
     *
     * @return the number of rack records written during restore.
     */
    suspend fun replaceRestoredRacks(racks: List<Rack>): Result<DomainError, Long>
}

/**
 * Gateway owned by the slot feature for account-restore write operations.
 */
interface SlotRestoreGateway {
    /**
     * Replaces the locally restored slot slice with the authoritative remote account slice.
     *
     * @return the number of slot records written during restore.
     */
    suspend fun replaceRestoredSlots(slots: List<ShelfSlot>): Result<DomainError, Long>
}

/**
 * Gateway owned by the item feature for account-restore write operations.
 */
interface ItemRestoreGateway {
    /**
     * Replaces the locally restored item slice with the authoritative remote account slice.
     *
     * @return the number of item records written during restore.
     */
    suspend fun replaceRestoredItems(items: List<Item>): Result<DomainError, Long>
}

/**
 * Gateway owned by the photo backup feature for account-restore photo scope operations.
 */
interface PhotoRestoreGateway {
    /**
     * Replaces restored photo synchronization scope entries for the account dataset.
     *
     * @return the number of photo sync scope records written during restore.
     */
    suspend fun replaceRestoredPhotoSyncScope(photoSyncScope: List<PhotoSyncScope>): Result<DomainError, Long>
}

/**
 * Gateway owned by the sync/account metadata feature for restore metadata transitions.
 */
interface AccountRestoreMetadataGateway {
    /**
     * Returns the current local dataset state before restore starts.
     */
    suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?>

    /**
     * Persists the successful restore checkpoint and synchronized local state.
     */
    suspend fun markRestoreSynchronized(
        accountDataset: AccountDataset,
        localDatasetState: LocalDatasetState,
        syncState: SyncState,
    ): Result<DomainError, Unit>

    /**
     * Persists restore-pending state after a restore failure that can be retried later.
     */
    suspend fun markRestorePending(
        localDatasetState: LocalDatasetState,
        syncState: SyncState,
    ): Result<DomainError, Unit>
}
