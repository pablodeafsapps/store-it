package org.deafsapps.storeit.domain.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot

interface SlotRepository {
    suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>>
    suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot>
    suspend fun deleteByRack(rackId: String): Result<DomainError, Unit>
}
