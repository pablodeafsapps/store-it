package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.RackData
import org.deafsapps.storeit.domain.usecase.GetRackDataByRackIdUseCaseType

internal class FakeGetRackDataByRackIdUseCase : GetRackDataByRackIdUseCaseType {
    var invokeResult: Result<DomainError, RackData>? = null

    override suspend fun invoke(input: String): Result<DomainError, RackData> =
        invokeResult ?: DomainError.NotFound(resource = "Rack data for rack with id $input", id = input).err()
}
