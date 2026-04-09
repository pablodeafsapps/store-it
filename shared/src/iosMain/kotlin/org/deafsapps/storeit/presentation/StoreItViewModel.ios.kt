package org.deafsapps.storeit.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.koin.core.annotation.Factory

@Factory
actual abstract class StoreItViewModel actual constructor(coroutineScope: CoroutineScope?) {

    actual val viewModelScope: CoroutineScope = MainScope()

    protected actual open fun onCleared() {
    }

    fun clear() {
        onCleared()
        viewModelScope.cancel()
    }
}