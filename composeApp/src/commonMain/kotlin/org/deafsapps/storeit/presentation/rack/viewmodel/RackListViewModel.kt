package org.deafsapps.storeit.presentation.rack.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.usecase.GetRacksUseCaseType
import org.deafsapps.storeit.presentation.rack.model.RackListUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackListUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Provided

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class RackListViewModel(
    @Provided private val coroutineScope: CoroutineScope,
    private val getRacksUseCase: GetRacksUseCaseType,
) {

    val uiState: StateFlow<RackListUiState> =
        flow {
            getRacksUseCase(input = Unit)
                .fold(ifErr = { error ->
                    emit(RackListUiState.getDefault().copy(error = error.toErrorCause()))
                }, ifOk = { racks ->
                    emit(RackListUiState.getDefault().copy(racks = racks))
                })
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = RackListUiState.getDefault().copy(isLoading = true),
        )

    private val _uiEvent = MutableSharedFlow<RackListUiEvent?>()
    val uiEvent: SharedFlow<RackListUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
            replay = 1,
        )

    fun onAddRackSelect() {
        coroutineScope.launch {
            _uiEvent.emit(RackListUiEvent.NavigateToAddRack)
        }
    }

    fun onRackSelect(rack: Rack) {
        coroutineScope.launch {
            _uiEvent.emit(RackListUiEvent.NavigateToRackDetail(rackId = rack.id))
        }
    }

    fun clear() {
        coroutineScope.cancel()
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Racks not found"
    is DomainError.Unknown -> "An unknown error occurred"
}
