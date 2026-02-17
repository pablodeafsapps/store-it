package org.deafsapps.storeit.domain.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack

internal interface RackRepository {
  suspend fun getAllRacks(): Result<DomainError, List<Rack>>
  suspend fun getRackById(id: String): Result<DomainError, Rack>
  suspend fun saveRack(rack: Rack): Result<DomainError, Rack>
  suspend fun deleteRack(id: String): Result<DomainError, Unit>
}
