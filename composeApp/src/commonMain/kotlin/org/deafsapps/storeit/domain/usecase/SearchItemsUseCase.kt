package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.mapLeft
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.base.suspendFlatMap
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.ItemWithPlacement
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.koin.core.annotation.Factory

interface SearchItemsUseCaseType : UseCase<String, Result<DomainError, List<ItemWithPlacement>>>

@Factory(binds = [SearchItemsUseCaseType::class])
internal class SearchItemsUseCase(
    private val itemRepository: ItemRepository,
    private val getRackByIdUseCase: GetRackByIdUseCaseType,
    private val getSlotsByRackIdUseCase: GetSlotsByRackIdUseCaseType,
) : SearchItemsUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, List<ItemWithPlacement>> =
        itemRepository.searchItems(query = input)
            .suspendFlatMap(::enrichSearchResults)

    private suspend fun enrichSearchResults(items: List<Item>): Result<DomainError, List<ItemWithPlacement>> {
        if (items.isEmpty()) return emptyList<ItemWithPlacement>().ok()
        val rackCache = mutableMapOf<String, Rack>()
        val slotsCache = mutableMapOf<String, List<ShelfSlot>>()
        val out = mutableListOf<ItemWithPlacement>()
        for (item in items) {
            val rack = rackCache[item.rackId]
                ?: getRackByIdUseCase(input = item.rackId).fold(
                    ifErr = { error -> return error.err() },
                    ifOk = { r -> r.also { rackCache[item.rackId] = r } }
                )
            val slots = slotsCache[item.rackId]
                ?: getSlotsByRackIdUseCase(item.rackId).fold(
                    ifErr = { error -> return error.err() },
                    ifOk = { list -> list.also { slotsCache[item.rackId] = list } }
                )
            val slot = slots.firstOrNull { slot -> slot.id == item.slotId }
            out.add(
                ItemWithPlacement(
                    item = item,
                    rackName = rack.name,
                    slotSummary = slot?.toSlotSummary() ?: item.slotId,
                )
            )
        }
        return out.ok()
    }
}

private fun ShelfSlot.toSlotSummary(): String {
    val x = (position.xRel * 100f).toInt().coerceIn(0, 100)
    val y = (position.yRel * 100f).toInt().coerceIn(0, 100)
    return "$x%, $y%"
}
