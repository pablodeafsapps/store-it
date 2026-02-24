package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.SaveRackUseCase
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType
import org.deafsapps.storeit.presentation.createViewModelScope
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    data object NavigateBack : AddRackUiEvent
    data class ShowError(val message: String) : AddRackUiEvent
}

class AddRackViewModel(
    saveRackUseCase: SaveRackUseCaseType,
    coroutineScope: CoroutineScope = createViewModelScope(),
) {
    private val saveRackUseCase: SaveRackUseCase = saveRackUseCase
    private val coroutineScope: CoroutineScope = coroutineScope

    private val _uiState = MutableStateFlow(AddRackUiState())
    val uiState: StateFlow<AddRackUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableStateFlow<AddRackUiEvent?>(null)
    val uiEvent: StateFlow<AddRackUiEvent?> = _uiEvent.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description, error = null)
    }

    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(location = location, error = null)
    }

    fun updatePhotoUri(uri: String?) {
        _uiState.value = _uiState.value.copy(photoUri = uri, error = null)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun saveRack() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            _uiState.value = currentState.copy(error = "Name is required")
            return
        }

        coroutineScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            val rack = Rack(
                id = Uuid.random().toString(),
                name = currentState.name.trim(),
                description = currentState.description.trim(),
                location = currentState.location.trim(),
                photoUri = currentState.photoUri,
            )

            val result = saveRackUseCase(rack)
            result.fold(
                ifErr = { error: DomainError ->
                    val errorMessage = when (error) {
                        is DomainError.ValidationError -> error.reason
                        is DomainError.NotFound -> "Rack not found"
                        is DomainError.Unknown -> "An unknown error occurred"
                    }
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = errorMessage,
                    )
                },
                ifOk = { savedRack: Rack ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isSuccess = true,
                    )
                    _uiEvent.value = AddRackUiEvent.NavigateBack
                },
            )
        }
    }

    fun clearEvent() {
        _uiEvent.value = null
    }
}
