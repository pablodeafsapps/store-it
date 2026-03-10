package org.deafsapps.storeit.presentation

import kotlinx.coroutines.CoroutineScope

expect abstract class StoreItViewModel(coroutineScope: CoroutineScope? = null) {
    val viewModelScope: CoroutineScope
    protected open fun onCleared()
}
