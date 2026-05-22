package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository

class DeleteRackUseCaseTest {
    private lateinit var sut: DeleteRackUseCaseType
    private lateinit var fakeRackRepository: FakeRackRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var fakeItemRepository: FakeItemRepository

    @BeforeTest
    fun setUp() {
        fakeRackRepository = FakeRackRepository()
        fakeSlotRepository = FakeSlotRepository()
        fakeItemRepository = FakeItemRepository()
        sut = DeleteRackUseCase(
            rackRepository = fakeRackRepository,
            slotRepository = fakeSlotRepository,
            itemRepository = fakeItemRepository,
        )
    }

    @Test
    fun `GIVEN slot deletion fails WHEN deleting rack THEN returns error and stops cascade`() = runTest {
        val expectedError = DomainError.ValidationError(field = "rackId", reason = "slots failed")
        fakeSlotRepository.deleteByRackResult = expectedError.err()

        val result = sut(input = "rack-1")

        assertTrue(actual = result.isErr)
        assertEquals(expected = expectedError, actual = result.failureOrNull())
        assertEquals(expected = 0, actual = fakeItemRepository.deleteItemsByRackInvocations.size)
        assertEquals(expected = 0, actual = fakeRackRepository.deleteRackInvocations.size)
    }

    @Test
    fun `GIVEN item deletion fails WHEN deleting rack THEN returns error and does not delete rack`() = runTest {
        val expectedError = DomainError.ValidationError(field = "rackId", reason = "items failed")
        fakeItemRepository.deleteItemsByRackResult = expectedError.err()

        val result = sut(input = "rack-1")

        assertTrue(actual = result.isErr)
        assertEquals(expected = expectedError, actual = result.failureOrNull())
        assertEquals(expected = listOf("rack-1"), actual = fakeSlotRepository.deleteByRackInvocations)
        assertEquals(expected = 0, actual = fakeRackRepository.deleteRackInvocations.size)
    }

    @Test
    fun `GIVEN dependencies delete successfully WHEN deleting rack THEN deletes rack and returns success`() = runTest {
        fakeSlotRepository.deleteByRackResult = 2L.ok()
        fakeItemRepository.itemsByRackResult = listOf(
            Item(id = "item-1", rackId = "rack-1", slotId = "slot-1", name = "A"),
            Item(id = "item-2", rackId = "rack-1", slotId = "slot-2", name = "B"),
        ).ok()

        val result = sut(input = "rack-1")

        assertTrue(actual = result.isOk)
        assertEquals(
            expected = DeleteRackOutcome.Deleted(
                rackId = "rack-1",
                deletedSlotCount = 2L,
                deletedItemCount = 2,
            ),
            actual = result.getOrNull(),
        )
        assertEquals(expected = listOf("rack-1"), actual = fakeSlotRepository.deleteByRackInvocations)
        assertEquals(expected = listOf("rack-1"), actual = fakeItemRepository.deleteItemsByRackInvocations)
        assertEquals(expected = listOf("rack-1"), actual = fakeRackRepository.deleteRackInvocations)
    }
}

private class FakeRackRepository : RackRepository {
    val deleteRackInvocations: MutableList<String> = mutableListOf()
    var deleteRackResult: Result<DomainError, Unit> = Unit.ok()

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> = emptyFlow()

    override suspend fun getRackById(id: String): Result<DomainError, Rack> =
        DomainError.NotFound(resource = "Rack", id = id).err()

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> = rack.ok()

    override suspend fun deleteRack(id: String): Result<DomainError, Unit> {
        deleteRackInvocations += id
        return deleteRackResult
    }

    override suspend fun clear() = Unit
}

private class FakeSlotRepository : SlotRepository {
    val deleteByRackInvocations: MutableList<String> = mutableListOf()
    var deleteByRackResult: Result<DomainError, Long> = 1L.ok()

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> = emptyList<ShelfSlot>().ok()

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> = slot.ok()

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Long> {
        deleteByRackInvocations += rackId
        return deleteByRackResult
    }

    override suspend fun clear() = Unit
}

private class FakeItemRepository : ItemRepository {
    val deleteItemsByRackInvocations: MutableList<String> = mutableListOf()
    var deleteItemsByRackResult: Result<DomainError, Unit> = Unit.ok()
    var itemsByRackResult: Result<DomainError, List<Item>> = emptyList<Item>().ok()

    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> = itemsByRackResult

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> =
        emptyList<Item>().ok()

    override suspend fun getItemById(id: String): Result<DomainError, Item> =
        DomainError.NotFound(resource = "Item", id = id).err()

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> = emptyList<Item>().ok()

    override suspend fun saveItem(item: Item): Result<DomainError, Item> = item.ok()

    override suspend fun deleteItem(id: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit> {
        deleteItemsByRackInvocations += rackId
        return deleteItemsByRackResult
    }

    override suspend fun clear() = Unit
}
