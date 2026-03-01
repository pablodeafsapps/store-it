package org.deafsapps.storeit.presentation.rack.ui

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T> StateFlow<T>.observe(
    scope: CoroutineScope,
    onValue: (T) -> Unit
) {
    scope.launch {
        collect { value ->
            onValue(value)
        }
    }
}
