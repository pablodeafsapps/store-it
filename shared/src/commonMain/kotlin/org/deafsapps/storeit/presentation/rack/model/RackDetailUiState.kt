package org.deafsapps.storeit.presentation.rack.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class RackDetailUiState(
    val rack: RackSummaryVo?,
    val slots: ImmutableList<RackSlotMarkerVo>,
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
            slots = persistentListOf(),
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
    data class NavigateToAddItemDraft(
        val rackId: String,
        val slotId: String,
        val slotXRel: Float,
        val slotYRel: Float,
    ) : RackDetailUiEvent
    data class NavigateToSlotItems(val rackId: String, val slotId: String) : RackDetailUiEvent
}
