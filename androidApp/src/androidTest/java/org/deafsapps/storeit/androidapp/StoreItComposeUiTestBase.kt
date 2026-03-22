package org.deafsapps.storeit.androidapp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createAndroidComposeExtension
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.presentation.item.ui.AddItemScreen
import org.deafsapps.storeit.androidapp.presentation.item.ui.ItemDetailScreen
import org.deafsapps.storeit.androidapp.presentation.item.ui.SlotItemsScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.AddRackScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackDetailScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackListScreen
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal abstract class StoreItComposeUiTestBase : KoinComponent {

    @JvmField
    @RegisterExtension
    @OptIn(ExperimentalTestApi::class)
    val composeExtension = createAndroidComposeExtension<ComponentActivity>()

    private val rackRepository: RackRepository by inject()
    private val slotRepository: SlotRepository by inject()
    private val itemRepository: ItemRepository by inject()

    @BeforeEach
    fun baseSetUp() {
        runTest {
            rackRepository.clear()
            slotRepository.clear()
            itemRepository.clear()
        }
    }

    protected fun composeUiTest(
        initialScreen: NavScreen = NavScreen.RackList,
        testBody: ComposeContext.() -> Unit,
    ) {
        composeExtension.use {
            renderApp(initialScreen)
            testBody()
        }
    }

    private fun ComposeContext.renderApp(
        initialScreen: NavScreen,
    ) {
        setContent {
            val viewModelStoreOwner = remember {
                object : ViewModelStoreOwner {
                    override val viewModelStore: ViewModelStore = ViewModelStore()
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    viewModelStoreOwner.viewModelStore.clear()
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var currentScreen: NavScreen by remember { mutableStateOf(initialScreen) }

                    val rackListViewModel: RackListViewModel = koinViewModel(
                        viewModelStoreOwner = viewModelStoreOwner,
                    )
                    val addRackViewModel: AddRackViewModel = koinViewModel(
                        viewModelStoreOwner = viewModelStoreOwner,
                    )

                    when (val screen = currentScreen) {
                        NavScreen.RackList -> {
                            val uiState by rackListViewModel.uiState.collectAsStateWithLifecycle()
                            RackListScreen(
                                uiState = uiState,
                                uiEvent = { rackListViewModel.uiEvent },
                                onAddRackSelect = rackListViewModel::onAddRackSelected,
                                onRackSelected = rackListViewModel::onRackSelected,
                                onNavigateToAddRack = { currentScreen = NavScreen.AddRack },
                                onNavigateToRackDetail = { id ->
                                    currentScreen = NavScreen.RackDetail(rackId = id)
                                },
                                onNavigateToAddItem = {
                                    currentScreen = NavScreen.AddItem()
                                },
                            )
                        }

                        NavScreen.AddRack -> {
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
                                    currentScreen = NavScreen.SlotItems(
                                        rackId = screen.rackId,
                                        slotId = screen.slotId,
                                    )
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

    protected fun seedRack(
        id: String,
        name: String,
    ) {
        runTest {
            rackRepository.saveRack(
                rack = Rack(
                    id = id,
                    name = name,
                ),
            )
        }
    }

    protected fun seedSlot(
        rackId: String,
        slotId: String,
        xRel: Float = 0.5f,
        yRel: Float = 0.5f,
    ) {
        runTest {
            slotRepository.saveSlot(
                slot = ShelfSlot(
                    id = slotId,
                    rackId = rackId,
                    position = SlotPosition(x = 0f, y = 0f, xRel = xRel, yRel = yRel),
                ),
            )
        }
    }

    protected fun seedItem(
        id: String,
        rackId: String,
        slotId: String,
        name: String,
    ) {
        runTest {
            itemRepository.saveItem(
                item = Item(
                    id = id,
                    rackId = rackId,
                    slotId = slotId,
                    name = name,
                ),
            )
        }
    }
}
