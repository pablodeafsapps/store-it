package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.koin.core.annotation.Factory

interface AddItemUseCaseType : UseCase<Item, Result<DomainError, Item>>

@Factory(binds = [AddItemUseCaseType::class])
internal class AddItemUseCase(
    private val itemRepository: ItemRepository,
) : AddItemUseCaseType {
    override suspend fun invoke(input: Item): Result<DomainError, Item> = itemRepository.saveItem(item = input)
}
