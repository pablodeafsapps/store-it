package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.usecase.AddItemUseCaseType

internal class FakeAddItemUseCase : AddItemUseCaseType {
    var invokeResult: Result<DomainError, Item>? = null
    var lastItem: Item? = null

    override suspend fun invoke(input: Item): Result<DomainError, Item> {
        lastItem = input
        return invokeResult ?: input.ok()
    }
}
