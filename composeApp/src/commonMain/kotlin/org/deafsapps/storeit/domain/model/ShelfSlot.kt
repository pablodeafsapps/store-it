package org.deafsapps.storeit.domain.model

internal data class ShelfSlot(
    val id: String,
    val rackId: String,
    val position: SlotPosition,
)
