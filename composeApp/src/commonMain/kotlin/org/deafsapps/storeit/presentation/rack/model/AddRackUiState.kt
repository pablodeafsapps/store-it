package org.deafsapps.storeit.presentation.rack.model

data class AddRackUiState(
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val photoUri: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
)

sealed interface AddRackUiEvent {
    data object NavigateBack: AddRackUiEvent
    data class ShowError(val message: String) : AddRackUiEvent
}
