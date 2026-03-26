package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.usecase.DeleteItemUseCaseType

internal class FakeDeleteItemUseCase : DeleteItemUseCaseType {
    var invokeResult: Result<DomainError, Unit>? = null
    var lastId: String? = null

    override suspend fun invoke(input: String): Result<DomainError, Unit> {
        lastId = input
        return invokeResult ?: Unit.ok()
    }
}
