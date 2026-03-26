package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Factory

interface SaveSlotUseCaseType : UseCase<ShelfSlot, Result<DomainError, ShelfSlot>>

@Factory(binds = [SaveSlotUseCaseType::class])
internal class SaveSlotUseCase(
    private val slotRepository: SlotRepository,
) : SaveSlotUseCaseType {
    override suspend fun invoke(input: ShelfSlot): Result<DomainError, ShelfSlot> =
        slotRepository.saveSlot(slot = input)
}
