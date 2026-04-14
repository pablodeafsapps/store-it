package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.usecase.GetRackDataByRackIdUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.rack.mapper.toRackSlotMarkerVos
import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo
import org.deafsapps.storeit.presentation.rack.model.RackSlotPickerUiState
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SLOT_TAP_HIT_RADIUS_REL = 0.08f

@Factory
class RackSlotPickerViewModel(
    @InjectedParam private val rackId: String,
    coroutineScope: CoroutineScope?,
    private val getRackDataByRackIdUseCase: GetRackDataByRackIdUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState = MutableStateFlow(RackSlotPickerUiState.getDefault())
    val uiState: StateFlow<RackSlotPickerUiState> = _uiState.asStateFlow()

    init {
        loadRackDataById(rackId = rackId)
    }

    fun onImageTap(xRel: Float, yRel: Float) {
        val slots = _uiState.value.slots
        val existing = slots.findNearestSlotWithinOrNull(xRel = xRel, yRel = yRel, radiusRel = SLOT_TAP_HIT_RADIUS_REL)
        if (existing != null) {
            _uiState.update {
                it.copy(
                    selectedSlot = existing,
                    selectedPlacementType = SlotPlacementType.EXISTING,
                )
            }
            return
        }
        createDraftSlot(xRel = xRel, yRel = yRel)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun createDraftSlot(xRel: Float, yRel: Float) {
        val current = _uiState.value
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
        _uiState.update {
            it.copy(
                slots = mergedSlots,
                selectedSlot = draftSlot,
                selectedPlacementType = SlotPlacementType.DRAFT,
            )
        }
    }

    private fun loadRackDataById(rackId: String) {
        viewModelScope.launch {
            getRackDataByRackIdUseCase(input = rackId).fold(
                ifErr = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.toErrorCause(),
                        )
                    }
                },
                ifOk = { rackData ->
                    _uiState.update {
                        it.copy(
                            rack = rackData.rack,
                            slots = rackData.shelfSlots.toRackSlotMarkerVos(),
                            selectedSlot = null,
                            selectedPlacementType = null,
                            isLoading = false,
                            error = null,
                        )
                    }
                },
            )
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Rack details not found"
    is DomainError.Unknown -> message
}
