package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Factory

/**
 * Deletes a rack together with its dependent slots and items.
 */
interface DeleteRackUseCaseType : UseCase<String, Result<DomainError, Unit>>

@Factory(binds = [DeleteRackUseCaseType::class])
internal class DeleteRackUseCase(
    private val rackRepository: RackRepository,
    private val slotRepository: SlotRepository,
    private val itemRepository: ItemRepository,
) : DeleteRackUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, Unit> {
        slotRepository.deleteByRack(rackId = input)
        itemRepository.deleteItemsByRack(rackId = input)
        return rackRepository.deleteRack(id = input)
    }
}
