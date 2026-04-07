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
import org.deafsapps.storeit.presentation.rack.mapper.toRackSlotMarkerVos
import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo
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
    private val saveRackUseCase: SaveRackUseCaseType,
    private val deleteRackUseCase: DeleteRackUseCaseType,
    private val saveSlotUseCase: SaveSlotUseCaseType,
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

    fun onImageTap(xRel: Float, yRel: Float) {
        viewModelScope.launch {
            val rack = _uiState.value.rack ?: return@launch
            val slots = _uiState.value.slots
            slots.findNearestSlotWithinOrNull(
                xRel = xRel,
                yRel = yRel,
                radiusRel = SLOT_TAP_HIT_RADIUS_REL,
            )?.let { slot ->
                _uiEvent.emit(
                    RackDetailUiEvent.NavigateToSlotItems(
                        rackId = rack.id,
                        slotId = slot.id,
                    ),
                )
            } ?: run {
                navigateToAddItemWithDraftSlot(rack = rack, xRel = xRel, yRel = yRel)
            }
        }
    }

    fun onSlotMarkerDrag(slotId: String, xRel: Float, yRel: Float) {
        applySlotMarkerPosition(slotId = slotId, xRel = xRel, yRel = yRel)
    }

    fun onSaveSlotMarkerPosition(slotId: String, xRel: Float, yRel: Float) {
        val rack = _uiState.value.rack ?: return
        val (boundedXRel, boundedYRel) =
            applySlotMarkerPosition(slotId = slotId, xRel = xRel, yRel = yRel) ?: return
        viewModelScope.launch {
            saveSlotUseCase(
                input = ShelfSlot(
                    id = slotId,
                    rackId = rack.id,
                    position = SlotPosition(
                        x = boundedXRel,
                        y = boundedYRel,
                        xRel = boundedXRel,
                        yRel = boundedYRel,
                    ),
                ),
            ).fold(
                ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(message = error.toErrorCause())) },
                ifOk = { },
            )
        }
    }

    private fun applySlotMarkerPosition(slotId: String, xRel: Float, yRel: Float): Pair<Float, Float>? {
        if (_uiState.value.rack == null) return null
        val boundedXRel = xRel.coerceIn(0f, 1f)
        val boundedYRel = yRel.coerceIn(0f, 1f)
        _uiState.update { state ->
            state.copy(
                slots = state.slots.map { slot ->
                    if (slot.id == slotId) {
                        slot.copy(xRel = boundedXRel, yRel = boundedYRel)
                    } else {
                        slot
                    }
                },
            )
        }
        return boundedXRel to boundedYRel
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
                    _uiState.update { s -> s.copy(rack = updatedRack, showEditDialog = false) }
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
                },
                ifOk = { rackData ->
                    _uiState.update { state ->
                        val mappedSlots = rackData.shelfSlots.toRackSlotMarkerVos()
                        state.copy(
                            rack = rackData.rack,
                            slots = mappedSlots,
                            isLoading = false,
                            error = null,
                        )
                    }
                },
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun navigateToAddItemWithDraftSlot(rack: Rack, xRel: Float, yRel: Float) {
        _uiEvent.emit(
            RackDetailUiEvent.NavigateToAddItemDraft(
                rackId = rack.id,
                slotId = Uuid.random().toString(),
                slotXRel = xRel,
                slotYRel = yRel,
            ),
        )
    }
}

internal fun List<RackSlotMarkerVo>.findNearestSlotWithinOrNull(
    xRel: Float,
    yRel: Float,
    radiusRel: Float,
): RackSlotMarkerVo? =
    takeIf { list -> list.isNotEmpty() }?.let { slots ->
        val r2 = radiusRel * radiusRel
        slots
            .map { slot -> slot to slot.distanceSquared(xRel, yRel) }
            .filter { slotToDistance -> slotToDistance.second <= r2 }
            .minByOrNull { slotToDistance -> slotToDistance.second }
            ?.first
    }

private fun RackSlotMarkerVo.distanceSquared(otherXRel: Float, otherYRel: Float): Float {
    val dx = xRel - otherXRel
    val dy = yRel - otherYRel
    return dx * dx + dy * dy
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Rack details not found"
    is DomainError.Unknown -> "An unknown error occurred"
}
