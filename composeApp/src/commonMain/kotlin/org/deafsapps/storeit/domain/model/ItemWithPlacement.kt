package org.deafsapps.storeit.domain.model

data class ItemWithPlacement(
    val item: Item,
    val rackName: String,
    val slotSummary: String,
)
