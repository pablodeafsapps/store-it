package org.deafsapps.storeit.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.repository.ItemRepository

internal class FakeItemRepository : ItemRepository {
    private val items = mutableMapOf<String, Item>()

    var getItemsByRackResult: Result<DomainError, List<Item>>? = null
    var getItemsBySlotResult: Result<DomainError, List<Item>>? = null
    var getItemByIdResult: Result<DomainError, Item>? = null
    var searchItemsResult: Result<DomainError, List<Item>>? = null
    var saveItemResult: Result<DomainError, Item>? = null
    var deleteItemResult: Result<DomainError, Unit>? = null

    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> =
        getItemsByRackResult ?: items.values.filter { it.rackId == rackId }.ok()

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> =
        getItemsBySlotResult ?: items.values.filter { it.rackId == rackId && it.slotId == slotId }.ok()

    override suspend fun getItemById(id: String): Result<DomainError, Item> =
        getItemByIdResult ?: (items[id]?.ok() ?: DomainError.NotFound(resource = "Item", id = id).err())

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> =
        searchItemsResult ?: if (query.isBlank()) {
            emptyList<Item>().ok()
        } else {
            val lowerQuery = query.lowercase()
            items.values.filter { item ->
                item.name.lowercase().contains(lowerQuery) ||
                    item.description.lowercase().contains(lowerQuery)
            }.ok()
        }

    override suspend fun saveItem(item: Item): Result<DomainError, Item> =
        saveItemResult ?: run {
            items[item.id] = item
            item.ok()
        }

    override suspend fun deleteItem(id: String): Result<DomainError, Unit> =
        deleteItemResult ?: run {
            items.remove(id)
            Unit.ok()
        }

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit> = run {
        items.keys.toList().filter { items[it]?.rackId == rackId }.forEach { items.remove(it) }
        Unit.ok()
    }
}
