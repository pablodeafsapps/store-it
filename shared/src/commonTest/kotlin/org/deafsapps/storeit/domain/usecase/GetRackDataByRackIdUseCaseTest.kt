package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.fake.FakeItemRepository
import org.deafsapps.storeit.fake.FakeRackRepository
import org.deafsapps.storeit.fake.FakeSlotRepository
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetRackDataByRackIdUseCaseTest {

    @Test
    fun `returns rack slots and items for rack`() = runTest {
        val rackRepo = FakeRackRepository()
        val slotRepo = FakeSlotRepository()
        val itemRepo = FakeItemRepository()
        val rack = Rack(id = "r1", name = "Garage")
        rackRepo.saveRack(rack)
        val slot = ShelfSlot(id = "s1", rackId = "r1", position = SlotPosition(0f, 0f, 0.5f, 0.5f))
        slotRepo.saveSlot(slot)
        val item = Item(id = "i1", rackId = "r1", slotId = "s1", name = "Box")
        itemRepo.saveItem(item)

        val sut = GetRackDataByRackIdUseCase(
            getRackByIdUseCase = GetRackByIdUseCase(rackRepo),
            getSlotsByRackIdUseCase = GetSlotsByRackIdUseCase(slotRepo),
            itemRepository = itemRepo,
        )

        val data = sut(input = "r1").getOrNull()
        assertEquals("r1", data?.rack?.id)
        assertEquals(1, data?.shelfSlots?.size)
        assertEquals(1, data?.items?.size)
        assertEquals("Box", data?.items?.first()?.name)
    }
}
