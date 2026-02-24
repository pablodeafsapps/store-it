package org.deafsapps.storeit.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

internal actual fun createViewModelScope(): CoroutineScope =
    CoroutineScope(Job())
