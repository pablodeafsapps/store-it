package org.deafsapps.storeit.androidapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
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
import org.deafsapps.storeit.androidapp.presentation.account.ui.AccountScreen
import org.deafsapps.storeit.androidapp.presentation.search.ui.SearchScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.AddRackScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackBrowseScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackListScreen
import org.deafsapps.storeit.presentation.account.viewmodel.AccountViewModel
import org.deafsapps.storeit.presentation.item.model.AddItemSlotVo
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.deafsapps.storeit.presentation.search.viewmodel.SearchViewModel
import org.deafsapps.storeit.presentation.sync.viewmodel.SyncStatusViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun StoreItNavDisplay(
    backStack: NavBackStack<NavKey>,
    onRootBack: () -> Unit,
    rackListViewModel: () -> RackListViewModel,
    isDarkModeEnabled: Boolean,
    onThemeModeToggle: () -> Unit,
) {
    val accountViewModel: AccountViewModel = koinViewModel()
    val syncStatusViewModel: SyncStatusViewModel = koinViewModel()

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
                        Text(stringResource(R.string.nav_unknown_route, route.toString()))
                    }
                },
            ) {
                entry<NavScreen.RackList> {
                    RackListNavContent(
                        backStack = backStack,
                        rackListViewModel = rackListViewModel,
                        accountViewModel = accountViewModel,
                        syncStatusViewModel = syncStatusViewModel,
                        isDarkModeEnabled = isDarkModeEnabled,
                        onThemeModeToggle = onThemeModeToggle,
                    )
                }
                entry<NavScreen.Search> {
                    SearchNavContent(
                        backStack = backStack,
                    )
                }
                entry<NavScreen.Account> {
                    AccountNavContent(
                        backStack = backStack,
                        accountViewModel = accountViewModel,
                        syncStatusViewModel = syncStatusViewModel,
                    )
                }
                entry<NavScreen.AddRack> {
                    AddRackNavContent(
                        backStack = backStack,
                    )
                }
                entry<NavScreen.RackDetail> { screen ->
                    RackBrowseScreen(
                        rackId = screen.rackId,
                        onAddItemHere = { rackId, slotId, slotXRel, slotYRel ->
                            backStack.add(
                                NavScreen.AddItemAtDraftSlot(
                                    rackId = rackId,
                                    slotId = slotId,
                                    slotXRel = slotXRel,
                                    slotYRel = slotYRel,
                                )
                            )
                        },
                        onNavigateToSlotItems = { rackId, slotId ->
                            backStack.add(NavScreen.SlotItems(rackId = rackId, slotId = slotId))
                        },
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
                entry<NavScreen.SlotItems> { screen ->
                    SlotItemsScreen(
                        rackId = screen.rackId,
                        slotId = screen.slotId,
                        onAddItem = { rackId, slotId ->
                            backStack.add(NavScreen.AddItemAtSlot(rackId = rackId, slotId = slotId))
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
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
                entry<NavScreen.ItemDetail> { screen ->
                    ItemDetailScreen(
                        itemId = screen.itemId,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
                entry<NavScreen.AddItem> {
                    AddItemScreen(
                        initialRackId = null,
                        addItemSlot = AddItemSlotVo.None,
                        onNavigateToAddRack = { backStack.add(NavScreen.AddRack) },
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
                entry<NavScreen.AddItemAtSlot> { screen ->
                    AddItemScreen(
                        initialRackId = screen.rackId,
                        addItemSlot = AddItemSlotVo(
                            id = screen.slotId,
                            placementType = SlotPlacementType.EXISTING,
                        ),
                        onNavigateToAddRack = { backStack.add(NavScreen.AddRack) },
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
                entry<NavScreen.AddItemAtDraftSlot> { screen ->
                    AddItemScreen(
                        initialRackId = screen.rackId,
                        addItemSlot = AddItemSlotVo(
                            id = screen.slotId,
                            placementType = SlotPlacementType.DRAFT,
                            xRel = screen.slotXRel,
                            yRel = screen.slotYRel,
                        ),
                        onNavigateToAddRack = { backStack.add(NavScreen.AddRack) },
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            },
    )
}

@Composable
private fun RackListNavContent(
    backStack: NavBackStack<NavKey>,
    rackListViewModel: () -> RackListViewModel,
    accountViewModel: AccountViewModel,
    syncStatusViewModel: SyncStatusViewModel,
    isDarkModeEnabled: Boolean,
    onThemeModeToggle: () -> Unit,
) {
    val uiState by rackListViewModel().uiState.collectAsStateWithLifecycle()
    val accountUiState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val syncUiState by syncStatusViewModel.uiState.collectAsStateWithLifecycle()
    RackListScreen(
        uiState = uiState,
        uiEvent = { rackListViewModel().uiEvent },
        onAddRackSelect = rackListViewModel()::onAddRackSelected,
        onRackSelected = rackListViewModel()::onRackSelected,
        onNavigateToAddRack = { backStack.add(NavScreen.AddRack) },
        onNavigateToRackDetail = { id -> backStack.add(NavScreen.RackDetail(id)) },
        onNavigateToAddItem = { backStack.add(NavScreen.AddItem) },
        onNavigateToSearch = { backStack.add(NavScreen.Search) },
        onNavigateToAccount = { backStack.add(NavScreen.Account) },
        isAccountAuthenticated = accountUiState.isAuthenticated,
        accountEmail = accountUiState.accountEmail,
        isAccountReady = syncUiState.isDataBackedUp,
        isRestoreInProgress = syncUiState.isRestoreInProgress,
        hasPendingSyncWork = syncUiState.hasPendingWork,
        hasAccountAttentionState = syncUiState.hasAttentionState,
        isDarkModeEnabled = isDarkModeEnabled,
        onThemeModeToggle = onThemeModeToggle,
    )
}

@Composable
private fun AccountNavContent(
    backStack: NavBackStack<NavKey>,
    accountViewModel: AccountViewModel,
    syncStatusViewModel: SyncStatusViewModel,
) {
    val accountUiState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val syncUiState by syncStatusViewModel.uiState.collectAsStateWithLifecycle()

    AccountScreen(
        accountUiState = accountUiState,
        syncUiState = syncUiState,
        onSelectSignIn = accountViewModel::selectSignInMode,
        onSelectSignUp = accountViewModel::selectSignUpMode,
        onEmailChange = accountViewModel::onEmailInputChanged,
        onPasswordChange = accountViewModel::onPasswordInputChanged,
        onSubmitCredentials = accountViewModel::submitCredentials,
        onSignOut = accountViewModel::signOut,
        onRetryRestore = syncStatusViewModel::retry,
        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
    )
}

@Composable
private fun SearchNavContent(
    backStack: NavBackStack<NavKey>,
) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    val searchViewModel: SearchViewModel = koinViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
    )
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    SearchScreen(
        uiState = uiState,
        onQueryChange = searchViewModel::onQueryChange,
        onItemSelected = { result ->
            backStack.add(
                NavScreen.ItemDetail(
                    itemId = result.itemId,
                    rackId = result.rackId,
                    slotId = result.slotId,
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
) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    val addRackViewModel: AddRackViewModel = koinViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
    )
    val uiState by addRackViewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    AddRackScreen(
        uiState = uiState,
        uiEvent = { addRackViewModel.uiEvent },
        onUpdatePhotoUri = addRackViewModel::onUpdatePhotoUri,
        onUpdateName = addRackViewModel::onUpdateName,
        onUpdateDescription = addRackViewModel::onUpdateDescription,
        onUpdateLocation = addRackViewModel::onUpdateLocation,
        onSaveRack = addRackViewModel::onSaveRack,
        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
    )
}
