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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.usecase.DeleteRackUseCaseType
import org.deafsapps.storeit.domain.usecase.GetRackDataByRackIdUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveSlotUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.rack.mapper.toRackDetailSlotVo
import org.deafsapps.storeit.presentation.rack.mapper.toRackDetailSlotsVo
import org.deafsapps.storeit.presentation.rack.model.RackDetailSlotVo
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L
private const val SLOT_TAP_HIT_RADIUS_REL = 0.08f

@Factory
class RackDetailViewModel(
    @InjectedParam private val rackId: String,
    coroutineScope: CoroutineScope?,
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
        loadRackDataById(rackId = rackId)
    }

    fun onImageTap(xRel: Float, yRel: Float, forItemPlacement: Boolean = false) {
        viewModelScope.launch {
            val rack = _uiState.value.rack ?: return@launch
            val slots = _uiState.value.slots
            slots.findNearestSlotWithin(
                xRel = xRel,
                yRel = yRel,
                radiusRel = SLOT_TAP_HIT_RADIUS_REL,
            )
                ?.selectSlotAndNavigateToDetailsIfApply(
                    rack = rack,
                    forItemPlacement = forItemPlacement
                )
                ?: run {
                    selectAndSaveNewSlot(rack = rack, xRel = xRel, yRel = yRel)
                }
        }
    }

    fun onEditSelected() {
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
            val updatedRack = Rack(
                id = rack.id,
                name = state.editName.trim(),
                description = state.editDescription.trim(),
                location = state.editLocation.trim(),
                photoUri = rack.photoUri,
                createdAt = rack.createdAt,
                updatedAt = rack.updatedAt,
            )
            saveRackUseCase(input = updatedRack).fold(
                ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(message = error.toErrorCause())) },
                ifOk = {
                    _uiState.update { state -> state.copy(rack = updatedRack, showEditDialog = false) }
                },
            )
        }
    }

    fun onRemoveRackSelected() {
        _uiState.update { state -> state.copy(showDeleteConfirm = true) }
    }

    fun onDismissDeleteConfirm() {
        _uiState.update { state -> state.copy(showDeleteConfirm = false) }
    }

    fun onConfirmDeleteRack() {
        viewModelScope.launch {
            deleteRackUseCase(input = rackId).fold(
                ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(message = error.toErrorCause())) },
                ifOk = {
                    _uiState.update { state -> state.copy(showDeleteConfirm = false) }
                    _uiEvent.emit(RackDetailUiEvent.NavigateBack)
                },
            )
        }
    }

    private fun loadRackDataById(rackId: String) {
        viewModelScope.launch {
            getRackDataByRackIdUseCase(input = rackId).fold(
                ifErr = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.toErrorCause(),
                        )
                    }
                }, ifOk = { rackData ->
                    _uiState.update { state ->
                        state.copy(
                            rack = rackData.rack,
                            slots = rackData.shelfSlots.toRackDetailSlotsVo(),
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            )
        }
    }

    private suspend fun RackDetailSlotVo.selectSlotAndNavigateToDetailsIfApply(
        rack: Rack,
        forItemPlacement: Boolean,
    ) {
        _uiState.update { state -> state.copy(selectedSlot = this) }
        if (!forItemPlacement) {
            _uiEvent.emit(
                RackDetailUiEvent.NavigateToSlotItems(
                    rackId = rack.id,
                    slotId = id
                ),
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun selectAndSaveNewSlot(rack: Rack, xRel: Float, yRel: Float) {
        val slot = ShelfSlot(
            id = Uuid.random().toString(),
            rackId = rack.id,
            position = SlotPosition(x = 0f, y = 0f, xRel = xRel, yRel = yRel),
        )
        saveSlotUseCase(input = slot).fold(
            ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(error.toErrorCause())) },
            ifOk = { saved ->
                _uiState.update { state -> state.copy(selectedSlot = slot.toRackDetailSlotVo()) }
                _uiState.update { state ->
                    state.copy(
                        slots = state.slots + RackDetailSlotVo(
                            id = saved.id,
                            xRel = saved.position.xRel,
                            yRel = saved.position.yRel,
                        ),
                        selectedSlot = saved.toRackDetailSlotVo(),
                    )
                }
                _uiEvent.emit(
                    RackDetailUiEvent.SlotSelected(
                        rackId = saved.rackId,
                        slotId = saved.id
                    )
                )
            },
        )
    }

}

internal fun List<RackDetailSlotVo>.findNearestSlotWithin(
    xRel: Float,
    yRel: Float,
    radiusRel: Float,
): RackDetailSlotVo? =
    takeIf { it.isNotEmpty() }?.let { slots ->
        val r2 = radiusRel * radiusRel
        slots
            .map { slot -> slot to slotDistanceSquared(xRel, yRel, slot) }
            .filter { it.second <= r2 }
            .minByOrNull { it.second }
            ?.first
    }

private fun slotDistanceSquared(xRel: Float, yRel: Float, slot: RackDetailSlotVo): Float {
    val dx = slot.xRel - xRel
    val dy = slot.yRel - yRel
    return dx * dx + dy * dy
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Rack details not found"
    is DomainError.Unknown -> "An unknown error occurred"
}
