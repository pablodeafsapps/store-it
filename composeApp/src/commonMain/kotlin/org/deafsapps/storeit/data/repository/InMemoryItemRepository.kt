package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Single(binds = [ItemRepository::class])
internal class InMemoryItemRepository : ItemRepository {
    private val items = mutableMapOf<String, Item>()
    private val mutex = Mutex()

    override suspend fun getItemsByRack(rackId: String) = mutex.withLock {
        when {
            rackId.isBlank() -> DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            else -> items.values.filter { it.rackId == rackId }.ok()
        }
    }

    override suspend fun getItemsBySlot(rackId: String, slotId: String) = mutex.withLock {
        when {
            rackId.isBlank() -> DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            slotId.isBlank() -> DomainError.ValidationError(field = "slotId", reason = "Slot ID cannot be blank").err()
            else -> items.values.filter { it.rackId == rackId && it.slotId == slotId }.ok()
        }
    }

    override suspend fun getItemById(id: String) = mutex.withLock {
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            else -> items[id]?.ok() ?: DomainError.NotFound(resource = "Item", id = id).err()
        }
    }

    override suspend fun searchItems(query: String) = mutex.withLock {
        when {
            query.isBlank() -> emptyList<Item>().ok()
            else -> {
                val lowerQuery = query.lowercase()
                items.values.filter { item ->
                    item.name.lowercase().contains(lowerQuery) ||
                        item.description.lowercase().contains(lowerQuery)
                }.ok()
            }
        }
    }

    override suspend fun saveItem(item: Item) = mutex.withLock {
        when {
            item.id.isBlank() ->
                DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            item.rackId.isBlank() ->
                DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            item.slotId.isBlank() ->
                DomainError.ValidationError(field = "slotId", reason = "Slot ID cannot be blank").err()
            else -> {
                val itemToSave = if (items.containsKey(item.id)) {
                    item.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
                } else {
                    item.copy(
                        createdAt = Clock.System.now().toEpochMilliseconds(),
                        updatedAt = null
                    )
                }
                items[itemToSave.id] = itemToSave
                itemToSave.ok()
            }
        }
    }

    override suspend fun deleteItem(id: String) = mutex.withLock {
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            items.remove(id) != null -> Unit.ok()
            else -> DomainError.NotFound(resource = "Item", id = id).err()
        }
    }

    override suspend fun deleteItemsByRack(rackId: String) = mutex.withLock {
        when {
            rackId.isBlank() -> DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
            else -> {
                Unit.ok().also {
                    items.keys.toList().filter { items[it]?.rackId == rackId }.forEach { items.remove(it) }
                }
            }
        }
    }

    suspend fun clear() = mutex.withLock {
        items.clear()
    }
}
