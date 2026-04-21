package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
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
private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val SLOT_TAP_HIT_RADIUS_REL = 0.08f
private const val STATE_CHANGE_BUFFER_CAPACITY = 64

@Factory
class RackDetailViewModel(
    @InjectedParam private val rackId: String,
    coroutineScope: CoroutineScope?,
    private val getRackDataByRackIdUseCase: GetRackDataByRackIdUseCaseType,
    private val saveRackUseCase: SaveRackUseCaseType,
    private val deleteRackUseCase: DeleteRackUseCaseType,
    private val saveSlotUseCase: SaveSlotUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private var latestState = RackDetailUiState.getDefault()

    private val loadRequests = MutableSharedFlow<RackDetailLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateChanges = MutableSharedFlow<RackDetailStateChange>(
        extraBufferCapacity = STATE_CHANGE_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RackDetailUiState> = loadRequests
        .onStart { emit(value = RackDetailLoadRequest.RefreshRackData) }
        .flatMapLatest { request -> request.toStateChanges() }
        .let { loadChanges -> listOf(loadChanges, stateChanges).merge() }
        .runningFold(
            initial = RackDetailUiState.getDefault(),
            operation = { state, change ->
                change.reduce(state = state).also { latestState = it }
            },
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = RackDetailUiState.getDefault(),
        )

    private val _uiEvent = MutableSharedFlow<RackDetailUiEvent?>()
    val uiEvent: SharedFlow<RackDetailUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    fun onImageTap(xRel: Float, yRel: Float) {
        viewModelScope.launch {
            val rack = latestState.rack ?: return@launch
            val slots = latestState.slots
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
        val rack = latestState.rack ?: return
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
        if (latestState.rack == null) return null
        val boundedXRel = xRel.coerceIn(0f, 1f)
        val boundedYRel = yRel.coerceIn(0f, 1f)
        enqueueStateChange(
            change = RackDetailStateChange.SlotMarkersUpdated(
                slots = latestState.slots.map { slot ->
                    if (slot.id == slotId) slot.copy(xRel = boundedXRel, yRel = boundedYRel) else slot
                },
            ),
        )
        return boundedXRel to boundedYRel
    }

    fun onEditSelected() {
        val rack = latestState.rack ?: return
        enqueueStateChange(change = RackDetailStateChange.EditDialogOpened(rack = rack))
    }

    fun onUpdateEditName(name: String) {
        enqueueStateChange(change = RackDetailStateChange.EditNameUpdated(name = name))
    }

    fun onUpdateEditDescription(description: String) {
        enqueueStateChange(change = RackDetailStateChange.EditDescriptionUpdated(description = description))
    }

    fun onUpdateEditLocation(location: String) {
        enqueueStateChange(change = RackDetailStateChange.EditLocationUpdated(location = location))
    }

    fun onDismissEditDialog() {
        enqueueStateChange(change = RackDetailStateChange.EditDialogDismissed)
    }

    fun onSaveRackEdits() {
        val state = latestState
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
                    emitStateChange(change = RackDetailStateChange.RackEditsSaved(rack = updatedRack))
                },
            )
        }
    }

    fun onRemoveRackSelected() {
        enqueueStateChange(change = RackDetailStateChange.DeleteConfirmVisibilityChanged(isVisible = true))
    }

    fun onDismissDeleteConfirm() {
        enqueueStateChange(change = RackDetailStateChange.DeleteConfirmVisibilityChanged(isVisible = false))
    }

    fun onConfirmDeleteRack() {
        viewModelScope.launch {
            deleteRackUseCase(input = rackId).fold(
                ifErr = { error -> _uiEvent.emit(RackDetailUiEvent.ShowError(message = error.toErrorCause())) },
                ifOk = {
                    emitStateChange(change = RackDetailStateChange.DeleteConfirmVisibilityChanged(isVisible = false))
                    _uiEvent.emit(RackDetailUiEvent.NavigateBack)
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

    private fun RackDetailLoadRequest.toStateChanges(): Flow<RackDetailStateChange> = flow {
        emit(value = RackDetailStateChange.Loading)
        when (this@toStateChanges) {
            RackDetailLoadRequest.RefreshRackData ->
                getRackDataByRackIdUseCase(input = rackId).fold(
                    ifErr = { error -> emit(value = RackDetailStateChange.LoadFailed(error = error)) },
                    ifOk = { rackData ->
                        emit(
                            value = RackDetailStateChange.RackDataLoaded(
                                rack = rackData.rack,
                                slots = rackData.shelfSlots.toRackSlotMarkerVos(),
                            ),
                        )
                    },
                )
        }
    }

    private fun enqueueStateChange(change: RackDetailStateChange) {
        latestState = change.reduce(state = latestState)
        stateChanges.tryEmit(value = change)
    }

    private suspend fun emitStateChange(change: RackDetailStateChange) {
        latestState = change.reduce(state = latestState)
        stateChanges.emit(value = change)
    }

    private enum class RackDetailLoadRequest {
        RefreshRackData,
    }

    private sealed interface RackDetailStateChange {
        fun reduce(state: RackDetailUiState): RackDetailUiState

        data object Loading : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(isLoading = true, error = null)
        }

        data class RackDataLoaded(
            private val rack: Rack,
            private val slots: List<RackSlotMarkerVo>,
        ) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(
                    rack = rack,
                    slots = slots,
                    isLoading = false,
                    error = null,
                )
        }

        data class LoadFailed(
            private val error: DomainError,
        ) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(
                    isLoading = false,
                    error = error.toErrorCause(),
                )
        }

        data class SlotMarkersUpdated(
            private val slots: List<RackSlotMarkerVo>,
        ) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(slots = slots)
        }

        data class EditDialogOpened(
            private val rack: Rack,
        ) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(
                    showEditDialog = true,
                    editName = rack.name,
                    editDescription = rack.description,
                    editLocation = rack.location,
                )
        }

        data class EditNameUpdated(private val name: String) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(editName = name)
        }

        data class EditDescriptionUpdated(private val description: String) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(editDescription = description)
        }

        data class EditLocationUpdated(private val location: String) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(editLocation = location)
        }

        data object EditDialogDismissed : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(showEditDialog = false)
        }

        data class DeleteConfirmVisibilityChanged(
            private val isVisible: Boolean,
        ) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(showDeleteConfirm = isVisible)
        }

        data class RackEditsSaved(
            private val rack: Rack,
        ) : RackDetailStateChange {
            override fun reduce(state: RackDetailUiState): RackDetailUiState =
                state.copy(rack = rack, showEditDialog = false)
        }
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
    is DomainError.Unknown -> message
}
