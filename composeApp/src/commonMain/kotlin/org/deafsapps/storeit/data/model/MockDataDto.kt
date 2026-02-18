package org.deafsapps.storeit.data.model

import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal object MockDataDto {
    const val MOCK_RACK_ID = "mock-rack-001"
    const val MOCK_SLOT_A_ID = "mock-slot-a"
    const val MOCK_SLOT_B_ID = "mock-slot-b"
    const val MOCK_ITEM_1_ID = "mock-item-001"
    const val MOCK_ITEM_2_ID = "mock-item-002"
    const val MOCK_ITEM_3_ID = "mock-item-003"

    fun getSampleRack(): Rack = Rack(
        id = MOCK_RACK_ID,
        name = "Garage shelf",
        description = "Main storage shelf in garage",
        location = "Garage",
        photoUri = null,
        createdAt = Clock.System.now().toEpochMilliseconds(),
        updatedAt = null,
    )

    fun getSampleSlots(): List<ShelfSlot> = listOf(
        ShelfSlot(
            id = MOCK_SLOT_A_ID,
            rackId = MOCK_RACK_ID,
            position = SlotPosition(x = 0.25f, y = 0.33f, xRel = 0.25f, yRel = 0.33f),
        ),
        ShelfSlot(
            id = MOCK_SLOT_B_ID,
            rackId = MOCK_RACK_ID,
            position = SlotPosition(x = 0.5f, y = 0.66f, xRel = 0.5f, yRel = 0.66f),
        ),
    )

    fun getSampleItems(): List<Item> = listOf(
        Item(
            id = MOCK_ITEM_1_ID,
            rackId = MOCK_RACK_ID,
            slotId = MOCK_SLOT_A_ID,
            name = "Power drill",
            description = "Cordless drill with battery",
            photoUri = null,
            quantity = 1,
            owner = "Household",
            tags = listOf("tools", "garage"),
        ),
        Item(
            id = MOCK_ITEM_2_ID,
            rackId = MOCK_RACK_ID,
            slotId = MOCK_SLOT_A_ID,
            name = "Paint cans",
            description = "Leftover paint for touch-ups",
            photoUri = null,
            quantity = 3,
            owner = "",
            tags = listOf("paint"),
        ),
        Item(
            id = MOCK_ITEM_3_ID,
            rackId = MOCK_RACK_ID,
            slotId = MOCK_SLOT_B_ID,
            name = "Toolbox",
            description = "Metal toolbox with hand tools",
            photoUri = null,
            quantity = 1,
            owner = "",
            tags = listOf("tools"),
        ),
    )

    suspend fun preloadMockData(
        rackRepository: RackRepository,
        itemRepository: ItemRepository,
    ) {
        rackRepository.saveRack(rack = getSampleRack())
        getSampleItems().forEach { item -> itemRepository.saveItem(item = item) }
    }

    fun preloadMockDataAsync(
        scope: CoroutineScope,
        rackRepository: RackRepository,
        itemRepository: ItemRepository,
    ) {
        scope.launch { preloadMockData(rackRepository = rackRepository, itemRepository = itemRepository) }
    }
}
