package org.deafsapps.storeit.data.datasource

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack

internal interface RackDataSource {
    fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>>

    suspend fun getRackById(id: String): Result<DomainError, Rack?>

    suspend fun saveRack(rack: Rack): Result<DomainError, Rack>

    suspend fun deleteRack(id: String): Result<DomainError, Boolean>

    suspend fun clear()
}
