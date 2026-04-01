package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.RackDataSource
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import org.koin.core.annotation.Single

@Single(binds = [RackRepository::class])
internal class SqlDelightRackRepository(
    private val rackDataSource: RackDataSource,
) : RackRepository {

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> =
        rackDataSource.getAllRacksFlow()

    override suspend fun getRackById(id: String): Result<DomainError, Rack> =
        if (id.isBlank()) {
            DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
        } else {
            rackDataSource.getRackById(id = id).flatMap { rack ->
                rack?.ok() ?: DomainError.NotFound(resource = "Rack", id = id).err()
            }
        }

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> =
        if (rack.id.isBlank()) {
            DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
        } else {
            rackDataSource.saveRack(rack = rack)
        }

    override suspend fun deleteRack(id: String): Result<DomainError, Unit> =
        if (id.isBlank()) {
            DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
        } else {
            rackDataSource.deleteRack(id = id).flatMap { deleted ->
                if (deleted) {
                    Unit.ok()
                } else {
                    DomainError.NotFound(resource = "Rack", id = id).err()
                }
            }
        }

    override suspend fun clear() {
        rackDataSource.clear()
    }
}
