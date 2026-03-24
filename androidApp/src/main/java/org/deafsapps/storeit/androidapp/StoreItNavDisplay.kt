package org.deafsapps.storeit.androidapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.deafsapps.storeit.androidapp.presentation.item.ui.AddItemScreen
import org.deafsapps.storeit.androidapp.presentation.item.ui.ItemDetailScreen
import org.deafsapps.storeit.androidapp.presentation.item.ui.SlotItemsScreen
import org.deafsapps.storeit.androidapp.presentation.search.ui.SearchScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.AddRackScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackDetailScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackListScreen
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.deafsapps.storeit.presentation.search.viewmodel.SearchViewModel

@Composable
internal fun StoreItNavDisplay(
    backStack: NavBackStack<NavKey>,
    rackListViewModel: () -> RackListViewModel,
    addRackViewModel: () -> AddRackViewModel,
    searchViewModel: () -> SearchViewModel,
    onRootBack: () -> Unit,
) {
    BackHandler {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            onRootBack()
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider =
            entryProvider(
                fallback = { key ->
                    NavEntry(key) { route ->
                        Text("Unknown route: $route")
                    }
                },
            ) {
                entry<NavScreen.RackList> {
                    RackListNavContent(
                        backStack = backStack,
                        rackListViewModel = rackListViewModel,
                    )
                }
                entry<NavScreen.Search> {
                    SearchNavContent(
                        backStack = backStack,
                        searchViewModel = searchViewModel,
                    )
                }
                entry<NavScreen.AddRack> {
                    AddRackNavContent(
                        backStack = backStack,
                        addRackViewModel = addRackViewModel,
                    )
                }
                entry<NavScreen.RackDetail> { screen ->
                    RackDetailScreen(
                        rackId = screen.rackId,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                        onAddItemHere = { rackId, slotId ->
                            backStack.add(NavScreen.AddItem(rackId = rackId, slotId = slotId))
                        },
                        onNavigateToSlotItems = { rackId, slotId ->
                            backStack.add(NavScreen.SlotItems(rackId = rackId, slotId = slotId))
                        },
                    )
                }
                entry<NavScreen.SlotItems> { screen ->
                    SlotItemsScreen(
                        rackId = screen.rackId,
                        slotId = screen.slotId,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                        onAddItem = { rackId, slotId ->
                            backStack.add(NavScreen.AddItem(rackId = rackId, slotId = slotId))
                        },
                        onItemSelected = { itemId ->
                            backStack.add(
                                NavScreen.ItemDetail(
                                    itemId = itemId,
                                    rackId = screen.rackId,
                                    slotId = screen.slotId,
                                ),
                            )
                        },
                    )
                }
                entry<NavScreen.ItemDetail> { screen ->
                    ItemDetailScreen(
                        itemId = screen.itemId,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
                entry<NavScreen.AddItem> { screen ->
                    AddItemScreen(
                        initialRackId = screen.rackId,
                        initialSlotId = screen.slotId,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                        onNavigateToAddRack = { backStack.add(NavScreen.AddRack) },
                    )
                }
            },
    )
}

@Composable
private fun RackListNavContent(
    backStack: NavBackStack<NavKey>,
    rackListViewModel: () -> RackListViewModel,
) {
    val uiState by rackListViewModel().uiState.collectAsStateWithLifecycle()
    RackListScreen(
        uiState = uiState,
        uiEvent = { rackListViewModel().uiEvent },
        onAddRackSelect = rackListViewModel()::onAddRackSelected,
        onRackSelected = rackListViewModel()::onRackSelected,
        onNavigateToAddRack = { backStack.add(NavScreen.AddRack) },
        onNavigateToRackDetail = { id -> backStack.add(NavScreen.RackDetail(id)) },
        onNavigateToAddItem = { backStack.add(NavScreen.AddItem()) },
        onNavigateToSearch = { backStack.add(NavScreen.Search) },
    )
}

@Composable
private fun SearchNavContent(
    backStack: NavBackStack<NavKey>,
    searchViewModel: () -> SearchViewModel,
) {
    val uiState by searchViewModel().uiState.collectAsStateWithLifecycle()
    SearchScreen(
        uiState = uiState,
        onQueryChange = searchViewModel()::onQueryChange,
        onItemSelected = { placement ->
            backStack.add(
                NavScreen.ItemDetail(
                    itemId = placement.item.id,
                    rackId = placement.item.rackId,
                    slotId = placement.item.slotId,
                    fromSearch = true,
                ),
            )
        },
        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
    )
}

@Composable
private fun AddRackNavContent(
    backStack: NavBackStack<NavKey>,
    addRackViewModel: () -> AddRackViewModel,
) {
    val uiState by addRackViewModel().uiState.collectAsStateWithLifecycle()
    AddRackScreen(
        uiState = uiState,
        uiEvent = { addRackViewModel().uiEvent },
        onUpdatePhotoUri = addRackViewModel()::onUpdatePhotoUri,
        onUpdateName = addRackViewModel()::onUpdateName,
        onUpdateDescription = addRackViewModel()::onUpdateDescription,
        onUpdateLocation = addRackViewModel()::onUpdateLocation,
        onSaveRack = addRackViewModel()::onSaveRack,
        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
    )
}
