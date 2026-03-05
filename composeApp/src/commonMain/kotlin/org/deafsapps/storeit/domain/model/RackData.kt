package org.deafsapps.storeit.domain.model

data class RackData(
    val id: String,
    val rack: Rack,
    val shelfSlots: List<ShelfSlot>,
    val items: List<Item>,
)
