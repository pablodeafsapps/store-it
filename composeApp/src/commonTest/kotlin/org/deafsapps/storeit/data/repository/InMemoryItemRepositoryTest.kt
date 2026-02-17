package org.deafsapps.storeit.data.repository

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

class InMemoryItemRepositoryTest {
    private val repository = InMemoryItemRepository()

    @Test
    fun `GIVEN empty repository WHEN getItemsByRack THEN returns empty list`() = runTest {

        val result = repository.getItemsByRack("rack1")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(0, items.size)
    }

    @Test
    fun `GIVEN items in different racks WHEN getItemsByRack THEN returns only items for specified rack`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Item 1")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Item 2")
        val item3 = Item(id = "3", rackId = "rack2", slotId = "slot1", name = "Item 3")
        repository.saveItem(item1)
        repository.saveItem(item2)
        repository.saveItem(item3)

        val result = repository.getItemsByRack("rack1")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(2, items.size)
        assertTrue(items.contains(item1))
        assertTrue(items.contains(item2))
        assertTrue(!items.contains(item3))
    }

    @Test
    fun `GIVEN any state WHEN getItemsByRack with blank rackId THEN returns ValidationError`() = runTest {

        val result = repository.getItemsByRack("")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("rackId", error.field)
    }

    @Test
    fun `GIVEN items in different slots WHEN getItemsBySlot THEN returns only items for specified slot`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Item 1")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot1", name = "Item 2")
        val item3 = Item(id = "3", rackId = "rack1", slotId = "slot2", name = "Item 3")
        repository.saveItem(item1)
        repository.saveItem(item2)
        repository.saveItem(item3)

        val result = repository.getItemsBySlot("rack1", "slot1")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(2, items.size)
        assertTrue(items.contains(item1))
        assertTrue(items.contains(item2))
        assertTrue(!items.contains(item3))
    }

    @Test
    fun `GIVEN empty repository WHEN getItemsBySlot THEN returns empty list`() = runTest {

        val result = repository.getItemsBySlot("rack1", "slot1")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(0, items.size)
    }

    @Test
    fun `GIVEN any state WHEN getItemsBySlot with blank rackId THEN returns ValidationError`() = runTest {

        val result = repository.getItemsBySlot("", "slot1")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("rackId", error.field)
    }

    @Test
    fun `GIVEN any state WHEN getItemsBySlot with blank slotId THEN returns ValidationError`() = runTest {

        val result = repository.getItemsBySlot("rack1", "")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("slotId", error.field)
    }

    @Test
    fun `GIVEN saved item WHEN getItemById with existing id THEN returns item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Test Item")
        repository.saveItem(item)

        val result = repository.getItemById("1")

        assertTrue(result.isOk)
        val retrievedItem = result.getOrNull()
        assertEquals(item, retrievedItem)
    }

    @Test
    fun `GIVEN empty repository WHEN getItemById with unknown id THEN returns NotFound`() = runTest {

        val result = repository.getItemById("nonexistent")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.NotFound)
        assertEquals("Item", error.resource)
        assertEquals("nonexistent", error.id)
    }

    @Test
    fun `GIVEN any state WHEN getItemById with blank id THEN returns ValidationError`() = runTest {

        val result = repository.getItemById("")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("id", error.field)
    }

    @Test
    fun `GIVEN items with different names WHEN searchItems by name THEN returns matching items`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Hammer")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Screwdriver")
        val item3 = Item(id = "3", rackId = "rack2", slotId = "slot1", name = "Hammer Set")
        repository.saveItem(item1)
        repository.saveItem(item2)
        repository.saveItem(item3)

        val result = repository.searchItems("hammer")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(2, items.size)
        assertTrue(items.contains(item1))
        assertTrue(items.contains(item3))
        assertTrue(!items.contains(item2))
    }

    @Test
    fun `GIVEN items with descriptions WHEN searchItems by description THEN returns matching items`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Tool", description = "Heavy duty")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Tool", description = "Lightweight")
        repository.saveItem(item1)
        repository.saveItem(item2)

        val result = repository.searchItems("heavy")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(1, items.size)
        assertTrue(items.contains(item1))
        assertTrue(!items.contains(item2))
    }

    @Test
    fun `GIVEN items WHEN searchItems matches both name and description THEN returns all matching items`() = runTest {
        val item1 = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Tool", description = "Heavy duty")
        val item2 = Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Heavy Box", description = "Storage")
        repository.saveItem(item1)
        repository.saveItem(item2)

        val result = repository.searchItems("heavy")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(2, items.size)
        assertTrue(items.contains(item1))
        assertTrue(items.contains(item2))
    }

    @Test
    fun `GIVEN any state WHEN searchItems with blank query THEN returns empty list`() = runTest {

        val result = repository.searchItems("")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(0, items.size)
    }

    @Test
    fun `GIVEN empty repository WHEN saveItem with valid item THEN creates and returns item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "New Item", description = "Description")

        val result = repository.saveItem(item)

        assertTrue(result.isOk)
        val savedItem = result.getOrNull()
        assertEquals(item.id, savedItem?.id)
        assertEquals(item.name, savedItem?.name)
        assertEquals(item.description, savedItem?.description)
        assertEquals(item.rackId, savedItem?.rackId)
        assertEquals(item.slotId, savedItem?.slotId)
    }

    @Test
    fun `GIVEN existing item WHEN saveItem with same id THEN updates and returns item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Original Name")
        repository.saveItem(item)
        val updatedItem = item.copy(name = "Updated Name", description = "New Description")

        val result = repository.saveItem(updatedItem)

        assertTrue(result.isOk)
        val savedItem = result.getOrNull()
        assertEquals("Updated Name", savedItem?.name)
        assertEquals("New Description", savedItem?.description)
        assertTrue(savedItem?.updatedAt != null)
    }

    @Test
    fun `GIVEN any state WHEN saveItem with blank id THEN returns ValidationError`() = runTest {
        val item = Item(id = "", rackId = "rack1", slotId = "slot1", name = "Invalid Item")

        val result = repository.saveItem(item)

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("id", error.field)
    }

    @Test
    fun `GIVEN any state WHEN saveItem with blank rackId THEN returns ValidationError`() = runTest {
        val item = Item(id = "1", rackId = "", slotId = "slot1", name = "Invalid Item")

        val result = repository.saveItem(item)

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("rackId", error.field)
    }

    @Test
    fun `GIVEN any state WHEN saveItem with blank slotId THEN returns ValidationError`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "", name = "Invalid Item")

        val result = repository.saveItem(item)

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("slotId", error.field)
    }

    @Test
    fun `GIVEN saved item WHEN deleteItem with existing id THEN removes item`() = runTest {
        val item = Item(id = "1", rackId = "rack1", slotId = "slot1", name = "To Delete")
        repository.saveItem(item)

        val deleteResult = repository.deleteItem("1")

        assertTrue(deleteResult.isOk)
        val getResult = repository.getItemById("1")
        assertTrue(getResult.isErr)
        val error = getResult.failureOrNull()
        assertTrue(error is DomainError.NotFound)
    }

    @Test
    fun `GIVEN empty repository WHEN deleteItem with unknown id THEN returns NotFound`() = runTest {

        val result = repository.deleteItem("nonexistent")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.NotFound)
        assertEquals("Item", error.resource)
    }

    @Test
    fun `GIVEN any state WHEN deleteItem with blank id THEN returns ValidationError`() = runTest {

        val result = repository.deleteItem("")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("id", error.field)
    }

    @Test
    fun `GIVEN multiple saved items WHEN clear THEN getAllRacks returns empty list`() = runTest {
        repository.saveItem(Item(id = "1", rackId = "rack1", slotId = "slot1", name = "Item 1"))
        repository.saveItem(Item(id = "2", rackId = "rack1", slotId = "slot2", name = "Item 2"))

        repository.clear()
        val result = repository.getItemsByRack("rack1")

        assertTrue(result.isOk)
        val items: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(0, items.size)
    }

    @Test
    fun `GIVEN empty repository WHEN concurrent saves THEN all items are saved correctly`() = runTest {
        val items = (1..100).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }

        items.map { item ->
            async { repository.saveItem(item) }
        }.awaitAll()

        val result = repository.getItemsByRack("rack1")
        assertTrue(result.isOk)
        val savedItems: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(100, savedItems.size)
        val savedIds = savedItems.map { it.id }.toSet()
        val expectedIds = items.map { it.id }.toSet()
        assertEquals(expectedIds, savedIds)
    }

    @Test
    fun `GIVEN saved items WHEN concurrent reads THEN all reads succeed without race conditions`() = runTest {
        val items = (1..50).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }
        items.forEach { repository.saveItem(it) }

        val readResults = (1..50).map { i ->
            async { repository.getItemById("item$i") }
        }.awaitAll()

        assertEquals(50, readResults.size)
        readResults.forEach { result ->
            assertTrue(result.isOk)
            assertTrue(result.getOrNull() != null)
        }
    }

    @Test
    fun `GIVEN saved items WHEN concurrent writes and reads THEN operations complete without race conditions`() =
        runTest {
            val initialItems = (1..20).map { i ->
                Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
            }
            initialItems.forEach { repository.saveItem(it) }

            val writeJobs = (21..40).map { i ->
                async {
                    repository.saveItem(
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
                async { repository.getItemById("item$i") }
            }

            writeJobs.awaitAll()
            val readResults = readJobs.awaitAll()

            assertEquals(20, readResults.size)
            readResults.forEach { result ->
                assertTrue(result.isOk)
            }
            val allItemsResult = repository.getItemsByRack("rack1")
            assertTrue(allItemsResult.isOk)
            val allItems: List<Item> = allItemsResult.getOrNull() ?: emptyList()
            assertEquals(40, allItems.size)
        }

    @Test
    fun `GIVEN saved items WHEN concurrent deletes THEN items are deleted correctly`() = runTest {
        val items = (1..50).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }
        items.forEach { repository.saveItem(it) }

        val deleteJobs = (1..50).map { i ->
            async { repository.deleteItem("item$i") }
        }.awaitAll()

        assertEquals(50, deleteJobs.size)
        deleteJobs.forEach { result ->
            assertTrue(result.isOk)
        }
        val result = repository.getItemsByRack("rack1")
        assertTrue(result.isOk)
        val remainingItems: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(0, remainingItems.size)
    }

    @Test
    fun `GIVEN saved items WHEN concurrent mixed operations THEN no race conditions occur`() = runTest {
        val initialItems = (1..30).map { i ->
            Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i")
        }
        initialItems.forEach { repository.saveItem(it) }

        val saveJobs = (31..50).map { i ->
            async {
                repository.saveItem(Item(id = "item$i", rackId = "rack1", slotId = "slot1", name = "Item $i"))
            }
        }
        val readJobs = (1..30).map { i ->
            async { repository.getItemById("item$i") }
        }
        val deleteJobs = (1..10).map { i ->
            async { repository.deleteItem("item$i") }
        }
        val searchJobs = (1..5).map {
            async { repository.searchItems("Item") }
        }

        saveJobs.awaitAll()
        readJobs.awaitAll()
        deleteJobs.awaitAll()
        searchJobs.awaitAll()

        val result = repository.getItemsByRack("rack1")
        assertTrue(result.isOk)
        val allItems: List<Item> = result.getOrNull() ?: emptyList()
        assertEquals(40, allItems.size)
        (1..10).forEach { i ->
            val getResult = repository.getItemById("item$i")
            assertTrue(getResult.isErr)
        }
    }
}
