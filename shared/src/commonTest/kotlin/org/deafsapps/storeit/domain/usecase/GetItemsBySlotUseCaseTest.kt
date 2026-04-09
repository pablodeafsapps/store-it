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
import org.deafsapps.storeit.fake.FakeItemRepository

class GetItemsBySlotUseCaseTest {
    private lateinit var sut: GetItemsBySlotUseCase
    private lateinit var fakeItemRepository: FakeItemRepository

    @BeforeTest
    fun setUp() {
        fakeItemRepository = FakeItemRepository()
        sut = GetItemsBySlotUseCase(itemRepository = fakeItemRepository)
    }

    @Test
    fun `GIVEN fake returns empty list WHEN invoke THEN returns empty list`() = runTest {
        fakeItemRepository.getItemsBySlotResult = emptyList<Item>().ok()

        val result = sut(input = GetItemsBySlotInput(rackId = "r1", slotId = "s1"))

        assertTrue(actual = result.isOk)
        val items = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = items.size)
    }

    @Test
    fun `GIVEN fake returns two items WHEN invoke THEN returns both`() = runTest {
        val item1 = Item(id = "1", rackId = "r1", slotId = "s1", name = "Item 1")
        val item2 = Item(id = "2", rackId = "r1", slotId = "s1", name = "Item 2")
        fakeItemRepository.getItemsBySlotResult = listOf(item1, item2).ok()

        val result = sut(input = GetItemsBySlotInput(rackId = "r1", slotId = "s1"))

        assertTrue(actual = result.isOk)
        val items = result.getOrNull() ?: emptyList()
        assertEquals(expected = 2, actual = items.size)
        assertTrue(actual = items.containsAll(listOf(item1, item2)))
    }

    @Test
    fun `GIVEN fake returns error WHEN invoke THEN returns same error`() = runTest {
        fakeItemRepository.getItemsBySlotResult = DomainError.Unknown.err()

        val result = sut(input = GetItemsBySlotInput(rackId = "r1", slotId = "s1"))

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.Unknown)
    }

    @Test
    fun `GIVEN fake returns NotFound WHEN invoke THEN returns NotFound`() = runTest {
        fakeItemRepository.getItemsBySlotResult = DomainError.NotFound(resource = "Slot", id = "s1").err()

        val result = sut(input = GetItemsBySlotInput(rackId = "r1", slotId = "s1"))

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
        assertEquals(expected = "Slot", actual = (error as DomainError.NotFound).resource)
    }
}
