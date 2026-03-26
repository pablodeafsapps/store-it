package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.koin.core.annotation.Factory

data class GetItemsBySlotInput(val rackId: String, val slotId: String)

interface GetItemsBySlotUseCaseType : UseCase<GetItemsBySlotInput, Result<DomainError, List<Item>>>

@Factory(binds = [GetItemsBySlotUseCaseType::class])
internal class GetItemsBySlotUseCase(
    private val itemRepository: ItemRepository,
) : GetItemsBySlotUseCaseType {
    override suspend fun invoke(input: GetItemsBySlotInput): Result<DomainError, List<Item>> =
        itemRepository.getItemsBySlot(rackId = input.rackId, slotId = input.slotId)
}
