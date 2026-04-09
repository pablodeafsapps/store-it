package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository
import org.koin.core.annotation.Factory

/**
 * Loads a single rack by identifier.
 */
interface GetRackByIdUseCaseType : UseCase<String, Result<DomainError, Rack>>

@Factory(binds = [GetRackByIdUseCaseType::class])
internal class GetRackByIdUseCase(
    private val rackRepository: RackRepository,
) : GetRackByIdUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, Rack> =
        rackRepository.getRackById(id = input)
}
