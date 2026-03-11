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

class AddItemUseCaseTest {
    private lateinit var sut: AddItemUseCase
    private lateinit var fakeItemRepository: FakeItemRepository

    @BeforeTest
    fun setUp() {
        fakeItemRepository = FakeItemRepository()
        sut = AddItemUseCase(itemRepository = fakeItemRepository)
    }

    @Test
    fun `GIVEN fake returns saved item WHEN invoke with valid item THEN returns saved item`() = runTest {
        val item = Item(id = "1", rackId = "r1", slotId = "s1", name = "Item 1", description = "Desc")
        fakeItemRepository.saveItemResult = item.ok()

        val result = sut(input = item)

        assertTrue(actual = result.isOk)
        val saved = result.getOrNull()
        assertEquals(expected = item.id, actual = saved?.id)
        assertEquals(expected = item.name, actual = saved?.name)
        assertEquals(expected = item.rackId, actual = saved?.rackId)
        assertEquals(expected = item.slotId, actual = saved?.slotId)
    }

    @Test
    fun `GIVEN fake returns updated item WHEN invoke with same id THEN returns updated item`() = runTest {
        val item = Item(id = "1", rackId = "r1", slotId = "s1", name = "Original")
        val updated = item.copy(name = "Updated", description = "New desc")
        fakeItemRepository.saveItemResult = updated.ok()

        val result = sut(input = updated)

        assertTrue(actual = result.isOk)
        val saved = result.getOrNull()
        assertEquals(expected = "Updated", actual = saved?.name)
        assertEquals(expected = "New desc", actual = saved?.description)
    }

    @Test
    fun `GIVEN fake returns ValidationError WHEN invoke THEN returns ValidationError`() = runTest {
        val item = Item(id = "1", rackId = "r1", slotId = "s1", name = "Item")
        fakeItemRepository.saveItemResult = DomainError.ValidationError(field = "name", reason = "Required").err()

        val result = sut(input = item)

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "name", actual = (error as DomainError.ValidationError).field)
    }

    @Test
    fun `GIVEN fake returns NotFound WHEN invoke THEN returns NotFound`() = runTest {
        val item = Item(id = "1", rackId = "r1", slotId = "s1", name = "Item")
        fakeItemRepository.saveItemResult = DomainError.NotFound(resource = "Rack", id = "r1").err()

        val result = sut(input = item)

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
        assertEquals(expected = "Rack", actual = (error as DomainError.NotFound).resource)
    }
}
