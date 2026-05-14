package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.deafsapps.storeit.base.Result

internal class InMemorySlotRepository : SlotRepository {
    private val slots = mutableMapOf<String, ShelfSlot>()
    private val mutex = Mutex()

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> = mutex.withLock {
        when {
            rackId.isBlank() -> DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            else -> slots.values.filter { it.rackId == rackId }.ok()
        }
    }

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> = mutex.withLock {
        when {
            slot.id.isBlank() ->
                DomainError.ValidationError(field = "id", reason = "Slot ID cannot be blank").err()
            slot.rackId.isBlank() ->
                DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            else ->
                slot.ok().also { slots[slot.id] = slot }
        }
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Long> = mutex.withLock {
        when {
            rackId.isBlank() -> DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            else -> {
                val deletedCount = slots.keys
                    .toList()
                    .count { key -> slots[key]?.rackId == rackId }
                    .toLong()
                slots.keys.toList().filter { key -> slots[key]?.rackId == rackId }.forEach { key -> slots.remove(key) }
                deletedCount.ok()
            }
        }
    }

    override suspend fun clear() = mutex.withLock {
        slots.clear()
    }
}
