package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Single
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.deafsapps.storeit.base.Result

@Single(binds = [SlotRepository::class])
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

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Unit> = mutex.withLock {
        when {
            rackId.isBlank() -> DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            else -> {
                Unit.ok().also {
                    slots.keys.toList().filter { slots[it]?.rackId == rackId }.forEach { slots.remove(it) }
                }
            }
        }
    }

    suspend fun clear() = mutex.withLock {
        slots.clear()
    }
}
