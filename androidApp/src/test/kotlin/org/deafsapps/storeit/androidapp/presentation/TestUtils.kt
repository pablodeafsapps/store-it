package org.deafsapps.storeit.androidapp.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> TestScope.collectUiState(uiState: StateFlow<T>): List<T> {
    val states = mutableListOf<T>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        uiState.collect { state -> states.add(state) }
    }
    return states
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> TestScope.collectUiEvent(uiEvent: SharedFlow<T>): List<T> {
    val events = mutableListOf<T>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        uiEvent.collect { event -> events.add(event) }
    }
    return events
}
