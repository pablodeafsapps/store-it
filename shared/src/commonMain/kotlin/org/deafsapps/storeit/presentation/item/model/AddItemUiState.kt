package org.deafsapps.storeit.presentation.item.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType
import org.deafsapps.storeit.presentation.rack.model.RackSummaryVo

data class AddItemUiState(
    val name: String,
    val description: String,
    val quantity: Int?,
    val owner: String,
    val tags: ImmutableList<String>,
    val tagInput: String,
    val photoUri: String?,
    val selectedRackId: String?,
    val selectedSlotId: String?,
    val selectedSlotPlacementType: SlotPlacementType?,
    val selectedSlotXRel: Float?,
    val selectedSlotYRel: Float?,
    val racks: ImmutableList<RackSummaryVo>,
    val step: AddItemStep,
    val isLoading: Boolean,
    val error: String?,
    val isSuccess: Boolean,
) {
    companion object {
        fun getDefault(
            initialRackId: String? = null,
            addItemSlot: AddItemSlotVo = AddItemSlotVo.None,
        ): AddItemUiState = AddItemUiState(
            name = "",
            description = "",
            quantity = null,
            owner = "",
            tags = persistentListOf(),
            tagInput = "",
            photoUri = null,
            selectedRackId = initialRackId,
            selectedSlotId = addItemSlot.id,
            selectedSlotPlacementType = addItemSlot.placementType,
            selectedSlotXRel = addItemSlot.xRel,
            selectedSlotYRel = addItemSlot.yRel,
            racks = persistentListOf(),
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
