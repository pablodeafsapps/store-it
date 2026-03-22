package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import org.deafsapps.storeit.androidapp.design.Dimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.presentation.rack.model.RackDetailSlotVo
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiState
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun RackDetailScreen(
    rackId: String,
    onNavigateBack: () -> Unit,
    forItemPlacement: Boolean = false,
    onSlotSelectedForItem: (rackId: String, slotId: String) -> Unit = { _, _ -> },
    onAddItemHere: (rackId: String, slotId: String) -> Unit = { _, _ -> },
    onNavigateToSlotItems: (rackId: String, slotId: String) -> Unit = { _, _ -> },
) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore =
                ViewModelStore()
        }
    }
    val viewModel: RackDetailViewModel = koinViewModel<RackDetailViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        parameters = { parametersOf(rackId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is RackDetailUiEvent.NavigateBack -> onNavigateBack()
                    is RackDetailUiEvent.ShowError -> { }
                    is RackDetailUiEvent.SlotSelected -> {
                        onAddItemHere(event.rackId, event.slotId)
                    }
                    is RackDetailUiEvent.NavigateToSlotItems -> {
                        onNavigateToSlotItems(event.rackId, event.slotId)
                    }
                    null -> { }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    RackDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        forItemPlacement = forItemPlacement,
        onSlotSelectedForItem = { slotId -> onSlotSelectedForItem(rackId, slotId) },
        onEditSelect = viewModel::onEditSelect,
        onRemoveRackSelect = viewModel::onRemoveRackSelect,
        onImageTap = { x, y -> viewModel.onImageTap(x, y, forItemPlacement) },
        onUpdateEditName = viewModel::onUpdateEditName,
        onUpdateEditDescription = viewModel::onUpdateEditDescription,
        onUpdateEditLocation = viewModel::onUpdateEditLocation,
        onDismissEditDialog = viewModel::onDismissEditDialog,
        onSaveRackEdits = viewModel::onSaveRackEdits,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDeleteRack = viewModel::onConfirmDeleteRack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RackDetailContent(
    uiState: RackDetailUiState,
    onNavigateBack: () -> Unit,
    forItemPlacement: Boolean = false,
    onSlotSelectedForItem: (slotId: String) -> Unit = {},
    onEditSelect: () -> Unit = {},
    onRemoveRackSelect: () -> Unit = {},
    onImageTap: (Float, Float) -> Unit = { _, _ -> },
    onUpdateEditName: (String) -> Unit = {},
    onUpdateEditDescription: (String) -> Unit = {},
    onUpdateEditLocation: (String) -> Unit = {},
    onDismissEditDialog: () -> Unit = {},
    onSaveRackEdits: () -> Unit = {},
    onDismissDeleteConfirm: () -> Unit = {},
    onConfirmDeleteRack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            val title = (uiState.rack?.name ?: "Rack") + if (forItemPlacement) " - select slot" else ""
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("rackDetailBackButton"),
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    if (!forItemPlacement) {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("rackDetailOverflowMenuButton"),
                        ) {
                            Text("⋮", style = MaterialTheme.typography.titleLarge)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.testTag("editRackMenuItem"),
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEditSelect()
                                },
                            )
                            DropdownMenuItem(
                                modifier = Modifier.testTag("removeRackMenuItem"),
                                text = { Text("Remove rack") },
                                onClick = {
                                    showMenu = false
                                    onRemoveRackSelect()
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            uiState.selectedSlot?.let { slot ->
                if (forItemPlacement) {
                    Button(
                        onClick = { onSlotSelectedForItem(slot.id) },
                        modifier = Modifier.testTag("useThisSlotButton"),
                    ) {
                        Text("Use this slot")
                    }
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
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(Dimens.progressIndicatorSizeLarge),
                    )
                }
                uiState.rack == null -> {
                    Text(
                        text = uiState.error ?: "Rack not found",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(Dimens.screenPadding),
                    )
                }
                else -> {
                    val rack = uiState.rack!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.screenPadding),
                    ) {
                        RackImageWithSlots(
                            photoUri = rack.photoUri,
                            slots = uiState.slots,
                            selectedSlot = uiState.selectedSlot,
//                            selectedSlotId = uiState.selectedSlotId,
                            onTap = onImageTap,
                        )
                        if (rack.description.isNotBlank()) {
                            Text(
                                text = rack.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = Dimens.spacingDefault),
                            )
                        }
                        if (rack.location.isNotBlank()) {
                            Text(
                                text = "Location: ${rack.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Dimens.spacingSmall),
                            )
                        }
                    }
                }
            }
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(Dimens.screenPadding),
                )
            }
        }
    }

    if (uiState.showEditDialog) {
        EditRackDialog(
            name = uiState.editName,
            description = uiState.editDescription,
            location = uiState.editLocation,
            onNameChange = onUpdateEditName,
            onDescriptionChange = onUpdateEditDescription,
            onLocationChange = onUpdateEditLocation,
            onDismiss = onDismissEditDialog,
            onSave = onSaveRackEdits,
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            title = { Text("Remove rack?") },
            text = { Text("This will delete the rack and all its slots and items. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDeleteRack,
                    modifier = Modifier.testTag("removeRackConfirmButton"),
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteConfirm,
                    modifier = Modifier.testTag("removeRackCancelButton"),
                ) {
                    Text("Cancel")
                }
            },
        )
    }

}

