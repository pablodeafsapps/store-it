package org.deafsapps.storeit.presentation.item.model

import androidx.compose.runtime.Immutable
import org.deafsapps.storeit.domain.model.Rack

@Immutable
data class AddItemUiState(
    val name: String,
    val description: String,
    val quantity: Int?,
    val owner: String,
    val tags: List<String>,
    val tagInput: String,
    val photoUri: String?,
    val selectedRackId: String?,
    val selectedSlotId: String?,
    val racks: List<Rack>,
    val step: AddItemStep,
    val isLoading: Boolean,
    val error: String?,
    val isSuccess: Boolean,
) {
    companion object {
        fun getDefault(
            initialRackId: String? = null,
            initialSlotId: String? = null,
        ): AddItemUiState = AddItemUiState(
            name = "",
            description = "",
            quantity = null,
            owner = "",
            tags = emptyList(),
            tagInput = "",
            photoUri = null,
            selectedRackId = initialRackId,
            selectedSlotId = initialSlotId,
            racks = emptyList(),
            step = AddItemStep.FORM,
            isLoading = false,
            error = null,
            isSuccess = false,
        )
    }
}

enum class AddItemStep {
    FORM,
    SELECT_RACK,
    SELECT_SLOT,
}

sealed interface AddItemUiEvent {
    data object NavigateBack : AddItemUiEvent
    data class ShowError(val message: String) : AddItemUiEvent
}
