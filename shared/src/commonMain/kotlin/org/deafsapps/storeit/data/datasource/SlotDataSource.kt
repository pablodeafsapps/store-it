package org.deafsapps.storeit.data.datasource

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot

internal interface SlotDataSource {
    suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>>

    suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot>

    suspend fun deleteByRack(rackId: String): Result<DomainError, Long>

    suspend fun clear()
}
