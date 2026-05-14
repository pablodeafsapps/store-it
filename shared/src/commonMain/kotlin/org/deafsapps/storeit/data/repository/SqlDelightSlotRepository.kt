package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.getOrElse
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.data.datasource.SlotDataSource
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Single

@Single(binds = [SlotRepository::class])
internal class SqlDelightSlotRepository(
    private val slotDataSource: SlotDataSource,
    private val syncOperationRepository: SyncOperationRepository,
    private val localDatasetStateDataSource: LocalDatasetStateDataSource,
) : SlotRepository {

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> =
        if (rackId.isBlank()) {
            DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
        } else {
            slotDataSource.getSlotsByRack(rackId = rackId)
        }

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> {
        if (slot.id.isBlank()) return DomainError.ValidationError(
            field = "id",
            reason = "Slot ID cannot be blank",
        ).err()
        if (slot.rackId.isBlank()) return DomainError.ValidationError(
            field = "rackId",
            reason = "Rack ID cannot be blank",
        ).err()
        val existingSlots = getSlotsByRack(rackId = slot.rackId).getOrElse { error -> return error }
        val savedSlot = slotDataSource.saveSlot(slot = slot).getOrElse { error -> return error }
        return if (existingSlots.any { existing -> existing.id == savedSlot.id }) {
            syncOperationRepository.enqueueUpdate(
                accountId = resolveAccountId(),
                entityType = SyncEntityType.ShelfSlot,
                entityId = savedSlot.id,
            ).map { savedSlot }
        } else {
            syncOperationRepository.enqueueCreate(
                accountId = resolveAccountId(),
                entityType = SyncEntityType.ShelfSlot,
                entityId = savedSlot.id,
            ).map { savedSlot }
        }
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Long> =
        if (rackId.isBlank()) {
            DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
        } else {
            val existingSlots = slotDataSource.getSlotsByRack(rackId = rackId).getOrElse { error -> return error }
            val deletedCount = slotDataSource.deleteByRack(rackId = rackId).getOrElse { error -> return error }
            for (slot in existingSlots) {
                syncOperationRepository.enqueueDelete(
                    accountId = resolveAccountId(),
                    entityType = SyncEntityType.ShelfSlot,
                    entityId = slot.id,
                ).getOrElse { error -> return error }
            }
            deletedCount.ok()
        }

    override suspend fun clear() {
        slotDataSource.clear()
    }

    private suspend fun resolveAccountId(): String? =
        localDatasetStateDataSource.getLocalDatasetState().getOrNull()?.accountId
}
