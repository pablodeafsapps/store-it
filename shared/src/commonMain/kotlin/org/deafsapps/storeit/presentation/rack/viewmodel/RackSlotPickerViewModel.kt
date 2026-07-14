package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.collections.immutable.toImmutableList
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.usecase.GetRackDataByRackIdUseCaseType
import org.deafsapps.storeit.presentation.mapper.toRackSummaryVo
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.rack.mapper.toRackSlotMarkerVos
import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo
import org.deafsapps.storeit.presentation.rack.model.RackSlotPickerUiState
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val SLOT_TAP_HIT_RADIUS_REL = 0.08f
private const val STATE_CHANGE_BUFFER_CAPACITY = 32

@Factory
class RackSlotPickerViewModel(
    @InjectedParam private val rackId: String,
    coroutineScope: CoroutineScope?,
    private val getRackDataByRackIdUseCase: GetRackDataByRackIdUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private var latestState = RackSlotPickerUiState.getDefault()

    private val loadRequests = MutableSharedFlow<RackSlotPickerLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateChanges = MutableSharedFlow<RackSlotPickerStateChange>(
        extraBufferCapacity = STATE_CHANGE_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RackSlotPickerUiState> = loadRequests
        .onStart { emit(value = RackSlotPickerLoadRequest.RefreshRackData) }
        .flatMapLatest { request -> request.toStateChanges() }
        .let { loadChanges -> listOf(loadChanges, stateChanges).merge() }
        .runningFold(
            initial = RackSlotPickerUiState.getDefault(),
            operation = { state, change ->
                change.reduce(state = state).also { latestState = it }
            },
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = RackSlotPickerUiState.getDefault(),
        )

    fun onImageTap(xRel: Float, yRel: Float) {
        val slots = latestState.slots
        val existing = slots.findNearestSlotWithinOrNull(xRel = xRel, yRel = yRel, radiusRel = SLOT_TAP_HIT_RADIUS_REL)
        if (existing != null) {
            enqueueStateChange(
                change = RackSlotPickerStateChange.SlotSelected(
                    slot = existing,
                    placementType = SlotPlacementType.EXISTING,
                ),
            )
            return
        }
        createDraftSlot(xRel = xRel, yRel = yRel)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun createDraftSlot(xRel: Float, yRel: Float) {
        val current = latestState
        val draftSlot = RackSlotMarkerVo(
            id = Uuid.random().toString(),
            xRel = xRel,
            yRel = yRel,
        )
        val mergedSlots = current.slots
            .filterNot {
                current.selectedPlacementType == SlotPlacementType.DRAFT &&
                current.selectedSlot?.id == it.id
            }
            .plus(draftSlot)
        enqueueStateChange(
            change = RackSlotPickerStateChange.DraftSlotCreated(
                slots = mergedSlots,
                slot = draftSlot,
            ),
        )
    }

    private fun RackSlotPickerLoadRequest.toStateChanges(): Flow<RackSlotPickerStateChange> = flow {
        emit(value = RackSlotPickerStateChange.Loading)
        when (this@toStateChanges) {
            RackSlotPickerLoadRequest.RefreshRackData ->
                getRackDataByRackIdUseCase(input = rackId).fold(
                    ifErr = { error -> emit(value = RackSlotPickerStateChange.LoadFailed(error = error)) },
                    ifOk = { rackData ->
                        emit(
                            value = RackSlotPickerStateChange.RackDataLoaded(
                                rack = rackData.rack,
                                slots = rackData.shelfSlots.toRackSlotMarkerVos(),
                            ),
                        )
                    },
                )
        }
    }

    private fun enqueueStateChange(change: RackSlotPickerStateChange) {
        latestState = change.reduce(state = latestState)
        stateChanges.tryEmit(value = change)
    }

    private enum class RackSlotPickerLoadRequest {
        RefreshRackData,
    }

    private sealed interface RackSlotPickerStateChange {
        fun reduce(state: RackSlotPickerUiState): RackSlotPickerUiState

        data object Loading : RackSlotPickerStateChange {
            override fun reduce(state: RackSlotPickerUiState): RackSlotPickerUiState =
                state.copy(isLoading = true, error = null)
        }

        data class RackDataLoaded(
            private val rack: org.deafsapps.storeit.domain.model.Rack,
            private val slots: List<RackSlotMarkerVo>,
        ) : RackSlotPickerStateChange {
            override fun reduce(state: RackSlotPickerUiState): RackSlotPickerUiState =
                state.copy(
                    rack = rack.toRackSummaryVo(),
                    slots = slots.toImmutableList(),
                    selectedSlot = null,
                    selectedPlacementType = null,
                    isLoading = false,
                    error = null,
                )
        }

        data class LoadFailed(
            private val error: DomainError,
        ) : RackSlotPickerStateChange {
            override fun reduce(state: RackSlotPickerUiState): RackSlotPickerUiState =
                state.copy(
                    isLoading = false,
                    error = error.toErrorCause(),
                )
        }

        data class SlotSelected(
            private val slot: RackSlotMarkerVo,
            private val placementType: SlotPlacementType,
        ) : RackSlotPickerStateChange {
            override fun reduce(state: RackSlotPickerUiState): RackSlotPickerUiState =
                state.copy(
                    selectedSlot = slot,
                    selectedPlacementType = placementType,
                )
        }

        data class DraftSlotCreated(
            private val slots: List<RackSlotMarkerVo>,
            private val slot: RackSlotMarkerVo,
        ) : RackSlotPickerStateChange {
            override fun reduce(state: RackSlotPickerUiState): RackSlotPickerUiState =
                state.copy(
                    slots = slots.toImmutableList(),
                    selectedSlot = slot,
                    selectedPlacementType = SlotPlacementType.DRAFT,
                )
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.AuthenticationFailed,
    is DomainError.ServiceUnavailable,
    is DomainError.ConfigurationError,
    is DomainError.Unknown -> message
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Rack details not found"
}
