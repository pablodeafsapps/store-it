package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.rack.model.AddRackUiEvent
import org.deafsapps.storeit.presentation.rack.model.AddRackUiState
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class AddRackViewModel(
    coroutineScope: CoroutineScope?,
    private val saveRackUseCase: SaveRackUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState = MutableStateFlow(AddRackUiState.getDefault())
    val uiState: StateFlow<AddRackUiState> = _uiState.asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = AddRackUiState.getDefault(),
        )

    private val _uiEvent = MutableSharedFlow<AddRackUiEvent?>()
    val uiEvent: SharedFlow<AddRackUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    fun onUpdateName(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onUpdateDescription(description: String) {
        _uiState.update { it.copy(description = description, error = null) }
    }

    fun onUpdateLocation(location: String) {
        _uiState.update { it.copy(location = location, error = null) }
    }

    fun onUpdatePhotoUri(uri: String?) {
        _uiState.update { it.copy(photoUri = uri, error = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun onSaveRack() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            _uiState.update { state -> state.copy(error = "Name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, error = null) }

            val rack = Rack(
                id = Uuid.random().toString(),
                name = currentState.name.trim(),
                description = currentState.description.trim(),
                location = currentState.location.trim(),
                photoUri = currentState.photoUri,
            )

            saveRackUseCase(rack).fold(
                ifErr = { error ->
                    val errorMessage = when (error) {
                        is DomainError.AuthenticationFailed,
                        is DomainError.ServiceUnavailable,
                        is DomainError.ConfigurationError,
                        is DomainError.Unknown -> error.message
                        is DomainError.ValidationError -> error.reason
                        is DomainError.NotFound -> "Rack not found"
                    }
                    _uiState.update { state ->
                        state.copy(isLoading = false, error = errorMessage)
                    }
                },
                ifOk = { _ ->
                    _uiEvent.emit(AddRackUiEvent.NavigateBack)
                    _uiState.value = AddRackUiState.getDefault()
                },
            )
        }
    }
}
