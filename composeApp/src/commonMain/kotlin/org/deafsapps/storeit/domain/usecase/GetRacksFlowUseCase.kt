package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.FlowUseCase
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import org.koin.core.annotation.Factory

interface GetRacksFlowUseCaseType : FlowUseCase<Unit, Result<DomainError, List<Rack>>>

@Factory(binds = [GetRacksFlowUseCaseType::class])
internal class GetRacksFlowUseCase(
    private val rackRepository: RackRepository,
) : GetRacksFlowUseCaseType {
    override fun invoke(input: Unit): Flow<Result<DomainError, List<Rack>>> = rackRepository.getAllRacksFlow()
}
