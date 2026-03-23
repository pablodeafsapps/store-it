package org.deafsapps.storeit.domain.model

interface RackData {
    val id: String
    val rack: Rack
    val shelfSlots: List<ShelfSlot>
    val items: List<Item>
}

internal data class RackDataModel(
    override val id: String,
    override val rack: Rack,
    override val shelfSlots: List<ShelfSlot>,
    override val items: List<Item>,
) : RackData

fun RackData(
    id: String,
    rack: Rack,
    shelfSlots: List<ShelfSlot>,
    items: List<Item>,
): RackData = RackDataModel(
    id = id,
    rack = rack,
    shelfSlots = shelfSlots,
    items = items,
)

internal fun RackData.asModel(): RackDataModel = when (this) {
    is RackDataModel -> this
    else -> RackDataModel(
        id = id,
        rack = rack,
        shelfSlots = shelfSlots,
        items = items,
    )
}
