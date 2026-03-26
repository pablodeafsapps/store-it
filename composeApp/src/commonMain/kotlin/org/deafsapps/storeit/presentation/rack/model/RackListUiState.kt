package org.deafsapps.storeit.presentation.rack.model

import androidx.compose.runtime.Immutable
import org.deafsapps.storeit.domain.model.Rack

@Immutable
data class RackListUiState(
    val racks: List<Rack>,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): RackListUiState = RackListUiState(
            racks = emptyList(),
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
