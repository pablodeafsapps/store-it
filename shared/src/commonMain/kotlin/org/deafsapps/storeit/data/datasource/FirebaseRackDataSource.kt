package org.deafsapps.storeit.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack

internal class FirebaseRackDataSource : RackDataSource {
    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> =
        flowOf(emptyList<Rack>().ok())

    override suspend fun getRackById(id: String): Result<DomainError, Rack?> = null.ok()

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> = rack.ok()

    override suspend fun deleteRack(id: String): Result<DomainError, Boolean> = false.ok()

    override suspend fun clear() = Unit
}
