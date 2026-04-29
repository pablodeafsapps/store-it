package org.deafsapps.storeit.presentation.mapper

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.ItemWithPlacement
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.presentation.item.model.ItemSummaryVo
import org.deafsapps.storeit.presentation.rack.model.RackSummaryVo
import org.deafsapps.storeit.presentation.search.model.SearchResultVo

internal fun Rack.toRackSummaryVo(): RackSummaryVo = RackSummaryVo(
    id = id,
    name = name,
    location = location,
    photoUri = photoUri,
)

internal fun List<Rack>.toRackSummaryVos(): ImmutableList<RackSummaryVo> =
    map { rack -> rack.toRackSummaryVo() }.toImmutableList()

internal fun Item.toItemSummaryVo(): ItemSummaryVo = ItemSummaryVo(
    id = id,
    name = name,
)

internal fun List<Item>.toItemSummaryVos(): ImmutableList<ItemSummaryVo> =
    map { item -> item.toItemSummaryVo() }.toImmutableList()

internal fun ItemWithPlacement.toSearchResultVo(): SearchResultVo = SearchResultVo(
    itemId = item.id,
    rackId = item.rackId,
    slotId = item.slotId,
    itemName = item.name,
    rackName = rackName,
    slotSummary = slotSummary,
)

internal fun List<ItemWithPlacement>.toSearchResultVos(): ImmutableList<SearchResultVo> =
    map { itemWithPlacement -> itemWithPlacement.toSearchResultVo() }.toImmutableList()
