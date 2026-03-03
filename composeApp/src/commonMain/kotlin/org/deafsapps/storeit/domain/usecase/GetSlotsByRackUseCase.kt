package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Factory

interface GetSlotsByRackUseCaseType : UseCase<String, Result<DomainError, List<ShelfSlot>>>

@Factory(binds = [GetSlotsByRackUseCaseType::class])
internal class GetSlotsByRackUseCase(
    private val slotRepository: SlotRepository,
) : GetSlotsByRackUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, List<ShelfSlot>> =
        slotRepository.getSlotsByRack(rackId = input)
}