@Composable
private fun RackImageWithSlots(
    photoUri: String?,
    slots: List<RackDetailSlotVo>,
    selectedSlot: RackDetailSlotVo?,
//    selectedSlotId: String?,
    onTap: (xRel: Float, yRel: Float) -> Unit,
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { imageSize = it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.rackDetailImageVerticalPadding),
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Rack photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(Dimens.rackDetailPlaceholderHeight)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("rackDetailImageOverlay")
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val w = imageSize.width.toFloat().coerceAtLeast(1f)
                            val h = imageSize.height.toFloat().coerceAtLeast(1f)
                            onTap((offset.x / w).coerceIn(0f, 1f), (offset.y / h).coerceIn(0f, 1f))
                        }
                    },
            )
            slots.forEach { slot ->
                val isSelected = selectedSlot?.id == slot.id
                val color = slotMarkerColor(isSelected = isSelected)
                with(density) {
                    val halfPx = Dimens.rackDetailSlotMarkerHalfSize.toPx()
                    val xPx = (slot.xRel * imageSize.width - halfPx).toInt()
                    val yPx = (slot.yRel * imageSize.height - halfPx).toInt()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(xPx, yPx) }
                            .size(Dimens.rackDetailSlotMarkerSize)
                            .background(color = color, shape = CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun slotMarkerColor(isSelected: Boolean): Color =
    if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

@Composable
private fun EditRackDialog(
    name: String,
    description: String,
    location: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit rack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dialogContentSpacing)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editRackNameField"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editRackDescriptionField"),
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editRackLocationField"),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                modifier = Modifier.testTag("editRackSaveButton"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("editRackCancelButton"),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun RackDetailScreenPreview() {
    MaterialTheme {
        val selectedSlot = RackDetailSlotVo(id = "s1", xRel = 0.2f, yRel = 0.3f)
        RackDetailContent(
            uiState = RackDetailUiState(
                rack = Rack(
                    id = "1",
                    name = "Garage Rack",
                    description = "Rack for tools and equipment",
                    location = "Garage - East Wall",
                    photoUri = null
                ),
                slots = listOf(
                    RackDetailSlotVo(id = "s1", xRel = 0.2f, yRel = 0.3f),
                    RackDetailSlotVo(id = "s2", xRel = 0.5f, yRel = 0.6f)
                ),
                selectedSlot = selectedSlot,
                isLoading = false,
                error = null,
                showEditDialog = false,
                editName = "",
                editDescription = "",
                editLocation = "",
                showDeleteConfirm = false
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RackDetailScreenLoadingPreview() {
    MaterialTheme {
        RackDetailContent(
            uiState = RackDetailUiState.getDefault().copy(isLoading = true),
            onNavigateBack = {}
        )
    }
}
