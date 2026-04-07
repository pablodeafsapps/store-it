package org.deafsapps.storeit.data.datasource

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.database.StoreItDatabaseProvider
import org.deafsapps.storeit.data.database.decodeTags
import org.deafsapps.storeit.data.database.encodeTags
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.koin.core.annotation.Single

@Single(binds = [ItemDataSource::class])
internal class SqlDelightItemDataSource(
    private val databaseProvider: StoreItDatabaseProvider,
) : ItemDataSource {

    override suspend fun getItemsByRack(rackId: String): Result<DomainError, List<Item>> = try {
        databaseProvider.database.storeItDatabaseQueries
            .selectItemsByRack(
                rack_id = rackId,
                mapper = ::mapItem,
            )
            .executeAsList()
            .ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun getItemsBySlot(rackId: String, slotId: String): Result<DomainError, List<Item>> = try {
        databaseProvider.database.storeItDatabaseQueries
            .selectItemsBySlot(
                rack_id = rackId,
                slot_id = slotId,
                mapper = ::mapItem,
            )
            .executeAsList()
            .ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun getItemById(id: String): Result<DomainError, Item?> = try {
        databaseProvider.database.storeItDatabaseQueries
            .selectItemById(id = id, mapper = ::mapItem)
            .executeAsOneOrNull()
            .ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun searchItems(query: String): Result<DomainError, List<Item>> = try {
        databaseProvider.database.storeItDatabaseQueries
            .searchItemsByQuery(
                query,
                query,
                ::mapItem,
            )
            .executeAsList()
            .ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun saveItem(item: Item): Result<DomainError, Item> = try {
        databaseProvider.database.storeItDatabaseQueries.upsertItem(
            id = item.id,
            rack_id = item.rackId,
            slot_id = item.slotId,
            name = item.name,
            description = item.description,
            photo_uri = item.photoUri,
            quantity = item.quantity?.toLong(),
            owner = item.owner,
            tags_json = encodeTags(item.tags),
            created_at = item.createdAt,
            updated_at = item.updatedAt,
        )
        item.ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun deleteItem(id: String): Result<DomainError, Boolean> = try {
        val deleted = databaseProvider.database.storeItDatabaseQueries.deleteItemById(id = id).value > 0L
        deleted.ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun deleteItemsByRack(rackId: String): Result<DomainError, Unit> = try {
        databaseProvider.database.storeItDatabaseQueries.deleteItemsByRack(rack_id = rackId)
        Unit.ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun clear() {
        databaseProvider.database.storeItDatabaseQueries.deleteAllItems()
    }

    private fun mapItem(
        id: String,
        rackId: String,
        slotId: String,
        name: String,
        description: String,
        photoUri: String?,
        quantity: Long?,
        owner: String,
        tagsJson: String,
        createdAt: Long,
        updatedAt: Long?,
    ): Item = Item(
        id = id,
        rackId = rackId,
        slotId = slotId,
        name = name,
        description = description,
        photoUri = photoUri,
        quantity = quantity?.toInt(),
        owner = owner,
        tags = decodeTags(tagsJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
