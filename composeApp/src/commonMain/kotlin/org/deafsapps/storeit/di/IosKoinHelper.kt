package org.deafsapps.storeit.di

import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object IosKoinHelper : KoinComponent {
    fun getAddRackViewModel(): AddRackViewModel = get<AddRackViewModel>()
    fun getRackListViewModel(): RackListViewModel = get<RackListViewModel>()
}
