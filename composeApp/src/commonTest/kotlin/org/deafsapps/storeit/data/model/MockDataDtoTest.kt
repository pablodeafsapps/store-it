package org.deafsapps.storeit.data.model

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.fake.FakeItemRepository
import org.deafsapps.storeit.fake.FakeRackRepository

class MockDataDtoTest {
    private lateinit var sut: MockDataDto

    @BeforeTest
    fun setUp() {
        sut = MockDataDto
    }

    @Test
    fun `getSampleRack returns rack with expected id and name`() {
        val rack = sut.getSampleRack()
        assertEquals(expected = sut.MOCK_RACK_ID, actual = rack.id)
        assertEquals(expected = "Garage shelf", actual = rack.name)
        assertEquals(expected = "Garage", actual = rack.location)
    }

    @Test
    fun `getSampleSlots returns two slots for mock rack`() {
        val slots = sut.getSampleSlots()
        assertEquals(expected = 2, actual = slots.size)
        assertTrue(actual = slots.all { it.rackId == sut.MOCK_RACK_ID })
        assertEquals(expected = sut.MOCK_SLOT_A_ID, actual = slots[0].id)
        assertEquals(expected = sut.MOCK_SLOT_B_ID, actual = slots[1].id)
    }

    @Test
    fun `getSampleItems returns three items`() {
        val items = sut.getSampleItems()
        assertEquals(expected = 3, actual = items.size)
        assertEquals(expected = sut.MOCK_RACK_ID, actual = items[0].rackId)
        assertTrue(actual = items.any { it.name == "Power drill" })
        assertTrue(actual = items.any { it.name == "Paint cans" })
        assertTrue(actual = items.any { it.name == "Toolbox" })
    }

    @Test
    fun `preloadMockData populates rack and item repositories`() = runTest {
        val fakeRackRepository: RackRepository = FakeRackRepository()
        val fakeItemRepository: ItemRepository = FakeItemRepository()
        sut.preloadMockData(rackRepository = fakeRackRepository, itemRepository = fakeItemRepository)

        val racks = fakeRackRepository.getAllRacks().getOrNull() ?: emptyList()
        assertEquals(expected = 1, actual = racks.size)
        assertEquals(expected = sut.MOCK_RACK_ID, actual = racks[0].id)

        val items = fakeItemRepository.getItemsByRack(rackId = sut.MOCK_RACK_ID).getOrNull() ?: emptyList()
        assertEquals(expected = 3, actual = items.size)
    }
}
