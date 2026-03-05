package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.usecase.GetSlotsByRackIdUseCaseType

internal class FakeGetSlotsByRackIdUseCase : GetSlotsByRackIdUseCaseType {
    var invokeResult: Result<DomainError, List<ShelfSlot>>? = null

    override suspend fun invoke(input: String): Result<DomainError, List<ShelfSlot>> =
        invokeResult ?: emptyList<ShelfSlot>().ok()
}
