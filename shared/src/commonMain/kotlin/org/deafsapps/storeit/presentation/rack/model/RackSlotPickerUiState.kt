package org.deafsapps.storeit.presentation.rack.model

import androidx.compose.runtime.Immutable
import org.deafsapps.storeit.domain.model.Rack

@Immutable
data class RackSlotPickerUiState(
    val rack: Rack?,
    val slots: List<RackSlotMarkerVo>,
    val selectedSlot: RackSlotMarkerVo?,
    val selectedPlacementType: SlotPlacementType?,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): RackSlotPickerUiState = RackSlotPickerUiState(
            rack = null,
            slots = emptyList(),
            selectedSlot = null,
            selectedPlacementType = null,
            isLoading = false,
            error = null,
        )
    }
}
