package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.usecase.DeleteRackUseCaseType
import org.deafsapps.storeit.domain.usecase.GetRackDataByRackIdUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveSlotUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.rack.model.RackDetailSlotView
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class RackDetailViewModel(
    @InjectedParam private val rackId: String,
    coroutineScope: CoroutineScope? = null,
    private val getRackDataByRackIdUseCase: GetRackDataByRackIdUseCaseType,
    private val saveSlotUseCase: SaveSlotUseCaseType,
    private val saveRackUseCase: SaveRackUseCaseType,
    private val deleteRackUseCase: DeleteRackUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState = MutableStateFlow(RackDetailUiState.getDefault())
    val uiState: StateFlow<RackDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RackDetailUiEvent?>()
    val uiEvent: SharedFlow<RackDetailUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    init {
        flow {
            getRackDataByRackIdUseCase(input = rackId).fold(
                ifErr = { error ->
                    emit(_uiState.value.copy(isLoading = false, error = error.toErrorCause()))
                }, ifOk = { rackData ->
                    emit(
                        _uiState.value.copy(
                            rack = rackData.rack,
                            slots = rackData.shelfSlots.map { slot ->
                                RackDetailSlotView(
                                    id = slot.id,
                                    xRel = slot.position.xRel,
                                    yRel = slot.position.yRel,
                                )
                            },
                            isLoading = false,
                            error = null,
                        )
                    )
                }
            )
        }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun onImageTap(xRel: Float, yRel: Float) {
        viewModelScope.launch {
            val rack = _uiState.value.rack ?: return@launch
            val slot = ShelfSlot(
                id = Uuid.random().toString(),
                rackId = rack.id,
                position = SlotPosition(x = 0f, y = 0f, xRel = xRel, yRel = yRel),
            )
            saveSlotUseCase(input = slot).fold(
                ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(error.toErrorCause())) },
                ifOk = { saved ->
                    _uiState.update { state ->
                        state.copy(
                            slots = state.slots + RackDetailSlotView(
                                id = saved.id,
                                xRel = saved.position.xRel,
                                yRel = saved.position.yRel,
                            ),
                            selectedSlotId = saved.id,
                        )
                    }
                    _uiEvent.emit(RackDetailUiEvent.SlotSelected(saved.id))
                },
            )
        }
    }

    fun onEditSelect() {
        val rack = _uiState.value.rack ?: return
        _uiState.update { state ->
            state.copy(
                showEditDialog = true,
                editName = rack.name,
                editDescription = rack.description,
                editLocation = rack.location,
            )
        }
    }

    fun onUpdateEditName(name: String) {
        _uiState.update { state -> state.copy(editName = name) }
    }

    fun onUpdateEditDescription(description: String) {
        _uiState.update { state -> state.copy(editDescription = description) }
    }

    fun onUpdateEditLocation(location: String) {
        _uiState.update { state -> state.copy(editLocation = location) }
    }

    fun onDismissEditDialog() {
        _uiState.update { state -> state.copy(showEditDialog = false) }
    }

    fun onSaveRackEdits() {
        val state = _uiState.value
        val rack = state.rack ?: return
        viewModelScope.launch {
            val updated = rack.copy(
                name = state.editName.trim(),
                description = state.editDescription.trim(),
                location = state.editLocation.trim(),
            )
            saveRackUseCase(input = updated).fold(
                ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(error.toErrorCause())) },
                ifOk = {
                    _uiState.update { state -> state.copy(rack = updated, showEditDialog = false) }
                },
            )
        }
    }

    fun onRemoveRackSelect() {
        _uiState.update { state -> state.copy(showDeleteConfirm = true) }
    }

    fun onDismissDeleteConfirm() {
        _uiState.update { state -> state.copy(showDeleteConfirm = false) }
    }

    fun onConfirmDeleteRack() {
        viewModelScope.launch {
            deleteRackUseCase(input = rackId).fold(
                ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(error.toErrorCause())) },
                ifOk = {
                    _uiState.update { state -> state.copy(showDeleteConfirm = false) }
                    _uiEvent.emit(RackDetailUiEvent.NavigateBack)
                },
            )
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> message
    is DomainError.Unknown -> message
}
