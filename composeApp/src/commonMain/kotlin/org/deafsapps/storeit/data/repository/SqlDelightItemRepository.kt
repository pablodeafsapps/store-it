package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.ItemDataSource
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.koin.core.annotation.Single

@Single(binds = [ItemRepository::class])
internal class SqlDelightItemRepository(
    private val itemDataSource: ItemDataSource,
) : ItemRepository {

    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> =
        if (rackId.isBlank()) {
            DomainError.ValidationError(
                field = "rackId",
                reason = "Rack ID cannot be blank",
            ).err()
        } else {
            itemDataSource.getItemsByRack(rackId = rackId)
        }

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> {
        if (rackId.isBlank()) return DomainError.ValidationError(
            field = "rackId",
            reason = "Rack ID cannot be blank",
        ).err()
        if (slotId.isBlank()) return DomainError.ValidationError(
            field = "slotId",
            reason = "Slot ID cannot be blank",
        ).err()
        return itemDataSource.getItemsBySlot(rackId = rackId, slotId = slotId)
    }

    override suspend fun getItemById(id: String): Result<DomainError, Item> =
        if (id.isBlank()) {
            DomainError.ValidationError(
                field = "id",
                reason = "ID cannot be blank",
            ).err()
        } else {
            itemDataSource.getItemById(id = id).flatMap { item ->
                item?.ok() ?: DomainError.NotFound(resource = "Item", id = id).err()
            }
        }

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> =
        if (query.isBlank()) {
            emptyList<Item>().ok()
        } else {
            itemDataSource.searchItems(query = query)
        }

    override suspend fun saveItem(item: Item): Result<DomainError, Item> {
        if (item.id.isBlank()) return DomainError.ValidationError(
            field = "id",
            reason = "ID cannot be blank",
        ).err()
        if (item.rackId.isBlank()) return DomainError.ValidationError(
            field = "rackId",
            reason = "Rack ID cannot be blank",
        ).err()
        if (item.slotId.isBlank()) return DomainError.ValidationError(
            field = "slotId",
            reason = "Slot ID cannot be blank",
        ).err()
        return itemDataSource.saveItem(item = item)
    }

    override suspend fun deleteItem(id: String): Result<DomainError, Unit> =
        if (id.isBlank()) {
            DomainError.ValidationError(
                field = "id",
                reason = "ID cannot be blank",
            ).err()
        } else {
            itemDataSource.deleteItem(id = id).flatMap { deleted ->
                if (deleted) {
                    Unit.ok()
                } else {
                    DomainError.NotFound(
                        resource = "Item",
                        id = id,
                    ).err()
                }
            }
        }

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit> =
        if (rackId.isBlank()) {
            DomainError.ValidationError(field = "rackId", reason = "Rack ID cannot be blank").err()
        } else {
            itemDataSource.deleteItemsByRack(rackId = rackId)
        }

    override suspend fun clear() {
        itemDataSource.clear()
    }
}
