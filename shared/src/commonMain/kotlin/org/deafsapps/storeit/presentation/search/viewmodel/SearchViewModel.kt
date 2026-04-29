package org.deafsapps.storeit.presentation.search.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.usecase.SearchItemsUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.search.model.SearchUiState
import org.koin.core.annotation.Factory

private const val SEARCH_DEBOUNCE_MS = 300L

@Factory
class SearchViewModel(
    coroutineScope: CoroutineScope?,
    private val searchItemsUseCase: SearchItemsUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState = MutableStateFlow(SearchUiState.getDefault())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { state -> state.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { state ->
                state.copy(isLoading = false, results = emptyList(), error = null)
            }
            return
        }
        _uiState.update { state -> state.copy(isLoading = true, error = null) }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchItemsUseCase(input = query.trim()).fold(
                ifErr = { error ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, error = error.toErrorCause(), results = emptyList())
                    }
                },
                ifOk = { results ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, error = null, results = results)
                    }
                },
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
    is DomainError.NotFound -> "No items found"
}
