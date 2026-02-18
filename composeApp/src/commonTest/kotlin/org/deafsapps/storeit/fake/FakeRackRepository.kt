package org.deafsapps.storeit.fake

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

    override suspend fun getAllRacks(): Result<DomainError, List<Rack>> =
        getAllRacksResult ?: racks.values.toList().ok()

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
}
