package org.deafsapps.storeit.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository

internal class FakeSlotRepository : SlotRepository {
    private val slots = mutableMapOf<String, ShelfSlot>()

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> =
        slots.values.filter { it.rackId == rackId }.ok()

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> = run {
        slot.ok().also { slots[slot.id] = slot }
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Unit> = run {
        Unit.ok().also {
            slots.keys.toList().filter { slots[it]?.rackId == rackId }.forEach { slots.remove(it) }
        }
    }

    override suspend fun clear() {
        slots.clear()
    }
}
