package org.deafsapps.storeit.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

object IosKoinHelper : KoinComponent {

    private val iosScope = CoroutineScope(Job())

    fun getRackListViewModel(): RackListViewModel =
        get(parameters = { parametersOf(iosScope) })

    fun getAddRackViewModel(): AddRackViewModel =
        get(parameters = { parametersOf(iosScope) })

    fun getRackDetailViewModel(rackId: String): RackDetailViewModel =
        get(parameters = { parametersOf(iosScope, rackId) })
}
