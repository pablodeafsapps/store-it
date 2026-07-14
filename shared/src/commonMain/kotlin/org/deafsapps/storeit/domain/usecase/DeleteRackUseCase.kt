package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.getOrElse
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.koin.core.annotation.Factory

/**
 * Deletes a rack together with its dependent slots and items.
 */
sealed interface DeleteRackOutcome {
    data class Deleted(
        val rackId: String,
        val deletedSlotCount: Long,
        val deletedItemCount: Int,
    ) : DeleteRackOutcome
}

interface DeleteRackUseCaseType : UseCase<String, Result<DomainError, DeleteRackOutcome>>

@Factory(binds = [DeleteRackUseCaseType::class])
internal class DeleteRackUseCase(
    private val rackRepository: RackRepository,
    private val slotRepository: SlotRepository,
    private val itemRepository: ItemRepository,
) : DeleteRackUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, DeleteRackOutcome> {
        val itemsToDelete = itemRepository.getItemsByRack(rackId = input).getOrElse { error -> return error }
        val deletedSlotCount = slotRepository.deleteByRack(rackId = input).getOrElse { error -> return error }
        itemRepository.deleteItemsByRack(rackId = input).getOrElse { error -> return error }
        rackRepository.deleteRack(id = input).getOrElse { error -> return error }

        return DeleteRackOutcome.Deleted(
            rackId = input,
            deletedSlotCount = deletedSlotCount,
            deletedItemCount = itemsToDelete.size,
        ).ok()
    }
}
