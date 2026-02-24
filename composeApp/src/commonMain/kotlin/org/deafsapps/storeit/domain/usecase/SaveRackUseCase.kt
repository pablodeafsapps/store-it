package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository

typealias SaveRackUseCaseType = UseCase<Rack, Result<DomainError, Rack>>

class SaveRackUseCase(
    private val rackRepository: RackRepository,
) : UseCase<Rack, Result<DomainError, Rack>> {
    override suspend fun invoke(input: Rack): Result<DomainError, Rack> = rackRepository.saveRack(input)
}
