package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.GetRacksUseCaseType

internal class FakeGetRacksUseCase : GetRacksUseCaseType {
    var invokeResult: Result<DomainError, List<Rack>>? = null

    override suspend fun invoke(input: Unit): Result<DomainError, List<Rack>> =
        invokeResult ?: emptyList<Rack>().ok()
}
