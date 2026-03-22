package org.deafsapps.storeit.presentation.item.viewmodel

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
import org.deafsapps.storeit.domain.usecase.GetItemsBySlotInput
import org.deafsapps.storeit.domain.usecase.GetItemsBySlotUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.item.model.SlotItemsUiEvent
import org.deafsapps.storeit.presentation.item.model.SlotItemsUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class SlotItemsViewModel(
    @InjectedParam private val rackId: String,
    @InjectedParam private val slotId: String,
    coroutineScope: CoroutineScope?,
    private val getItemsBySlotUseCase: GetItemsBySlotUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState = MutableStateFlow(SlotItemsUiState.getDefault())
    val uiState: StateFlow<SlotItemsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SlotItemsUiEvent?>()
    val uiEvent: SharedFlow<SlotItemsUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getItemsBySlotUseCase(input = GetItemsBySlotInput(rackId = rackId, slotId = slotId)).fold(
                ifErr = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.toErrorCause(),
                            items = emptyList(),
                        )
                    }
                },
                ifOk = { items ->
                    _uiState.update {
                        it.copy(isLoading = false, error = null, items = items)
                    }
                },
            )
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Slot items not found"
    is DomainError.Unknown -> "An unknown error occurred"
}
