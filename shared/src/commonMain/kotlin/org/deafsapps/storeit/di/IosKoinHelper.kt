package org.deafsapps.storeit.di

import org.deafsapps.storeit.data.model.DebugMockDataPreloader
import org.deafsapps.storeit.presentation.account.viewmodel.AccountViewModel
import org.deafsapps.storeit.presentation.item.model.AddItemSlotVo
import org.deafsapps.storeit.presentation.item.viewmodel.AddItemViewModel
import org.deafsapps.storeit.presentation.item.viewmodel.ItemDetailViewModel
import org.deafsapps.storeit.presentation.item.viewmodel.SlotItemsViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackSlotPickerViewModel
import org.deafsapps.storeit.presentation.search.viewmodel.SearchViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

object IosKoinHelper : KoinComponent {

    suspend fun preloadDebugMockDataIfNeeded(isDebugBuild: Boolean) {
        if (!isDebugBuild) return

        get<DebugMockDataPreloader>().preloadIfEmpty()
    }

    fun getRackListViewModel(): RackListViewModel = get()

    fun getAddRackViewModel(): AddRackViewModel = get()

    fun getRackDetailViewModel(rackId: String): RackDetailViewModel =
        get(parameters = { parametersOf(rackId) })

    fun getRackSlotPickerViewModel(rackId: String): RackSlotPickerViewModel =
        get(parameters = { parametersOf(rackId) })

    fun getAddItemViewModel(
        initialRackId: String?,
        addItemSlot: AddItemSlotVo,
    ): AddItemViewModel = get(
        parameters = { parametersOf(initialRackId, addItemSlot) },
    )

    fun getSlotItemsViewModel(rackId: String, slotId: String): SlotItemsViewModel =
        get(parameters = { parametersOf(rackId, slotId) })

    fun getItemDetailViewModel(itemId: String): ItemDetailViewModel =
        get(parameters = { parametersOf(itemId) })

    fun getSearchViewModel(): SearchViewModel = get()

    fun getAccountViewModel(): AccountViewModel = get()
}
