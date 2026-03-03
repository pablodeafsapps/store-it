package org.deafsapps.storeit.presentation.rack.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.usecase.DeleteRackUseCaseType
import org.deafsapps.storeit.domain.usecase.GetRackByIdUseCaseType
import org.deafsapps.storeit.domain.usecase.GetSlotsByRackUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveSlotUseCaseType
import org.deafsapps.storeit.presentation.createViewModelScope
import org.deafsapps.storeit.presentation.rack.model.RackDetailSlotView
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiState
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@KoinViewModel
class RackDetailViewModel(
    @InjectedParam private val rackId: String,
    private val getRackByIdUseCase: GetRackByIdUseCaseType,
    private val getSlotsByRackUseCase: GetSlotsByRackUseCaseType,
    private val saveSlotUseCase: SaveSlotUseCaseType,
    private val saveRackUseCase: SaveRackUseCaseType,
    private val deleteRackUseCase: DeleteRackUseCaseType,
    scope: CoroutineScope? = null,
) : ViewModel() {

    private val coroutineScope = scope ?: createViewModelScope()

    private val _uiState = MutableStateFlow(RackDetailUiState())
    val uiState: StateFlow<RackDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RackDetailUiEvent?>()
    val uiEvent: SharedFlow<RackDetailUiEvent?> = _uiEvent.asSharedFlow()

    init {
        loadRackAndSlots()
    }

    fun loadRackAndSlots() {
        coroutineScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getRackByIdUseCase(input = rackId).fold(
                ifErr = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.toErrorCause(),
                        )
                    }
                },
                ifOk = { rack ->
                    getSlotsByRackUseCase(input = rackId).fold(
                        ifErr = { err ->
                            _uiState.update {
                                it.copy(
                                    rack = rack,
                                    slots = emptyList(),
                                    isLoading = false,
                                    error = err.toErrorCause(),
                                )
                            }
                        },
                        ifOk = { slotList ->
                            _uiState.update {
                                it.copy(
                                    rack = rack,
                                    slots = slotList.map { s ->
                                        RackDetailSlotView(
                                            id = s.id,
                                            xRel = s.position.xRel,
                                            yRel = s.position.yRel,
                                        )
                                    },
                                    isLoading = false,
                                    error = null,
                                )
                            }
                        },
                    )
                },
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun onImageTap(xRel: Float, yRel: Float) {
        coroutineScope.launch {
            val rack = _uiState.value.rack ?: return@launch
            val slot = ShelfSlot(
                id = Uuid.random().toString(),
                rackId = rack.id,
                position = SlotPosition(x = 0f, y = 0f, xRel = xRel, yRel = yRel),
            )
            saveSlotUseCase(input = slot).fold(
                ifErr = { _uiEvent.emit(RackDetailUiEvent.ShowError(it.toErrorCause())) },
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

    fun onEditClick() {
        val rack = _uiState.value.rack ?: return
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editName = rack.name,
                editDescription = rack.description,
                editLocation = rack.location,
            )
        }
    }

    fun updateEditName(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun updateEditDescription(description: String) {
        _uiState.update { it.copy(editDescription = description) }
    }

    fun updateEditLocation(location: String) {
        _uiState.update { it.copy(editLocation = location) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(showEditDialog = false) }
    }

    fun saveRackEdits() {
        val state = _uiState.value
        val rack = state.rack ?: return
        coroutineScope.launch {
            val updated = rack.copy(
                name = state.editName.trim(),
                description = state.editDescription.trim(),
                location = state.editLocation.trim(),
            )
            saveRackUseCase(input = updated).fold(
                ifErr = { _uiEvent.emit(RackDetailUiEvent.ShowError(it.toErrorCause())) },
                ifOk = {
                    _uiState.update { it.copy(rack = updated, showEditDialog = false) }
                },
            )
        }
    }

    fun onRemoveRackClick() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDeleteRack() {
        coroutineScope.launch {
            deleteRackUseCase(input = rackId).fold(
                ifErr = { _uiEvent.emit(RackDetailUiEvent.ShowError(it.toErrorCause())) },
                ifOk = {
                    _uiState.update { it.copy(showDeleteConfirm = false) }
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
