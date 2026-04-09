package org.deafsapps.storeit.data.datasource

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item

internal class FirebaseItemDataSource : ItemDataSource {
    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> = emptyList<Item>().ok()

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> =
        emptyList<Item>().ok()

    override suspend fun getItemById(id: String): Result<DomainError, Item?> = null.ok()

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> = emptyList<Item>().ok()

    override suspend fun saveItem(item: Item): Result<DomainError, Item> = item.ok()

    override suspend fun deleteItem(id: String): Result<DomainError, Boolean> = false.ok()

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clear() = Unit
}
