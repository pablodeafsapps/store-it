package org.deafsapps.storeit.presentation.item.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.usecase.AddItemUseCaseType
import org.deafsapps.storeit.domain.usecase.GetRacksFlowUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveSlotUseCaseType
import org.deafsapps.storeit.presentation.mapper.toRackSummaryVos
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.item.model.AddItemStep
import org.deafsapps.storeit.presentation.item.model.AddItemUiEvent
import org.deafsapps.storeit.presentation.item.model.AddItemUiState
import org.deafsapps.storeit.presentation.item.model.AddItemSlotVo
import org.deafsapps.storeit.presentation.rack.model.RackSummaryVo
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class AddItemViewModel(
    @InjectedParam private val initialRackId: String?,
    @InjectedParam private val addItemSlot: AddItemSlotVo,
    coroutineScope: CoroutineScope?,
    private val addItemUseCase: AddItemUseCaseType,
    private val getRacksFlowUseCase: GetRacksFlowUseCaseType,
    private val saveSlotUseCase: SaveSlotUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState: MutableStateFlow<AddItemUiState> = MutableStateFlow(
        AddItemUiState.getDefault(
            initialRackId = initialRackId,
            addItemSlot = addItemSlot,
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AddItemUiState> = _uiState
        .combine(getRacksStateFlow()) { state, racksState ->
            state.copy(
                racks = racksState.racks,
                error = racksState.error ?: state.error,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = AddItemUiState.getDefault(
                initialRackId = initialRackId,
                addItemSlot = addItemSlot,
            ),
        )

    private val _uiEvent = MutableSharedFlow<AddItemUiEvent?>()
    val uiEvent: SharedFlow<AddItemUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getRacksStateFlow() =
        getRacksFlowUseCase(input = Unit)
            .mapLatest { result ->
                result.fold(
                    ifErr = { error -> AddItemRacksState(racks = persistentListOf(), error = error.toErrorCause()) },
                    ifOk = { racks -> AddItemRacksState(racks = racks.toRackSummaryVos(), error = null) },
                )
            }

    fun onUpdateName(name: String) {
        _uiState.update { state -> state.copy(name = name, error = null) }
    }

    fun onUpdateDescription(description: String) {
        _uiState.update { state -> state.copy(description = description, error = null) }
    }

    fun onUpdateQuantity(quantity: Int?) {
        _uiState.update { state -> state.copy(quantity = quantity, error = null) }
    }

    fun onUpdateOwner(owner: String) {
        _uiState.update { state -> state.copy(owner = owner, error = null) }
    }

    fun onUpdateTagInput(tagInput: String) {
        _uiState.update { state -> state.copy(tagInput = tagInput, error = null) }
    }

    fun onAddTag() {
        val tag = _uiState.value.tagInput.trim()
        if (tag.isNotEmpty()) {
            _uiState.update { state ->
                val updatedTags: ImmutableList<String> = state.tags.toPersistentList().add(element = tag)
                state.copy(tags = updatedTags, tagInput = "", error = null)
            }
        }
    }

    fun onRemoveTag(tag: String) {
        _uiState.update { state ->
            val updatedTags: ImmutableList<String> =
                state.tags.filterNot { existingTag -> existingTag == tag }.toImmutableList()
            state.copy(tags = updatedTags)
        }
    }

    fun onUpdatePhotoUri(uri: String?) {
        _uiState.update { state -> state.copy(photoUri = uri, error = null) }
    }

    fun onSelectRackAndSlotSelected() {
        _uiState.update { state ->
            state.copy(step = if (state.selectedRackId != null) AddItemStep.SELECT_SLOT else AddItemStep.SELECT_RACK)
        }
    }

    fun onRackSelected(rack: RackSummaryVo) {
        _uiState.update { state -> state.copy(step = AddItemStep.SELECT_SLOT, selectedRackId = rack.id) }
    }

    fun onSlotSelectedForItem(rackId: String, slot: AddItemSlotVo) {
        _uiState.update { state ->
            state.copy(
                step = AddItemStep.FORM,
                selectedRackId = rackId,
                selectedSlotId = slot.id,
                selectedSlotPlacementType = slot.placementType,
                selectedSlotXRel = slot.xRel,
                selectedSlotYRel = slot.yRel,
            )
        }
    }

    fun onBackFromSelectRack() {
        _uiState.update { state -> state.copy(step = AddItemStep.FORM) }
    }

    fun onBackFromSelectSlot() {
        _uiState.update { state -> state.copy(step = AddItemStep.SELECT_RACK) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun onSaveItem() {
        val state = _uiState.value
        val rackId = state.selectedRackId
        val slotId = state.selectedSlotId
        if (rackId.isNullOrBlank() || slotId.isNullOrBlank()) {
            _uiState.update { s -> s.copy(error = "Select a rack and slot to place the item") }
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, error = null) }

            val item = Item(
                id = Uuid.random().toString(),
                rackId = rackId,
                slotId = slotId,
                name = state.name.trim().ifBlank { "Item" },
                description = state.description.trim(),
                photoUri = state.photoUri,
                quantity = state.quantity,
                owner = state.owner.trim(),
                tags = state.tags,
            )
            if (state.selectedSlotPlacementType == SlotPlacementType.DRAFT) {
                val xRel = state.selectedSlotXRel
                val yRel = state.selectedSlotYRel
                if (xRel == null || yRel == null) {
                    _uiState.update { state -> state.copy(isLoading = false, error = "Draft slot position is missing") }
                    return@launch
                }
                val slot = ShelfSlot(
                    id = slotId,
                    rackId = rackId,
                    position = SlotPosition(x = 0f, y = 0f, xRel = xRel, yRel = yRel),
                )
                saveSlotUseCase(input = slot).fold(
                    ifErr = { error ->
                        _uiState.update { state -> state.copy(isLoading = false, error = error.toErrorCause()) }
                    },
                    ifOk = { saveItemAndReset(item = item) },
                )
            } else {
                saveItemAndReset(item = item)
            }
        }
    }

    private suspend fun saveItemAndReset(item: Item) {
        addItemUseCase(item).fold(
            ifErr = { error: DomainError ->
                val message = when (error) {
                    is DomainError.AuthenticationFailed -> error.message
                    is DomainError.ServiceUnavailable -> error.message
                    is DomainError.ConfigurationError -> error.message
                    is DomainError.ValidationError -> error.reason
                    is DomainError.NotFound -> "Not found"
                    is DomainError.Unknown -> error.message
                }
                _uiState.update { state -> state.copy(isLoading = false, error = message) }
            },
            ifOk = {
                _uiEvent.emit(AddItemUiEvent.NavigateBack)
                _uiState.value = AddItemUiState.getDefault(
                    initialRackId = initialRackId,
                    addItemSlot = addItemSlot,
                )
            },
        )
    }
}

private data class AddItemRacksState(
    val racks: ImmutableList<RackSummaryVo>,
    val error: String?,
)

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.AuthenticationFailed,
    is DomainError.ServiceUnavailable,
    is DomainError.ConfigurationError,
    is DomainError.Unknown -> message
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Item not found"
}
