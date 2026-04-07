package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.koin.core.annotation.Factory

/**
 * Loads a single item by identifier.
 */
interface GetItemByIdUseCaseType : UseCase<String, Result<DomainError, Item>>

@Factory(binds = [GetItemByIdUseCaseType::class])
internal class GetItemByIdUseCase(
    private val itemRepository: ItemRepository,
) : GetItemByIdUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, Item> =
        itemRepository.getItemById(id = input)
}
