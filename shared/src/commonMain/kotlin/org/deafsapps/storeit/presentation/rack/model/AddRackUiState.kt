package org.deafsapps.storeit.presentation.rack.model

data class AddRackUiState(
    val name: String,
    val description: String,
    val location: String,
    val photoUri: String?,
    val isLoading: Boolean,
    val error: String?,
    val isSuccess: Boolean,
) {
    companion object {
        fun getDefault(): AddRackUiState = AddRackUiState(
            name = "",
            description = "",
            location = "",
            photoUri = null,
            isLoading = false,
            error = null,
            isSuccess = false,
        )
    }
}

sealed interface AddRackUiEvent {
    data object NavigateBack: AddRackUiEvent
    data class ShowError(val message: String) : AddRackUiEvent
}
