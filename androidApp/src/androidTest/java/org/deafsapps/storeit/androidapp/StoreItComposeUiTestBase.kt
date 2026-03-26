package org.deafsapps.storeit.androidapp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation3.runtime.rememberNavBackStack
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createAndroidComposeExtension
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.deafsapps.storeit.presentation.search.viewmodel.SearchViewModel
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

            val backStack = rememberNavBackStack(initialScreen)

            val rackListViewModel: RackListViewModel = koinViewModel(
                viewModelStoreOwner = viewModelStoreOwner,
            )
            val addRackViewModel: AddRackViewModel = koinViewModel(
                viewModelStoreOwner = viewModelStoreOwner,
            )
            val searchViewModel: SearchViewModel = koinViewModel()

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    StoreItNavDisplay(
                        backStack = backStack,
                        rackListViewModel = rackListViewModel,
                        addRackViewModel = addRackViewModel,
                        searchViewModel = searchViewModel,
                        onRootBack = { },
                    )
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
