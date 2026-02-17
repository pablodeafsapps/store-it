package org.deafsapps.storeit.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.data.repository.InMemoryItemRepository
import org.deafsapps.storeit.data.repository.InMemoryRackRepository

class MockDataDtoTest {

    @Test
    fun `getSampleRack returns rack with expected id and name`() {
        val rack = MockDataDto.getSampleRack()
        assertEquals(MockDataDto.MOCK_RACK_ID, rack.id)
        assertEquals("Garage shelf", rack.name)
        assertEquals("Garage", rack.location)
    }

    @Test
    fun `getSampleSlots returns two slots for mock rack`() {
        val slots = MockDataDto.getSampleSlots()
        assertEquals(2, slots.size)
        assertTrue(slots.all { it.rackId == MockDataDto.MOCK_RACK_ID })
        assertEquals(MockDataDto.MOCK_SLOT_A_ID, slots[0].id)
        assertEquals(MockDataDto.MOCK_SLOT_B_ID, slots[1].id)
    }

    @Test
    fun `getSampleItems returns three items`() {
        val items = MockDataDto.getSampleItems()
        assertEquals(3, items.size)
        assertEquals(MockDataDto.MOCK_RACK_ID, items[0].rackId)
        assertTrue(items.any { it.name == "Power drill" })
        assertTrue(items.any { it.name == "Paint cans" })
        assertTrue(items.any { it.name == "Toolbox" })
    }

    @Test
    fun `preloadMockData populates rack and item repositories`() = runTest {
        val rackRepo = InMemoryRackRepository()
        val itemRepo = InMemoryItemRepository()
        MockDataDto.preloadMockData(rackRepo, itemRepo)

        val racks = rackRepo.getAllRacks().getOrNull() ?: emptyList()
        assertEquals(1, racks.size)
        assertEquals(MockDataDto.MOCK_RACK_ID, racks[0].id)

        val items = itemRepo.getItemsByRack(MockDataDto.MOCK_RACK_ID).getOrNull() ?: emptyList()
        assertEquals(3, items.size)
    }
}
