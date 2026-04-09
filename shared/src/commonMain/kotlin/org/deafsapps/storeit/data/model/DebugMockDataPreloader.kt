package org.deafsapps.storeit.data.model

import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Single

@Single
class DebugMockDataPreloader(
    private val rackRepository: RackRepository,
    private val slotRepository: SlotRepository,
    private val itemRepository: ItemRepository,
) {
    suspend fun preloadIfEmpty() {
        val existingRack = rackRepository.getRackById(id = MockDataDto.MOCK_RACK_ID).getOrNull()
        if (existingRack != null) return

        MockDataDto.preloadMockData(
            rackRepository = rackRepository,
            slotRepository = slotRepository,
            itemRepository = itemRepository,
        )
    }
}
