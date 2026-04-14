package org.deafsapps.storeit.domain.usecase

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.fake.FakeItemRepository
import org.deafsapps.storeit.fake.FakeRackRepository
import org.deafsapps.storeit.fake.FakeSlotRepository

internal class SearchItemsUseCaseTest {
    private lateinit var sut: SearchItemsUseCase
    private lateinit var fakeItemRepository: FakeItemRepository
    private lateinit var fakeRackRepository: FakeRackRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository

    @BeforeTest
    fun setUp() {
        fakeRackRepository = FakeRackRepository()
        fakeSlotRepository = FakeSlotRepository()
        fakeItemRepository = FakeItemRepository()
        sut = SearchItemsUseCase(
            getRackByIdUseCase = GetRackByIdUseCase(rackRepository = fakeRackRepository),
            getSlotsByRackIdUseCase = GetSlotsByRackIdUseCase(slotRepository = fakeSlotRepository),
            itemRepository = fakeItemRepository,
        )
    }

    @Test
    fun `GIVEN search returns empty WHEN invoke THEN returns empty`() = runTest {
        fakeItemRepository.searchItemsResult = emptyList<Item>().ok()

        val result = sut(input = "needle")

        assertTrue(actual = result.isOk)
        assertEquals(expected = emptyList(), actual = result.getOrNull())
    }

    @Test
    fun `GIVEN search matches WHEN invoke THEN enriches with rack name and slot summary`() = runTest {
        val rack = Rack(id = "r1", name = "Garage rack")
        fakeRackRepository.saveRack(rack)
        val slot = ShelfSlot(
            id = "s1",
            rackId = "r1",
            position = SlotPosition(x = 0f, y = 0f, xRel = 0.25f, yRel = 0.5f),
        )
        fakeSlotRepository.saveSlot(slot)
        val item = Item(id = "i1", rackId = "r1", slotId = "s1", name = "Drill")
        fakeItemRepository.searchItemsResult = listOf(item).ok()

        val result = sut(input = "drill")

        assertTrue(actual = result.isOk)
        val rows = result.getOrNull().orEmpty()
        assertEquals(expected = 1, actual = rows.size)
        assertEquals(expected = item, actual = rows[0].item)
        assertEquals(expected = "Garage rack", actual = rows[0].rackName)
        assertEquals(expected = "25%, 50%", actual = rows[0].slotSummary)
    }

    @Test
    fun `GIVEN slot id missing from rack WHEN invoke THEN slot summary is slot id`() = runTest {
        fakeRackRepository.saveRack(Rack(id = "r190", name = "Only rack"))
        val item = Item(id = "i2", rackId = "r190", slotId = "ghost", name = "Orphan")
        fakeItemRepository.searchItemsResult = listOf(item).ok()

        val result = sut(input = "orph")

        assertTrue(actual = result.isOk)
        assertEquals(expected = "ghost", actual = result.getOrNull()?.first()?.slotSummary)
    }

    @Test
    fun `GIVEN search fails WHEN invoke THEN returns error`() = runTest {
        fakeItemRepository.searchItemsResult = DomainError.Unknown().err()

        val result = sut(input = "q")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.Unknown)
    }

    @Test
    fun `GIVEN rack missing during enrich WHEN invoke THEN returns NotFound`() = runTest {
        fakeItemRepository.searchItemsResult =
            listOf(Item(id = "i1", rackId = "missing-rack", slotId = "s1", name = "Box")).ok()

        val result = sut(input = "box")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.NotFound)
    }

    @Test
    fun `GIVEN slots load fails during enrich WHEN invoke THEN returns error`() = runTest {
        fakeRackRepository.saveRack(Rack(id = "r1", name = "R"))
        fakeItemRepository.searchItemsResult =
            listOf(Item(id = "i1", rackId = "r1", slotId = "s1", name = "Box")).ok()
        fakeSlotRepository.getSlotsByRackResult = DomainError.Unknown().err()

        val result = sut(input = "box")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.Unknown)
    }
}
