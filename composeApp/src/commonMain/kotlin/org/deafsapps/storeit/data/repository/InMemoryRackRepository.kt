package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import kotlin.time.Clock

internal class InMemoryRackRepository : RackRepository {
    private val racks = mutableMapOf<String, Rack>()

    override suspend fun getAllRacks() =
        racks.values.toList().ok()

    override suspend fun getRackById(id: String) =
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            else -> racks[id]?.ok() ?: DomainError.NotFound(resource = "Rack", id = id).err()
        }

    override suspend fun saveRack(rack: Rack) =
        when {
            rack.id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            else -> {
                val rackToSave = if (racks.containsKey(rack.id)) {
                    rack.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
                } else {
                    rack.copy(
                        createdAt = Clock.System.now().toEpochMilliseconds(),
                        updatedAt = null
                    )
                }
                racks[rackToSave.id] = rackToSave
                rackToSave.ok()
            }
        }

    override suspend fun deleteRack(id: String) =
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            racks.remove(id) != null -> Unit.ok()
            else -> DomainError.NotFound(resource = "Rack", id = id).err()
        }

    fun clear() = racks.clear()
}
