package org.deafsapps.storeit.domain.repository

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack

/**
 * Defines rack persistence and observation operations for the domain layer.
 */
interface RackRepository {
    /**
     * Observes the current set of racks.
     */
    fun getAllRacksFlow(): Flow<Result<DomainError, List<Rack>>>

    /**
     * Returns a single rack by identifier.
     */
    suspend fun getRackById(id: String): Result<DomainError, Rack>

    /**
     * Creates or updates a rack.
     */
    suspend fun saveRack(rack: Rack): Result<DomainError, Rack>

    /**
     * Deletes a rack by identifier.
     */
    suspend fun deleteRack(id: String): Result<DomainError, Unit>

    /**
     * Removes all stored racks.
     */
    suspend fun clear()
}
