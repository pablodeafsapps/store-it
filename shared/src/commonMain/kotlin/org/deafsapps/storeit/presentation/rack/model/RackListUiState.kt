package org.deafsapps.storeit.presentation.rack.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class RackListUiState(
    val racks: ImmutableList<RackSummaryVo>,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): RackListUiState = RackListUiState(
            racks = persistentListOf(),
            isLoading = false,
            error = null,
        )
    }
}

sealed interface RackListUiEvent {
    data object NavigateToAddRack : RackListUiEvent
    data class NavigateToRackDetail(val rackId: String) : RackListUiEvent
    data class ShowError(val message: String) : RackListUiEvent
}
