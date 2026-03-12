package org.deafsapps.storeit.presentation.rack.model

import androidx.compose.runtime.Stable

@Stable
data class RackDetailSlotView(
    val id: String,
    val xRel: Float,
    val yRel: Float,
)
