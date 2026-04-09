package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.data.datasource.SlotDataSource
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Single

@Single(binds = [SlotRepository::class])
internal class SqlDelightSlotRepository(
    private val slotDataSource: SlotDataSource,
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
        return slotDataSource.saveSlot(slot = slot)
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Unit> =
        if (rackId.isBlank()) {
            DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
        } else {
            slotDataSource.deleteByRack(rackId = rackId)
        }

    override suspend fun clear() {
        slotDataSource.clear()
    }
}
