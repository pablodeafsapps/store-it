package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.androidapp.design.backArrowIcon
import org.deafsapps.storeit.presentation.item.model.AddItemSlotVo
import org.deafsapps.storeit.presentation.item.model.toAddItemSlotVo
import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo
import org.deafsapps.storeit.presentation.rack.model.RackSlotPickerUiState
import org.deafsapps.storeit.presentation.rack.model.RackSummaryVo
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType
import org.deafsapps.storeit.presentation.rack.viewmodel.RackSlotPickerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RackSlotPickerScreen(
    rackId: String,
    onSlotSelectedForItem: (rackId: String, slot: AddItemSlotVo) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    val viewModel: RackSlotPickerViewModel = koinViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        parameters = { parametersOf(rackId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    RackSlotPickerContent(
        uiState = uiState,
        onSlotSelectedForItem = onSlotSelectedForItem,
        onNavigateBack = onNavigateBack,
        onImageTap = viewModel::onImageTap,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RackSlotPickerContent(
    uiState: RackSlotPickerUiState,
    onSlotSelectedForItem: (rackId: String, slot: AddItemSlotVo) -> Unit,
    onNavigateBack: () -> Unit,
    onImageTap: (Float, Float) -> Unit,
) {
    Scaffold(
        topBar = {
            val rackName = uiState.rack?.name ?: stringResource(R.string.rack_detail_title_default)
            TopAppBar(
                title = { Text(stringResource(R.string.rack_detail_title_select_slot, rackName)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("rackDetailBackButton")) {
                        Icon(
                            imageVector = backArrowIcon,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            val selectedSlot = uiState.selectedSlot
            val placementType = uiState.selectedPlacementType
            val rack = uiState.rack
            if (selectedSlot != null && placementType != null && rack != null) {
                Button(
                    onClick = {
                        onSlotSelectedForItem(
                            rack.id,
                            selectedSlot.toAddItemSlotVo(placementType = placementType),
                        )
                    },
                    modifier = Modifier.testTag("useThisSlotButton"),
                ) {
                    Text(stringResource(R.string.rack_detail_use_this_slot))
                }
            }
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
                        modifier = Modifier.align(Alignment.Center).testTag("rackSlotPickerLoading"),
                    )
                }
                uiState.rack == null -> {
                    Text(
                        text = uiState.error ?: stringResource(R.string.rack_not_found),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(Dimens.screenPadding),
                    )
                }
                else -> {
                    RackImageWithSlots(
                        photoUri = uiState.rack?.photoUri,
                        slots = uiState.slots,
                        selectedSlot = uiState.selectedSlot,
                        onTap = onImageTap,
                        onSlotMarkerDrag = { _, _, _ -> },
                        onSlotMarkerDragFinished = { _, _, _, _, _ -> },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RackSlotPickerLoadingPreview() {
    MaterialTheme {
        RackSlotPickerContent(
            uiState = RackSlotPickerUiState.getDefault().copy(isLoading = true),
            onSlotSelectedForItem = { _, _ -> },
            onNavigateBack = {},
            onImageTap = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RackSlotPickerSelectionPreview() {
    val selectedSlot = RackSlotMarkerVo(id = "slot-1", xRel = 0.35f, yRel = 0.4f)
    MaterialTheme {
        RackSlotPickerContent(
            uiState = RackSlotPickerUiState.getDefault().copy(
                rack = RackSummaryVo(
                    id = "rack-1",
                    name = "Garage shelf",
                    location = "Garage",
                    photoUri = null,
                ),
                slots = persistentListOf(
                    selectedSlot,
                    RackSlotMarkerVo(id = "slot-2", xRel = 0.7f, yRel = 0.5f),
                ),
                selectedSlot = selectedSlot,
                selectedPlacementType = SlotPlacementType.EXISTING,
                isLoading = false,
            ),
            onSlotSelectedForItem = { _, _ -> },
            onNavigateBack = {},
            onImageTap = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RackSlotPickerErrorPreview() {
    MaterialTheme {
        RackSlotPickerContent(
            uiState = RackSlotPickerUiState.getDefault().copy(
                isLoading = false,
                error = "Rack not found.",
            ),
            onSlotSelectedForItem = { _, _ -> },
            onNavigateBack = {},
            onImageTap = { _, _ -> },
        )
    }
}
