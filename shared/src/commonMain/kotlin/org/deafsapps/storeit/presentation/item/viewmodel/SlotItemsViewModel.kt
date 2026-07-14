package org.deafsapps.storeit.presentation.item.viewmodel

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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.collections.immutable.persistentListOf
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.usecase.GetItemsBySlotInput
import org.deafsapps.storeit.domain.usecase.GetItemsBySlotUseCaseType
import org.deafsapps.storeit.presentation.mapper.toItemSummaryVos
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.item.model.SlotItemsUiEvent
import org.deafsapps.storeit.presentation.item.model.SlotItemsUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class SlotItemsViewModel(
    @InjectedParam private val rackId: String,
    @InjectedParam private val slotId: String,
    coroutineScope: CoroutineScope?,
    private val getItemsBySlotUseCase: GetItemsBySlotUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val loadRequests = MutableSharedFlow<SlotItemsLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SlotItemsUiState> = loadRequests
        .onStart { emit(value = SlotItemsLoadRequest.RefreshItems) }
        .flatMapLatest { request -> request.toStateChanges() }
        .runningFold(
            initial = SlotItemsUiState.getDefault(),
            operation = { state, change -> change.reduce(state = state) },
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = SlotItemsUiState.getDefault(),
        )

    private val _uiEvent = MutableSharedFlow<SlotItemsUiEvent?>()
    val uiEvent: SharedFlow<SlotItemsUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    fun refresh() {
        loadRequests.tryEmit(value = SlotItemsLoadRequest.RefreshItems)
    }

    private fun SlotItemsLoadRequest.toStateChanges(): Flow<SlotItemsStateChange> = flow {
        emit(value = SlotItemsStateChange.Loading)
        when (this@toStateChanges) {
            SlotItemsLoadRequest.RefreshItems ->
                getItemsBySlotUseCase(input = GetItemsBySlotInput(rackId = rackId, slotId = slotId)).fold(
                    ifErr = { error -> emit(value = SlotItemsStateChange.LoadFailed(error = error)) },
                    ifOk = { items -> emit(value = SlotItemsStateChange.ItemsLoaded(items = items)) },
                )
        }
    }

    private enum class SlotItemsLoadRequest {
        RefreshItems,
    }

    private sealed interface SlotItemsStateChange {
        fun reduce(state: SlotItemsUiState): SlotItemsUiState

        data object Loading : SlotItemsStateChange {
            override fun reduce(state: SlotItemsUiState): SlotItemsUiState =
                state.copy(isLoading = true, error = null)
        }

        data class ItemsLoaded(
            private val items: List<org.deafsapps.storeit.domain.model.Item>,
        ) : SlotItemsStateChange {
            override fun reduce(state: SlotItemsUiState): SlotItemsUiState =
                state.copy(isLoading = false, error = null, items = items.toItemSummaryVos())
        }

        data class LoadFailed(
            private val error: DomainError,
        ) : SlotItemsStateChange {
            override fun reduce(state: SlotItemsUiState): SlotItemsUiState =
                state.copy(
                    isLoading = false,
                    error = error.toErrorCause(),
                    items = persistentListOf(),
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
    is DomainError.NotFound -> "Slot items not found"
}
