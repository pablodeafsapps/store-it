package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.usecase.DeleteRackUseCaseType

internal class FakeDeleteRackUseCase : DeleteRackUseCaseType {
    var invokeResult: Result<DomainError, Unit>? = null

    override suspend fun invoke(input: String): Result<DomainError, Unit> =
        invokeResult ?: Unit.ok()
}
