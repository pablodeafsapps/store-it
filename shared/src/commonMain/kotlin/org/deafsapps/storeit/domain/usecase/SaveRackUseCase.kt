package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import org.koin.core.annotation.Factory

/**
 * Saves a new or updated rack in the repository layer.
 */
interface SaveRackUseCaseType : UseCase<Rack, Result<DomainError, Rack>>

@Factory(binds = [SaveRackUseCaseType::class])
internal class SaveRackUseCase(
    private val rackRepository: RackRepository,
) : SaveRackUseCaseType {
    override suspend fun invoke(input: Rack): Result<DomainError, Rack> = rackRepository.saveRack(rack = input)
}
