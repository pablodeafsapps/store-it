package org.deafsapps.storeit.presentation.rack.model

import androidx.compose.runtime.Immutable

@Immutable
data class RackSlotMarkerVo(
    val id: String,
    val xRel: Float,
    val yRel: Float,
)
