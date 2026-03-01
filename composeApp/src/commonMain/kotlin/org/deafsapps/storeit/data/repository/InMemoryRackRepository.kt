package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Single(binds = [RackRepository::class])
internal class InMemoryRackRepository : RackRepository {
    private val racks = mutableMapOf<String, Rack>()
    private val mutex = Mutex()

    override suspend fun getAllRacks() = mutex.withLock {
        racks.values.toList().ok()
    }

    override suspend fun getRackById(id: String) = mutex.withLock {
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            else -> racks[id]?.ok() ?: DomainError.NotFound(resource = "Rack", id = id).err()
        }
    }

    override suspend fun saveRack(rack: Rack) = mutex.withLock {
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
    }

    override suspend fun deleteRack(id: String) = mutex.withLock {
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            racks.remove(id) != null -> Unit.ok()
            else -> DomainError.NotFound(resource = "Rack", id = id).err()
        }
    }

    suspend fun clear() = mutex.withLock {
        racks.clear()
    }
}
