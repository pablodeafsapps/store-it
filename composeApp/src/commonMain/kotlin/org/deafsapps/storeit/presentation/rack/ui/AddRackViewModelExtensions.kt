package org.deafsapps.storeit.ui.rack

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

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
