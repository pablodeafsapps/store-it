package org.deafsapps.storeit.presentation.rack.viewmodel

import org.deafsapps.storeit.domain.model.Rack

data class RackListUiState(
    val racks: List<Rack> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    companion object {
        fun getDefault() = RackListUiState(isLoading = true)
    }
}

sealed interface RackListUiEvent {
    data object NavigateToAddRack : RackListUiEvent
    data class NavigateToRackDetail(val rackId: String) : RackListUiEvent
    data class ShowError(val message: String) : RackListUiEvent
}
