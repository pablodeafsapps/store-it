package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import org.koin.core.annotation.Factory

interface GetRacksUseCaseType : UseCase<Unit, Result<DomainError, List<Rack>>>

@Factory(binds = [GetRacksUseCaseType::class])
internal class GetRacksUseCase(
    private val rackRepository: RackRepository,
) : GetRacksUseCaseType {
    override suspend fun invoke(input: Unit): Result<DomainError, List<Rack>> = rackRepository.getAllRacks()
}
