package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.deafsapps.storeit.base.Result

@Single(binds = [RackRepository::class])
internal class InMemoryRackRepository : RackRepository {
    private val racks = MutableStateFlow<Map<String, Rack>>(emptyMap())
    private val mutex = Mutex()

    override fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>> =
        racks.map { m -> m.values.toList().ok() }

    override suspend fun getRackById(id: String): Result<DomainError, Rack> = mutex.withLock {
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            else -> racks.value[id]?.ok() ?: DomainError.NotFound(resource = "Rack", id = id).err()
        }
    }

    override suspend fun saveRack(rack: Rack): Result<DomainError, Rack> = mutex.withLock {
        when {
            rack.id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            else -> {
                val rackToSave = if (racks.value.containsKey(rack.id)) {
                    Rack(
                        id = rack.id,
                        name = rack.name,
                        description = rack.description,
                        location = rack.location,
                        photoUri = rack.photoUri,
                        createdAt = rack.createdAt,
                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                } else {
                    Rack(
                        id = rack.id,
                        name = rack.name,
                        description = rack.description,
                        location = rack.location,
                        photoUri = rack.photoUri,
                        createdAt = Clock.System.now().toEpochMilliseconds(),
                        updatedAt = null,
                    )
                }
                rackToSave.ok().also { racks.updateAndEmit(rackToUpdate = rackToSave) }
            }
        }
    }

    override suspend fun deleteRack(id: String): Result<DomainError, Unit> = mutex.withLock {
        when {
            id.isBlank() -> DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()
            racks.value.containsKey(id) -> Unit.ok().also{ racks.deleteAndEmit(rackId = id) }
            else -> DomainError.NotFound(resource = "Rack", id = id).err()
        }
    }

    override suspend fun clear() = mutex.withLock {
        racks.emit(emptyMap())
    }
}

private suspend fun MutableStateFlow<Map<String, Rack>>.updateAndEmit(rackToUpdate: Rack) {
    val updatedMap = value.toMutableMap()
    updatedMap[rackToUpdate.id] = rackToUpdate
    emit(updatedMap)
}

private suspend fun MutableStateFlow<Map<String, Rack>>.deleteAndEmit(rackId: String) {
    val updatedMap = value.toMutableMap()
    updatedMap.remove(rackId)
    emit(updatedMap)
}
