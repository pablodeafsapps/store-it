package org.deafsapps.storeit.presentation.rack.mapper

import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo

internal fun List<ShelfSlot>.toRackSlotMarkerVos(): List<RackSlotMarkerVo> =
    map { slot -> slot.toRackSlotMarkerVo() }

internal fun ShelfSlot.toRackSlotMarkerVo(): RackSlotMarkerVo =
    RackSlotMarkerVo(
        id = id,
        xRel = position.xRel,
        yRel = position.yRel,
    )
