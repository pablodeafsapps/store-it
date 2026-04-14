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

internal class GetItemByIdUseCaseTest {
    private lateinit var sut: GetItemByIdUseCase
    private lateinit var fakeItemRepository: FakeItemRepository

    @BeforeTest
    fun setUp() {
        fakeItemRepository = FakeItemRepository()
        sut = GetItemByIdUseCase(itemRepository = fakeItemRepository)
    }

    @Test
    fun `GIVEN fake returns item WHEN invoke THEN returns item`() = runTest {
        val item = Item(id = "i1", rackId = "r1", slotId = "s1", name = "Hammer")
        fakeItemRepository.getItemByIdResult = item.ok()

        val result = sut(input = "i1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = item, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN fake returns NotFound WHEN invoke THEN returns NotFound`() = runTest {
        fakeItemRepository.getItemByIdResult =
            DomainError.NotFound(resource = "Item", id = "missing").err()

        val result = sut(input = "missing")

        assertTrue(actual = result.isErr)
        val err = result.failureOrNull()
        assertTrue(actual = err is DomainError.NotFound)
        assertEquals(expected = "missing", actual = (err as DomainError.NotFound).id)
    }

    @Test
    fun `GIVEN fake returns Unknown WHEN invoke THEN returns unknown`() = runTest {
        fakeItemRepository.getItemByIdResult = DomainError.Unknown().err()

        val result = sut(input = "i1")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.Unknown)
    }
}
