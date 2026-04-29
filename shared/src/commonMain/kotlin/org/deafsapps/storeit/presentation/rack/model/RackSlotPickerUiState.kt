package org.deafsapps.storeit.presentation.rack.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class RackSlotPickerUiState(
    val rack: RackSummaryVo?,
    val slots: ImmutableList<RackSlotMarkerVo>,
    val selectedSlot: RackSlotMarkerVo?,
    val selectedPlacementType: SlotPlacementType?,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): RackSlotPickerUiState = RackSlotPickerUiState(
            rack = null,
            slots = persistentListOf(),
            selectedSlot = null,
            selectedPlacementType = null,
            isLoading = false,
            error = null,
        )
    }
}
