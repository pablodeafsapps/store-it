package org.deafsapps.storeit.data.repository

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.repository.ItemRepository

class InMemoryItemRepositoryTest {
    private lateinit var sut: ItemRepository

    @BeforeTest
    fun setUp() {
        sut = InMemoryItemRepository()
    }

    @Test
    fun `GIVEN empty repository WHEN getItemsByRack THEN returns empty list`() = runTest {

        val result = sut.getItemsByRack(rackId = "rack1")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = items.size)
    }

    @Test
    fun `GIVEN items in different racks WHEN getItemsByRack THEN returns only items for specified rack`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Item 1")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Item 2")
        val item3 = Item(id = "3", rackId = "rack2", slotId = "slot1", name = "Item 3")
        sut.saveItem(item =item1)
        sut.saveItem(item =item2)
        sut.saveItem(item =item3)

        val result = sut.getItemsByRack(rackId = "rack1")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 2, actual = items.size)
        assertTrue(actual = items.contains(item1))
        assertTrue(actual = items.contains(item2))
        assertTrue(actual = !items.contains(item3))
    }

    @Test
    fun `GIVEN any state WHEN getItemsByRack with blank rackId THEN returns ValidationError`() = runTest {

        val result = sut.getItemsByRack(rackId = "")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "rackId", actual = error.field)
    }

    @Test
    fun `GIVEN items in different slots WHEN getItemsBySlot THEN returns only items for specified slot`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Item 1")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot1", name = "Item 2")
        val item3 = Item(id = "3", rackId = "rack1", slotId = "slot2", name = "Item 3")
        sut.saveItem(item =item1)
        sut.saveItem(item =item2)
        sut.saveItem(item =item3)

        val result = sut.getItemsBySlot(rackId = "rack1", slotId = "slot1")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 2, actual = items.size)
        assertTrue(actual = items.contains(item1))
        assertTrue(actual = items.contains(item2))
        assertTrue(actual = !items.contains(item3))
    }

    @Test
    fun `GIVEN empty repository WHEN getItemsBySlot THEN returns empty list`() = runTest {

        val result = sut.getItemsBySlot(rackId = "rack1", slotId = "slot1")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = items.size)
    }

    @Test
    fun `GIVEN any state WHEN getItemsBySlot with blank rackId THEN returns ValidationError`() = runTest {

        val result = sut.getItemsBySlot(rackId = "", slotId = "slot1")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "rackId", actual = error.field)
    }

    @Test
    fun `GIVEN any state WHEN getItemsBySlot with blank slotId THEN returns ValidationError`() = runTest {

        val result = sut.getItemsBySlot(rackId = "rack1", slotId = "")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "slotId", actual = error.field)
    }

    @Test
    fun `GIVEN saved item WHEN getItemById with existing id THEN returns item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Test Item")
        sut.saveItem(item =item)

        val result = sut.getItemById(id = "1")

        assertTrue(actual = result.isOk)
        val retrievedItem = result.getOrNull()
        assertEquals(expected = item, actual = retrievedItem)
    }

    @Test
    fun `GIVEN empty repository WHEN getItemById with unknown id THEN returns NotFound`() = runTest {

        val result = sut.getItemById(id = "nonexistent")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
        assertEquals(expected = "Item", actual = error.resource)
        assertEquals(expected = "nonexistent", actual = error.id)
    }

    @Test
    fun `GIVEN any state WHEN getItemById with blank id THEN returns ValidationError`() = runTest {

        val result = sut.getItemById(id = "")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "id", actual = error.field)
    }

    @Test
    fun `GIVEN items with different names WHEN searchItems by name THEN returns matching items`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Hammer")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Screwdriver")
        val item3 = Item(id = "3", rackId = "rack2", slotId = "slot1", name = "Hammer Set")
        sut.saveItem(item =item1)
        sut.saveItem(item =item2)
        sut.saveItem(item =item3)

        val result = sut.searchItems(query = "hammer")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 2, actual = items.size)
        assertTrue(actual = items.contains(item1))
        assertTrue(actual = items.contains(item3))
        assertTrue(actual = !items.contains(item2))
    }

    @Test
    fun `GIVEN items with descriptions WHEN searchItems by description THEN returns matching items`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Tool", description = "Heavy duty")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Tool", description = "Lightweight")
        sut.saveItem(item =item1)
        sut.saveItem(item =item2)

        val result = sut.searchItems(query = "heavy")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 1, actual = items.size)
        assertTrue(actual = items.contains(item1))
        assertTrue(actual = !items.contains(item2))
    }

    @Test
    fun `GIVEN items WHEN searchItems matches both name and description THEN returns all matching items`() = runTest {
        val item1 = Item(
            id = "1",
            rackId = "rack1",
            slotId = "slot1",
            name = "Tool",
            description = "Heavy duty",
            createdAt = 10L,
        )
        val item2 = Item(
            id = "2",
            rackId = "rack1",
            slotId = "slot2",
            name = "Heavy Box",
            description = "Storage",
            createdAt = 5L,
        )
        sut.saveItem(item =item1)
        sut.saveItem(item =item2)

        val result = sut.searchItems(query = "heavy")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 2, actual = items.size)
        assertEquals(expected = listOf("2", "1"), actual = items.map { it.id })
    }

    @Test
    fun `GIVEN search hits in name and description WHEN searchItems THEN prioritises name matches`() = runTest {
        val descriptionMatch = Item(
            id = "1",
            rackId = "rack1",
            slotId = "slot1",
            name = "Tool",
            description = "Heavy duty",
            createdAt = 10L,
        )
        val nameMatch = Item(
            id = "2",
            rackId = "rack1",
            slotId = "slot2",
            name = "Heavy Box",
            description = "Storage",
            createdAt = 5L,
        )
        sut.saveItem(item = descriptionMatch)
        sut.saveItem(item = nameMatch)

        val result = sut.searchItems(query = "heavy")

        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = listOf("2", "1"), actual = items.map { it.id })
    }

    @Test
    fun `GIVEN any state WHEN searchItems with blank query THEN returns empty list`() = runTest {

        val result = sut.searchItems(query = "")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = items.size)
    }

    @Test
    fun `GIVEN empty repository WHEN saveItem with valid item THEN creates and returns item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "New Item", description = "Description")

        val result = sut.saveItem(item =item)

        assertTrue(actual = result.isOk)
        val savedItem = result.getOrNull()
        assertEquals(expected = item.id, actual = savedItem?.id)
        assertEquals(expected = item.name, actual = savedItem?.name)
        assertEquals(expected = item.description, actual = savedItem?.description)
        assertEquals(expected = item.rackId, actual = savedItem?.rackId)
        assertEquals(expected = item.slotId, actual = savedItem?.slotId)
    }

    @Test
    fun `GIVEN existing item WHEN saveItem with same id THEN updates and returns item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Original Name")
        sut.saveItem(item =item)
        val updatedItem = Item(
            id = item.id,
            rackId = item.rackId,
            slotId = item.slotId,
            name = "Updated Name",
            description = "New Description",
            photoUri = item.photoUri,
            quantity = item.quantity,
            owner = item.owner,
            tags = item.tags,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )

        val result = sut.saveItem(item =updatedItem)

        assertTrue(actual = result.isOk)
        val savedItem = result.getOrNull()
        assertEquals(expected = "Updated Name", actual = savedItem?.name)
        assertEquals(expected = "New Description", actual = savedItem?.description)
        assertTrue(actual = savedItem?.updatedAt != null)
    }

    @Test
    fun `GIVEN any state WHEN saveItem with blank id THEN returns ValidationError`() = runTest {
        val item = Item(id = "", rackId = "rack1", slotId = "slot1", name = "Invalid Item")

        val result = sut.saveItem(item =item)

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "id", actual = error.field)
    }

    @Test
    fun `GIVEN any state WHEN saveItem with blank rackId THEN returns ValidationError`() = runTest {
        val item = Item(id = "1", rackId = "", slotId = "slot1", name = "Invalid Item")

        val result = sut.saveItem(item =item)

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "rackId", actual = error.field)
    }

    @Test
    fun `GIVEN any state WHEN saveItem with blank slotId THEN returns ValidationError`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "", name = "Invalid Item")

        val result = sut.saveItem(item =item)

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "slotId", actual = error.field)
    }

    @Test
    fun `GIVEN saved item WHEN deleteItem with existing id THEN removes item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "To Delete")
        sut.saveItem(item =item)

        val deleteResult = sut.deleteItem(id = "1")

        assertTrue(actual = deleteResult.isOk)
        val getResult = sut.getItemById(id = "1")
        assertTrue(actual = getResult.isErr)
        val error = getResult.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
    }

    @Test
    fun `GIVEN empty repository WHEN deleteItem with unknown id THEN returns NotFound`() = runTest {

        val result = sut.deleteItem(id = "nonexistent")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
        assertEquals(expected = "Item", actual = error.resource)
    }

    @Test
    fun `GIVEN any state WHEN deleteItem with blank id THEN returns ValidationError`() = runTest {

        val result = sut.deleteItem(id = "")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "id", actual = error.field)
    }

    @Test
    fun `GIVEN multiple saved items WHEN clear THEN getItemsByRack returns empty list`() = runTest {
        sut.saveItem(item =Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Item 1"))
        sut.saveItem(item =Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Item 2"))

        sut.clear()
        val result = sut.getItemsByRack(rackId = "rack1")

        assertTrue(actual = result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = items.size)
    }

    @Test
    fun `GIVEN empty repository WHEN concurrent saves THEN all items are saved correctly`() = runTest {
        val items = (1..100).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }

        items.map { item ->
            async { sut.saveItem(item =item) }
        }.awaitAll()

        val result = sut.getItemsByRack(rackId = "rack1")
        assertTrue(actual = result.isOk)
        val savedItems: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 100, actual = savedItems.size)
        val savedIds = savedItems.map { it.id }.toSet()
        val expectedIds = items.map { it.id }.toSet()
        assertEquals(expected = expectedIds, actual = savedIds)
    }

    @Test
    fun `GIVEN saved items WHEN concurrent reads THEN all reads succeed without race conditions`() = runTest {
        val items = (1..50).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }
        items.forEach { sut.saveItem(item =it) }

        val readResults = (1..50).map { i ->
            async { sut.getItemById(id = "item$i") }
        }.awaitAll()

        assertEquals(expected = 50, actual = readResults.size)
        readResults.forEach { result ->
            assertTrue(actual = result.isOk)
            assertTrue(actual = result.getOrNull() != null)
        }
    }

    @Test
    fun `GIVEN saved items WHEN concurrent writes and reads THEN operations complete without race conditions`() =
        runTest {
            val initialItems = (1..20).map { i ->
                Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
            }
            initialItems.forEach { sut.saveItem(item =it) }

            val writeJobs = (21..40).map { i ->
                async {
                    sut.saveItem(item =
                        Item(
                            id = "item$i",
                            rackId = "rack1",
                            slotId = "slot1",
                            name = "Item $i"
                        )
                    )
                }
            }
            val readJobs = (1..20).map { i ->
                async { sut.getItemById(id = "item$i") }
            }

            writeJobs.awaitAll()
            val readResults = readJobs.awaitAll()

            assertEquals(expected = 20, actual = readResults.size)
            readResults.forEach { result ->
                assertTrue(actual = result.isOk)
            }
            val allItemsResult = sut.getItemsByRack(rackId = "rack1")
            assertTrue(actual = allItemsResult.isOk)
            val allItems: List<Item> = allItemsResult.getOrNull() ?: emptyList()
            assertEquals(expected = 40, actual = allItems.size)
        }

    @Test
    fun `GIVEN saved items WHEN concurrent deletes THEN items are deleted correctly`() = runTest {
        val items = (1..50).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }
        items.forEach { sut.saveItem(item =it) }

        val deleteJobs = (1..50).map { i ->
            async { sut.deleteItem(id = "item$i") }
        }.awaitAll()

        assertEquals(expected = 50, actual = deleteJobs.size)
        deleteJobs.forEach { result ->
            assertTrue(actual = result.isOk)
        }
        val result = sut.getItemsByRack(rackId = "rack1")
        assertTrue(actual = result.isOk)
        val remainingItems: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = remainingItems.size)
    }

    @Test
    fun `GIVEN saved items WHEN concurrent mixed operations THEN no race conditions occur`() = runTest {
        val initialItems = (1..30).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }
        initialItems.forEach { sut.saveItem(item =it) }

        val saveJobs = (31..50).map { i ->
            async {
                sut.saveItem(item =Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i"))
            }
        }
        val readJobs = (1..30).map { i ->
            async { sut.getItemById(id = "item$i") }
        }
        val deleteJobs = (1..10).map { i ->
            async { sut.deleteItem(id = "item$i") }
        }
        val searchJobs = (1..5).map {
            async { sut.searchItems(query = "Item") }
        }

        saveJobs.awaitAll()
        readJobs.awaitAll()
        deleteJobs.awaitAll()
        searchJobs.awaitAll()

        val result = sut.getItemsByRack(rackId = "rack1")
        assertTrue(actual = result.isOk)
        val allItems: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 40, actual = allItems.size)
        (1..10).forEach { i ->
            val getResult = sut.getItemById(id = "item$i")
            assertTrue(actual = getResult.isErr)
        }
    }
}
