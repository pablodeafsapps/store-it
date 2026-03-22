package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.usecase.GetItemByIdUseCaseType

internal class FakeGetItemByIdUseCase : GetItemByIdUseCaseType {
    var invokeResult: Result<DomainError, Item>? = null

    override suspend fun invoke(input: String): Result<DomainError, Item> =
        invokeResult ?: DomainError.NotFound(resource = "Item", id = input).err()
}
