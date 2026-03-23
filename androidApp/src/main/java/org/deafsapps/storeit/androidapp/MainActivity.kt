package org.deafsapps.storeit.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.koin.androidx.viewmodel.ext.android.viewModel

sealed interface NavScreen {
    data object RackList : NavScreen
    data object Search : NavScreen
    data object AddRack : NavScreen
    data class RackDetail(val rackId: String) : NavScreen
    data class SlotItems(val rackId: String, val slotId: String) : NavScreen
    data class ItemDetail(
        val itemId: String,
        val rackId: String,
        val slotId: String,
        val fromSearch: Boolean = false,
    ) : NavScreen
    data class AddItem(val rackId: String? = null, val slotId: String? = null) : NavScreen
}

class MainActivity : ComponentActivity() {

    private val rackListViewModel: RackListViewModel by viewModel()
    private val addRackViewModel: AddRackViewModel by viewModel()
    private val searchViewModel: SearchViewModel by viewModel()

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<NavScreen>(NavScreen.RackList) }
                    when (val screen = currentScreen) {
                        is NavScreen.RackList -> {
                            val uiState by rackListViewModel.uiState.collectAsStateWithLifecycle()
                            RackListScreen(
                                uiState = uiState,
                                uiEvent = { rackListViewModel.uiEvent },
                                onAddRackSelect = rackListViewModel::onAddRackSelected,
                                onRackSelected = rackListViewModel::onRackSelected,
                                onNavigateToAddRack = { currentScreen = NavScreen.AddRack },
                                onNavigateToRackDetail = { id -> currentScreen = NavScreen.RackDetail(id) },
                                onNavigateToAddItem = { currentScreen = NavScreen.AddItem() },
                                onNavigateToSearch = { currentScreen = NavScreen.Search },
                            )
                        }
                        is NavScreen.Search -> {
                            val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
                            SearchScreen(
                                uiState = uiState,
                                onQueryChange = searchViewModel::onQueryChange,
                                onItemSelected = { placement ->
                                    currentScreen = NavScreen.ItemDetail(
                                        itemId = placement.item.id,
                                        rackId = placement.item.rackId,
                                        slotId = placement.item.slotId,
                                        fromSearch = true,
                                    )
                                },
                                onNavigateBack = { currentScreen = NavScreen.RackList },
                            )
                        }
                        is NavScreen.AddRack -> {
                            val uiState by addRackViewModel.uiState.collectAsStateWithLifecycle()
                            AddRackScreen(
                                uiState = uiState,
                                uiEvent = { addRackViewModel.uiEvent },
                                onUpdatePhotoUri = addRackViewModel::onUpdatePhotoUri,
                                onUpdateName = addRackViewModel::onUpdateName,
                                onUpdateDescription = addRackViewModel::onUpdateDescription,
                                onUpdateLocation = addRackViewModel::onUpdateLocation,
                                onSaveRack = addRackViewModel::onSaveRack,
                                onNavigateBack = {
                                    addRackViewModel.onNavigateBack()
                                    currentScreen = NavScreen.RackList
                                                 },
                            )
                        }
                        is NavScreen.RackDetail -> {
                            RackDetailScreen(
                                rackId = screen.rackId,
                                onNavigateBack = { currentScreen = NavScreen.RackList },
                                onAddItemHere = { rackId, slotId ->
                                    currentScreen = NavScreen.AddItem(rackId = rackId, slotId = slotId)
                                },
                                onNavigateToSlotItems = { rackId, slotId ->
                                    currentScreen = NavScreen.SlotItems(rackId = rackId, slotId = slotId)
                                },
                            )
                        }
                        is NavScreen.SlotItems -> {
                            SlotItemsScreen(
                                rackId = screen.rackId,
                                slotId = screen.slotId,
                                onNavigateBack = { currentScreen = NavScreen.RackDetail(screen.rackId) },
                                onAddItem = { rackId, slotId ->
                                    currentScreen = NavScreen.AddItem(rackId = rackId, slotId = slotId)
                                },
                                onItemSelected = { itemId ->
                                    currentScreen = NavScreen.ItemDetail(
                                        itemId = itemId,
                                        rackId = screen.rackId,
                                        slotId = screen.slotId,
                                    )
                                },
                            )
                        }
                        is NavScreen.ItemDetail -> {
                            ItemDetailScreen(
                                itemId = screen.itemId,
                                onNavigateBack = {
                                    currentScreen = if (screen.fromSearch) {
                                        NavScreen.Search
                                    } else {
                                        NavScreen.SlotItems(
                                            rackId = screen.rackId,
                                            slotId = screen.slotId,
                                        )
                                    }
                                },
                            )
                        }
                        is NavScreen.AddItem -> {
                            AddItemScreen(
                                initialRackId = screen.rackId,
                                initialSlotId = screen.slotId,
                                onNavigateBack = { currentScreen = NavScreen.RackList },
                                onNavigateToAddRack = { currentScreen = NavScreen.AddRack },
                            )
                        }
                    }
                }
            }
        }
    }
}
