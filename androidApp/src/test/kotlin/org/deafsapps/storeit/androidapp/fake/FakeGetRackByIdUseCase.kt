package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.GetRackByIdUseCaseType

internal class FakeGetRackByIdUseCase : GetRackByIdUseCaseType {
    var invokeResult: Result<DomainError, Rack>? = null

    override suspend fun invoke(input: String): Result<DomainError, Rack> =
        invokeResult ?: DomainError.NotFound(resource = "Rack", id = input).err()
}
