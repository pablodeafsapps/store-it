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

internal class DeleteItemUseCaseTest {
    private lateinit var sut: DeleteItemUseCase
    private lateinit var fakeItemRepository: FakeItemRepository

    @BeforeTest
    fun setUp() {
        fakeItemRepository = FakeItemRepository()
        sut = DeleteItemUseCase(itemRepository = fakeItemRepository)
    }

    @Test
    fun `GIVEN item exists WHEN invoke THEN removes item`() = runTest {
        val item = Item(id = "i1", rackId = "r1", slotId = "s1", name = "Hammer")
        fakeItemRepository.saveItem(item = item)

        val result = sut(input = "i1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = Unit, actual = result.getOrNull())
        assertTrue(actual = fakeItemRepository.getItemById(id = "i1").isErr)
    }

    @Test
    fun `GIVEN fake returns NotFound WHEN invoke THEN returns NotFound`() = runTest {
        fakeItemRepository.deleteItemResult =
            DomainError.NotFound(resource = "Item", id = "missing").err()

        val result = sut(input = "missing")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.NotFound)
    }
}
