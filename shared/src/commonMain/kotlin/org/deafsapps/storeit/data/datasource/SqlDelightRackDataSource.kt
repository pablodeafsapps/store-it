package org.deafsapps.storeit.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.database.StoreItDatabaseException
import org.deafsapps.storeit.data.database.StoreItDatabaseProvider
import org.deafsapps.storeit.data.database.toStoreItDatabaseDomainErrorOrThrow
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.toUnknownDomainError
import org.koin.core.annotation.Single

@Single(binds = [RackDataSource::class])
internal class SqlDelightRackDataSource(
    private val databaseProvider: StoreItDatabaseProvider,
) : RackDataSource {

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> =
        databaseProvider.database.storeItDatabaseQueries
            .selectAllRacks(
                mapper = { id, name, description, location, photoUri, createdAt, updatedAt ->
                    Rack(
                        id = id,
                        name = name,
                        description = description,
                        location = location,
                        photoUri = photoUri,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    )
                },
            )
            .asFlow()
            .mapToList(context = Dispatchers.IO)
            .map { racks -> Result.ok(racks) as Result<DomainError, List<Rack>> }
            .catch { throwable -> emit(throwable.toStoreItDatabaseDomainErrorOrThrow().err()) }

    override suspend fun getRackById(id: String): Result<DomainError, Rack?> = try {
        databaseProvider.database.storeItDatabaseQueries
            .selectRackById(
                id = id,
                mapper = { rowId, name, description, location, photoUri, createdAt, updatedAt ->
                    Rack(
                        id = rowId,
                        name = name,
                        description = description,
                        location = location,
                        photoUri = photoUri,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    )
                },
            )
            .executeAsOneOrNull()
            .ok()
    } catch (exception: StoreItDatabaseException) {
        exception.toUnknownDomainError().err()
    }

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> = try {
        databaseProvider.database.storeItDatabaseQueries.upsertRack(
            id = rack.id,
            name = rack.name,
            description = rack.description,
            location = rack.location,
            photo_uri = rack.photoUri,
            created_at = rack.createdAt,
            updated_at = rack.updatedAt,
        )

        databaseProvider.database.storeItDatabaseQueries
            .selectRackById(
                id = rack.id,
                mapper = { rowId, name, description, location, photoUri, createdAt, updatedAt ->
                    Rack(
                        id = rowId,
                        name = name,
                        description = description,
                        location = location,
                        photoUri = photoUri,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    )
                },
            )
            .executeAsOneOrNull()
            ?.ok()
            ?: DomainError.Unknown(
                message = "Rack '${rack.id}' could not be reloaded after save",
            ).err()
    } catch (exception: StoreItDatabaseException) {
        exception.toUnknownDomainError().err()
    }

    override suspend fun deleteRack(id: String): Result<DomainError, Boolean> = try {
        val deleted = databaseProvider.database.storeItDatabaseQueries.deleteRackById(id = id).value > 0L
        deleted.ok()
    } catch (exception: StoreItDatabaseException) {
        exception.toUnknownDomainError().err()
    }

    override suspend fun clear() {
        databaseProvider.database.storeItDatabaseQueries.deleteAllRacks()
    }
}
