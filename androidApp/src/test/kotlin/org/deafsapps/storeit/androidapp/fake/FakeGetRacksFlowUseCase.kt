package org.deafsapps.storeit.androidapp.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.GetRacksFlowUseCaseType

internal class FakeGetRacksFlowUseCase : GetRacksFlowUseCaseType {
    var invokeResult: Result<DomainError, List<Rack>>? = null

    override fun invoke(input: Unit): Flow<Result<DomainError, List<Rack>>> =
        flow { emit(invokeResult ?: emptyList<Rack>().ok()) }
}
