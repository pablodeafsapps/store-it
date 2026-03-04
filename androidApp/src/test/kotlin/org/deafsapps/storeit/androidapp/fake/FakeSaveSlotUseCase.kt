package org.deafsapps.storeit.androidapp.fake

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.usecase.SaveSlotUseCaseType

internal class FakeSaveSlotUseCase : SaveSlotUseCaseType {
    var invokeResult: Result<DomainError, ShelfSlot>? = null

    override suspend fun invoke(input: ShelfSlot): Result<DomainError, ShelfSlot> =
        invokeResult ?: input.ok()
}
