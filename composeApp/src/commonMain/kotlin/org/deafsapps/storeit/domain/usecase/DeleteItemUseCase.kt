package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.koin.core.annotation.Factory

interface DeleteItemUseCaseType : UseCase<String, Result<DomainError, Unit>>

@Factory(binds = [DeleteItemUseCaseType::class])
internal class DeleteItemUseCase(private val itemRepository: ItemRepository) : DeleteItemUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, Unit> = itemRepository.deleteItem(id = input)
}
