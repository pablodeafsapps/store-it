package org.deafsapps.storeit.androidapp.presentation.item.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.presentation.item.viewmodel.SlotItemsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlotItemsScreen(
    rackId: String,
    slotId: String,
    onNavigateBack: () -> Unit,
    onAddItem: (rackId: String, slotId: String) -> Unit,
) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    val viewModel: SlotItemsViewModel = koinViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        parameters = { parametersOf(rackId, slotId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Items in this slot",
                        modifier = Modifier.testTag("slotItemsScreenTitle"),
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("slotItemsScreenBackButton"),
                    ) {
                        Text("Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("slotItemsScreenLoading"),
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(Dimens.screenPadding)
                            .testTag("slotItemsScreenError"),
                    )
                }
                uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.screenPadding),
                    ) {
                        Text(
                            text = "No items stored here.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("slotItemsScreenEmpty"),
                        )
                        Button(
                            onClick = { onAddItem(rackId, slotId) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .testTag("slotItemsScreenAddItemButton"),
                        ) {
                            Text("Add item")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("slotItemsScreenList"),
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .padding(Dimens.screenPadding)
                                    .testTag("slotItemRow_${item.id}"),
                            )
                        }
                        item {
                            Button(
                                onClick = { onAddItem(rackId, slotId) },
                                modifier = Modifier
                                    .padding(Dimens.screenPadding)
                                    .testTag("slotItemsScreenAddItemButton"),
                            ) {
                                Text("Add item")
                            }
                        }
                    }
                }
            }
        }
    }
}
