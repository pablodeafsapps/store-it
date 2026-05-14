package org.deafsapps.storeit.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository

internal class FakeSlotRepository : SlotRepository {
    private val slots = mutableMapOf<String, ShelfSlot>()

    var getSlotsByRackResult: Result<DomainError, List<ShelfSlot>>? = null

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> =
        getSlotsByRackResult ?: slots.values.filter { it.rackId == rackId }.ok()

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> = run {
        slot.ok().also { slots[slot.id] = slot }
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Long> = run {
        val deletedCount = slots.keys
            .toList()
            .count { key -> slots[key]?.rackId == rackId }
            .toLong()
        slots.keys.toList().filter { key -> slots[key]?.rackId == rackId }.forEach { key -> slots.remove(key) }
        deletedCount.ok()
    }

    override suspend fun clear() {
        slots.clear()
    }
}
