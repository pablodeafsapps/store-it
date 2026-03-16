package org.deafsapps.storeit.presentation.rack.mapper

import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.presentation.rack.model.RackDetailSlotVo

internal fun List<ShelfSlot>.toRackDetailSlotsVo(): List<RackDetailSlotVo> =
    map { slot -> slot.toRackDetailSlotVo() }

private fun ShelfSlot.toRackDetailSlotVo(): RackDetailSlotVo =
    RackDetailSlotVo(
        id = id,
        xRel = position.xRel,
        yRel = position.yRel,
    )
