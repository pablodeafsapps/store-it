package org.deafsapps.storeit.presentation.rack.model

import androidx.compose.runtime.Immutable
import org.deafsapps.storeit.domain.model.Rack

@Immutable
data class RackDetailUiState(
    val rack: Rack?,
    val slots: List<RackDetailSlotVo>,
    val selectedSlotId: String?,
    val isLoading: Boolean,
    val error: String?,
    val showEditDialog: Boolean,
    val editName: String,
    val editDescription: String,
    val editLocation: String,
    val showDeleteConfirm: Boolean,
) {
    companion object {
        fun getDefault(): RackDetailUiState = RackDetailUiState(
            rack = null,
            slots = emptyList(),
            selectedSlotId = null,
            isLoading = false,
            error = null,
            showEditDialog = false,
            editName = "",
            editDescription = "",
            editLocation = "",
            showDeleteConfirm = false,
        )
    }
}

sealed interface RackDetailUiEvent {
    data object NavigateBack : RackDetailUiEvent
    data class ShowError(val message: String) : RackDetailUiEvent
    data class SlotSelected(val rackId: String, val slotId: String) : RackDetailUiEvent
}
