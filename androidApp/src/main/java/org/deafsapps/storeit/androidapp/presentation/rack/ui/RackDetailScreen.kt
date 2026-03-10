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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import org.deafsapps.storeit.presentation.rack.model.RackDetailSlotView
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RackDetailScreen(
    viewModel: RackDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is RackDetailUiEvent.NavigateBack -> onNavigateBack()
                    is RackDetailUiEvent.ShowError -> { }
                    is RackDetailUiEvent.SlotSelected -> { }
                    null -> { }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.rack?.name ?: "Rack") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                viewModel.onEditClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove rack") },
                            onClick = {
                                showMenu = false
                                viewModel.onRemoveRackSelect()
                            },
                        )
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.screenPadding),
                    ) {
                        RackImageWithSlots(
                            photoUri = uiState.rack!!.photoUri,
                            slots = uiState.slots,
                            selectedSlotId = uiState.selectedSlotId,
                            onTap = { xRel, yRel -> viewModel.onImageTap(xRel, yRel) },
                        )
                        if (uiState.rack!!.description.isNotBlank()) {
                            Text(
                                text = uiState.rack!!.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = Dimens.spacingDefault),
                            )
                        }
                        if (uiState.rack!!.location.isNotBlank()) {
                            Text(
                                text = "Location: ${uiState.rack!!.location}",
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
            onNameChange = viewModel::onUpdateEditName,
            onDescriptionChange = viewModel::onUpdateEditDescription,
            onLocationChange = viewModel::onUpdateEditLocation,
            onDismiss = viewModel::onDismissEditDialog,
            onSave = viewModel::onSaveRackEdits,
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteConfirm,
            title = { Text("Remove rack?") },
            text = { Text("This will delete the rack and all its slots and items. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDeleteRack) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDeleteConfirm) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun RackImageWithSlots(
    photoUri: String?,
    slots: List<RackDetailSlotView>,
    selectedSlotId: String?,
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
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val w = imageSize.width.toFloat().coerceAtLeast(1f)
                            val h = imageSize.height.toFloat().coerceAtLeast(1f)
                            onTap((offset.x / w).coerceIn(0f, 1f), (offset.y / h).coerceIn(0f, 1f))
                        }
                    },
            )
            slots.forEach { slot ->
                with(density) {
                    val halfPx = Dimens.rackDetailSlotMarkerHalfSize.toPx()
                    val xPx = (slot.xRel * imageSize.width - halfPx).toInt()
                    val yPx = (slot.yRel * imageSize.height - halfPx).toInt()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(xPx, yPx) }
                            .size(Dimens.rackDetailSlotMarkerSize)
                            .background(
                                color = if (slot.id == selectedSlotId)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
