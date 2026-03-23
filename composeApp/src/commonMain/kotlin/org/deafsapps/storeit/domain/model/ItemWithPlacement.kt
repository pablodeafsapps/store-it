package org.deafsapps.storeit.domain.model

interface ItemWithPlacement {
    val item: Item
    val rackName: String
    val slotSummary: String
}

internal data class ItemWithPlacementModel(
    override val item: Item,
    override val rackName: String,
    override val slotSummary: String,
) : ItemWithPlacement

fun ItemWithPlacement(
    item: Item,
    rackName: String,
    slotSummary: String,
): ItemWithPlacement = ItemWithPlacementModel(
    item = item,
    rackName = rackName,
    slotSummary = slotSummary,
)

internal fun ItemWithPlacement.asModel(): ItemWithPlacementModel = when (this) {
    is ItemWithPlacementModel -> this
    else -> ItemWithPlacementModel(item = item, rackName = rackName, slotSummary = slotSummary)
}
