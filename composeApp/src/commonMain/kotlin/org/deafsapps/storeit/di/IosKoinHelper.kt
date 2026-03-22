package org.deafsapps.storeit.di

import org.deafsapps.storeit.presentation.item.viewmodel.AddItemViewModel
import org.deafsapps.storeit.presentation.item.viewmodel.ItemDetailViewModel
import org.deafsapps.storeit.presentation.item.viewmodel.SlotItemsViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

object IosKoinHelper : KoinComponent {

    fun getRackListViewModel(): RackListViewModel = get()

    fun getAddRackViewModel(): AddRackViewModel = get()

    fun getRackDetailViewModel(rackId: String): RackDetailViewModel =
        get(parameters = { parametersOf(rackId) })

    fun getAddItemViewModel(initialRackId: String?, initialSlotId: String?): AddItemViewModel =
        get(parameters = { parametersOf(initialRackId, initialSlotId) })

    fun getSlotItemsViewModel(rackId: String, slotId: String): SlotItemsViewModel =
        get(parameters = { parametersOf(rackId, slotId) })

    fun getItemDetailViewModel(itemId: String): ItemDetailViewModel =
        get(parameters = { parametersOf(itemId) })
}
