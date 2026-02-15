package org.deafsapps.storeit.domain.model

data class ShelfSlot(
    val id: String,
    val rackId: String,
    val position: SlotPosition,
)
