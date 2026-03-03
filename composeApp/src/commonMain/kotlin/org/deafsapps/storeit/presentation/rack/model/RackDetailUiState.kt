package org.deafsapps.storeit.presentation.rack.model

import org.deafsapps.storeit.domain.model.Rack

data class RackDetailUiState(
    val rack: Rack? = null,
    val slots: List<RackDetailSlotView> = emptyList(),
    val selectedSlotId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showEditDialog: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val editLocation: String = "",
    val showDeleteConfirm: Boolean = false,
)

sealed interface RackDetailUiEvent {
    data object NavigateBack : RackDetailUiEvent
    data class ShowError(val message: String) : RackDetailUiEvent
    data class SlotSelected(val slotId: String) : RackDetailUiEvent
}
