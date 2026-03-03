package org.deafsapps.storeit.domain.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item

internal interface ItemRepository {
    suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>>
    suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>>
    suspend fun getItemById(id: String): Result<DomainError, Item>
    suspend fun searchItems(query: String): Result<DomainError, List<Item>>
    suspend fun saveItem(item: Item): Result<DomainError, Item>
    suspend fun deleteItem(id: String): Result<DomainError, Unit>
    suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit>
}
