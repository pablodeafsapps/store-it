package org.deafsapps.storeit.domain.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot

/**
 * Defines rack-slot persistence operations for the domain layer.
 */
interface SlotRepository {
    /**
     * Returns every slot defined for the given rack.
     */
    suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>>

    /**
     * Creates or updates a slot definition.
     */
    suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot>

    /**
     * Deletes every slot defined for the given rack.
     */
    suspend fun deleteByRack(rackId: String): Result<DomainError, Unit>

    /**
     * Removes all stored slots.
     */
    suspend fun clear()
}
