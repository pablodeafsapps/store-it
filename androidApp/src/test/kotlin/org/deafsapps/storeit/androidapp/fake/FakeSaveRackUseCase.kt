package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType

internal class FakeSaveRackUseCase : SaveRackUseCaseType {
    var invokeResult: Result<DomainError, Rack>? = null

    override suspend fun invoke(input: Rack): Result<DomainError, Rack> =
        invokeResult ?: input.ok()
}
