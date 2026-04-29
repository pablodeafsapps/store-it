package org.deafsapps.storeit.presentation.item.model

import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType

data class AddItemSlotVo(
    val id: String? = null,
    val placementType: SlotPlacementType? = null,
    val xRel: Float? = null,
    val yRel: Float? = null,
) {
    companion object {
        val None: AddItemSlotVo = AddItemSlotVo()
    }
}

fun RackSlotMarkerVo.toAddItemSlotVo(placementType: SlotPlacementType): AddItemSlotVo =
    when (placementType) {
        SlotPlacementType.EXISTING ->
            AddItemSlotVo(id = id, placementType = placementType, xRel = null, yRel = null)
        SlotPlacementType.DRAFT ->
            AddItemSlotVo(id = id, placementType = placementType, xRel = xRel, yRel = yRel)
    }
