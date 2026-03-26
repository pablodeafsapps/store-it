package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.usecase.GetItemsBySlotInput
import org.deafsapps.storeit.domain.usecase.GetItemsBySlotUseCaseType

internal class FakeGetItemsBySlotUseCase : GetItemsBySlotUseCaseType {
    var invokeResult: Result<DomainError, List<Item>>? = null

    override suspend fun invoke(input: GetItemsBySlotInput): Result<DomainError, List<Item>> =
        invokeResult ?: DomainError.NotFound(resource = "Items", id = input.slotId).err()
}
