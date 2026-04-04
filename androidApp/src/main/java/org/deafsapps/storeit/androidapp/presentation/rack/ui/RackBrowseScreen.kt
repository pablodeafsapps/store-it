package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.androidapp.design.backArrowIcon
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiState
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun RackBrowseScreen(
    rackId: String,
    onNavigateBack: () -> Unit,
    onAddItemHere: (rackId: String, slotId: String, slotXRel: Float, slotYRel: Float) -> Unit,
    onNavigateToSlotItems: (rackId: String, slotId: String) -> Unit,
) {
    val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    val viewModel: RackDetailViewModel =
        koinViewModel(viewModelStoreOwner = owner, parameters = { parametersOf(rackId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is RackDetailUiEvent.NavigateBack -> onNavigateBack()
                    is RackDetailUiEvent.NavigateToSlotItems -> onNavigateToSlotItems(event.rackId, event.slotId)
                    is RackDetailUiEvent.NavigateToAddItemDraft -> {
                        onAddItemHere(event.rackId, event.slotId, event.slotXRel, event.slotYRel)
                    }
                    else -> { }
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { owner.viewModelStore.clear() } }

    RackBrowseContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onImageTap = viewModel::onImageTap,
        onEditSelected = viewModel::onEditSelected,
        onRemoveRackSelected = viewModel::onRemoveRackSelected,
        onUpdateEditName = viewModel::onUpdateEditName,
        onUpdateEditDescription = viewModel::onUpdateEditDescription,
        onUpdateEditLocation = viewModel::onUpdateEditLocation,
        onDismissEditDialog = viewModel::onDismissEditDialog,
        onSaveRackEdits = viewModel::onSaveRackEdits,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDeleteRack = viewModel::onConfirmDeleteRack,
        onSlotMarkerDrag = viewModel::onSlotMarkerDrag,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RackBrowseContent(
    uiState: RackDetailUiState,
    onNavigateBack: () -> Unit,
    onImageTap: (Float, Float) -> Unit,
    onEditSelected: () -> Unit,
    onRemoveRackSelected: () -> Unit,
    onUpdateEditName: (String) -> Unit,
    onUpdateEditDescription: (String) -> Unit,
    onUpdateEditLocation: (String) -> Unit,
    onDismissEditDialog: () -> Unit,
    onSaveRackEdits: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDeleteRack: () -> Unit,
    onSlotMarkerDrag: (slotId: String, xRel: Float, yRel: Float, commit: Boolean) -> Unit,
) {
    var pendingDragConfirmation by remember { mutableStateOf<PendingDragConfirmation?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.rack?.name ?: stringResource(R.string.rack_detail_title_default)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("rackDetailBackButton")) {
                        Icon(backArrowIcon, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("rackDetailOverflowMenuButton")
                    ) {
                        Text("⋮")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rack_detail_menu_edit)) },
                            onClick = { showMenu = false; onEditSelected() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rack_detail_menu_remove)) },
                            onClick = { showMenu = false; onRemoveRackSelected() },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            when {
                uiState.isLoading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.rack == null ->
                    Text(
                        text = uiState.error ?: stringResource(R.string.rack_not_found),
                        modifier = Modifier.align(Alignment.Center),
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.screenPadding)
                ) {
                    RackImageWithSlots(
                        photoUri = uiState.rack?.photoUri,
                        slots = uiState.slots,
                        selectedSlot = null,
                        onTap = onImageTap,
                        onSlotMarkerDrag = { slotId, xRel, yRel ->
                            onSlotMarkerDrag(slotId, xRel, yRel, false)
                        },
                        onSlotMarkerDragFinished = { slotId, initialXRel, initialYRel, finalXRel, finalYRel ->
                            pendingDragConfirmation = PendingDragConfirmation(
                                slotId = slotId,
                                initialXRel = initialXRel,
                                initialYRel = initialYRel,
                                finalXRel = finalXRel,
                                finalYRel = finalYRel,
                            )
                        },
                    )
                }
            }
        }
    }
    if (uiState.showEditDialog) {
        AlertDialog(
            onDismissRequest = onDismissEditDialog,
            title = { Text(stringResource(R.string.rack_edit_title)) },
            text = {
                Column {
                    OutlinedTextField(value = uiState.editName, onValueChange = onUpdateEditName)
                    OutlinedTextField(value = uiState.editDescription, onValueChange = onUpdateEditDescription)
                    OutlinedTextField(value = uiState.editLocation, onValueChange = onUpdateEditLocation)
                }
            },
            confirmButton = {
                TextButton(onClick = onSaveRackEdits) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEditDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            title = { Text(stringResource(R.string.rack_remove_confirm_title)) },
            text = { Text(stringResource(R.string.rack_remove_confirm_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteRack) {
                    Text(
                        stringResource(R.string.common_remove),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirm) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
    pendingDragConfirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = {
                onSlotMarkerDrag(pending.slotId, pending.initialXRel, pending.initialYRel, false)
                pendingDragConfirmation = null
            },
            title = { Text(stringResource(R.string.rack_slot_move_confirm_title)) },
            text = { Text(stringResource(R.string.rack_slot_move_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSlotMarkerDrag(pending.slotId, pending.finalXRel, pending.finalYRel, true)
                        pendingDragConfirmation = null
                    },
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onSlotMarkerDrag(pending.slotId, pending.initialXRel, pending.initialYRel, false)
                        pendingDragConfirmation = null
                    },
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private data class PendingDragConfirmation(
    val slotId: String,
    val initialXRel: Float,
    val initialYRel: Float,
    val finalXRel: Float,
    val finalYRel: Float,
)
