package org.deafsapps.storeit.presentation

import kotlinx.coroutines.CoroutineScope
import androidx.lifecycle.ViewModel as AndroidXViewModel
import androidx.lifecycle.viewModelScope as androidXViewModelScope

actual abstract class StoreItViewModel actual constructor(coroutineScope: CoroutineScope?) : AndroidXViewModel() {
    actual val viewModelScope: CoroutineScope = coroutineScope ?: androidXViewModelScope

    actual override fun onCleared() {
        super.onCleared()
    }
}