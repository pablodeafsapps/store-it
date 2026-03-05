package org.deafsapps.storeit.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository

internal class FakeRackRepository : RackRepository {
    private val racks = mutableMapOf<String, Rack>()

    var getAllRacksResult: Result<DomainError, List<Rack>>? = null
    var getRackByIdResult: Result<DomainError, Rack>? = null
    var saveRackResult: Result<DomainError, Rack>? = null
    var deleteRackResult: Result<DomainError, Unit>? = null

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> =
        flow { emit(getAllRacksResult ?: racks.values.toList().ok()) }

    override suspend fun getRackById(id: String): Result<DomainError, Rack> =
        getRackByIdResult ?: (racks[id]?.ok() ?: DomainError.NotFound(resource = "Rack", id = id).err())

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> =
        saveRackResult ?: run {
            racks[rack.id] = rack
            rack.ok()
        }

    override suspend fun deleteRack(id: String): Result<DomainError, Unit> =
        deleteRackResult ?: run {
            racks.remove(id)
            Unit.ok()
        }

    override suspend fun clear() {
        racks.clear()
    }
}
