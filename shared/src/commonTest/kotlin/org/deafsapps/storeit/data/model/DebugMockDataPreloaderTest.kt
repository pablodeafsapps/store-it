package org.deafsapps.storeit.data.model

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.fake.FakeItemRepository
import org.deafsapps.storeit.fake.FakeRackRepository
import org.deafsapps.storeit.fake.FakeSlotRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugMockDataPreloaderTest {
    private lateinit var fakeRackRepository: FakeRackRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var fakeItemRepository: FakeItemRepository
    private lateinit var sut: DebugMockDataPreloader

    @BeforeTest
    fun setUp() {
        fakeRackRepository = FakeRackRepository()
        fakeSlotRepository = FakeSlotRepository()
        fakeItemRepository = FakeItemRepository()
        sut = DebugMockDataPreloader(
            rackRepository = fakeRackRepository,
            slotRepository = fakeSlotRepository,
            itemRepository = fakeItemRepository,
        )
    }

    @Test
    fun `GIVEN empty repositories WHEN preloadIfEmpty THEN seeds mock data`() = runTest {
        sut.preloadIfEmpty()

        val racks = fakeRackRepository.getAllRacksFlow().first().getOrNull().orEmpty()
        val slots = fakeSlotRepository.getSlotsByRack(rackId = MockDataDto.MOCK_RACK_ID).getOrNull().orEmpty()
        val items = fakeItemRepository.getItemsByRack(rackId = MockDataDto.MOCK_RACK_ID).getOrNull().orEmpty()

        assertEquals(expected = 1, actual = racks.size)
        assertEquals(expected = 2, actual = slots.size)
        assertEquals(expected = 3, actual = items.size)
    }

    @Test
    fun `GIVEN repositories already contain mock rack WHEN preloadIfEmpty THEN does not duplicate mock data`() = runTest {
        sut.preloadIfEmpty()

        sut.preloadIfEmpty()

        val racks = fakeRackRepository.getAllRacksFlow().first().getOrNull().orEmpty()
        val slots = fakeSlotRepository.getSlotsByRack(rackId = MockDataDto.MOCK_RACK_ID).getOrNull().orEmpty()
        val items = fakeItemRepository.getItemsByRack(rackId = MockDataDto.MOCK_RACK_ID).getOrNull().orEmpty()

        assertEquals(expected = 1, actual = racks.size)
        assertEquals(expected = 2, actual = slots.size)
        assertEquals(expected = 3, actual = items.size)
    }
}
