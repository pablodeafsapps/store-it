package org.deafsapps.storeit.presentation.item.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.AddItemUseCaseType
import org.deafsapps.storeit.domain.usecase.GetRacksFlowUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.item.model.AddItemStep
import org.deafsapps.storeit.presentation.item.model.AddItemUiEvent
import org.deafsapps.storeit.presentation.item.model.AddItemUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class AddItemViewModel(
    @InjectedParam private val initialRackId: String?,
    @InjectedParam private val initialSlotId: String?,
    coroutineScope: CoroutineScope?,
    private val addItemUseCase: AddItemUseCaseType,
    private val getRacksFlowUseCase: GetRacksFlowUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState = MutableStateFlow(
        AddItemUiState.getDefault(initialRackId = initialRackId, initialSlotId = initialSlotId)
    )
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = AddItemUiState.getDefault(initialRackId = initialRackId, initialSlotId = initialSlotId),
        )
    private val _uiEvent = MutableSharedFlow<AddItemUiEvent?>()
    val uiEvent: SharedFlow<AddItemUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    init {
        startRacksFlowWhenSelectingRack()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startRacksFlowWhenSelectingRack() {
            getRacksFlowUseCase(input = Unit)
                .mapLatest { result ->
                    result.fold(ifErr = { error ->
                        _uiState.value.copy(racks = emptyList(), error = error.toErrorCause())
                    }, ifOk = { racks ->
                        _uiState.value.copy(racks = racks, error = null)
                    })
            }.onEach { newState ->
                _uiState.update { state ->
                    state.copy(racks = newState.racks, error = newState.error ?: state.error)
                }
            }.launchIn(viewModelScope)
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
                state.copy(tags = state.tags + tag, tagInput = "", error = null)
            }
        }
    }

    fun onRemoveTag(tag: String) {
        _uiState.update { state -> state.copy(tags = state.tags - tag) }
    }

    fun onUpdatePhotoUri(uri: String?) {
        _uiState.update { state -> state.copy(photoUri = uri, error = null) }
    }

    fun onSelectRackAndSlotSelect() {
        _uiState.update { state ->
            state.copy(step = if (state.selectedRackId != null) AddItemStep.SELECT_SLOT else AddItemStep.SELECT_RACK)
        }
    }

    fun onRackSelected(rack: Rack) {
        _uiState.update { state -> state.copy(step = AddItemStep.SELECT_SLOT, selectedRackId = rack.id) }
    }

    fun onSlotSelectedForItem(rackId: String, slotId: String) {
        _uiState.update { state ->
            state.copy(
                step = AddItemStep.FORM,
                selectedRackId = rackId,
                selectedSlotId = slotId,
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
            _uiState.update { state -> state.copy(error = "Select a rack and slot to place the item") }
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

            addItemUseCase(item).fold(
                ifErr = { error: DomainError ->
                    val message = when (error) {
                        is DomainError.ValidationError -> error.reason
                        is DomainError.NotFound -> "Not found"
                        is DomainError.Unknown -> "An unknown error occurred"
                    }
                    _uiState.update { state -> state.copy(isLoading = false, error = message) }
                },
                ifOk = {
                    _uiState.update { state ->
                        state.copy(isLoading = false, isSuccess = true)
                    }
                    _uiEvent.emit(AddItemUiEvent.NavigateBack)
                },
            )
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Not found"
    is DomainError.Unknown -> "An unknown error occurred"
}
