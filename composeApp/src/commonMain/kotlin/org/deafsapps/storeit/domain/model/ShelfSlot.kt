package org.deafsapps.storeit.domain.model

/**
 * Represents a tappable storage position inside a rack image.
 */
interface ShelfSlot {
    val id: String
    val rackId: String
    val position: SlotPosition
}

internal data class ShelfSlotModel(
    override val id: String,
    override val rackId: String,
    override val position: SlotPosition,
) : ShelfSlot

/**
 * Creates a public [ShelfSlot] instance for rack mapping and placement flows.
 */
fun ShelfSlot(
    id: String,
    rackId: String,
    position: SlotPosition,
): ShelfSlot = ShelfSlotModel(
    id = id,
    rackId = rackId,
    position = position,
)

internal fun ShelfSlot.asModel(): ShelfSlotModel = when (this) {
    is ShelfSlotModel -> this
    else -> ShelfSlotModel(id = id, rackId = rackId, position = position)
}
