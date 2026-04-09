package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Factory

/**
 * Loads every slot defined for a rack.
 */
interface GetSlotsByRackIdUseCaseType : UseCase<String, Result<DomainError, List<ShelfSlot>>>

@Factory(binds = [GetSlotsByRackIdUseCaseType::class])
internal class GetSlotsByRackIdUseCase(
    private val slotRepository: SlotRepository,
) : GetSlotsByRackIdUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, List<ShelfSlot>> =
        slotRepository.getSlotsByRack(rackId = input)
}
