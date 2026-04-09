package org.deafsapps.storeit.domain.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item

/**
 * Defines item persistence and lookup operations for the domain layer.
 */
interface ItemRepository {
    /**
     * Returns every item assigned to the given rack.
     */
    suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>>

    /**
     * Returns every item assigned to the given slot in the given rack.
     */
    suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>>

    /**
     * Returns a single item by identifier.
     */
    suspend fun getItemById(id: String): Result<DomainError, Item>

    /**
     * Searches items by user-facing text such as name and description.
     */
    suspend fun searchItems(query: String): Result<DomainError, List<Item>>

    /**
     * Creates or updates an item.
     */
    suspend fun saveItem(item: Item): Result<DomainError, Item>

    /**
     * Deletes a single item by identifier.
     */
    suspend fun deleteItem(id: String): Result<DomainError, Unit>

    /**
     * Deletes every item assigned to the given rack.
     */
    suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit>

    /**
     * Removes all stored items.
     */
    suspend fun clear()
}
