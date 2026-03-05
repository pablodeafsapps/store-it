package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
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
import org.deafsapps.storeit.presentation.rack.model.AddRackUiEvent
import org.deafsapps.storeit.presentation.rack.model.AddRackUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Provided
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class AddRackViewModel(
    @Provided private val coroutineScope: CoroutineScope,
    private val saveRackUseCase: SaveRackUseCaseType,
) {

    private val _uiState = MutableStateFlow(AddRackUiState.getDefault())
    val uiState: StateFlow<AddRackUiState> = _uiState.asStateFlow()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = AddRackUiState.getDefault(),
        )

    private val _uiEvent = MutableSharedFlow<AddRackUiEvent?>()
    val uiEvent: SharedFlow<AddRackUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description, error = null) }
    }

    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location, error = null) }
    }

    fun updatePhotoUri(uri: String?) {
        _uiState.update { it.copy(photoUri = uri, error = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun saveRack() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            _uiState.update { currentState.copy(error = "Name is required") }
            return
        }

        coroutineScope.launch {
            _uiState.update { currentState.copy(isLoading = true, error = null) }

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
                    _uiState.update {
                        currentState.copy(isLoading = false, error = errorMessage)
                    }
                },
                ifOk = { _ ->
                    _uiState.update {
                        AddRackUiState.getDefault().copy(isLoading = false, isSuccess = true)
                    }
                    _uiEvent.emit(AddRackUiEvent.NavigateBack)
                },
            )
        }
    }

    fun clear() {
        coroutineScope.cancel()
    }
}
