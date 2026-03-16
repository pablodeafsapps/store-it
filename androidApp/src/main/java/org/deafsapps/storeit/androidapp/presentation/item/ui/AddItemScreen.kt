package org.deafsapps.storeit.androidapp.presentation.item.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.androidapp.presentation.rack.ui.ImagePickerDialog
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackDetailScreen
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.presentation.item.model.AddItemStep
import org.deafsapps.storeit.presentation.item.model.AddItemUiEvent
import org.deafsapps.storeit.presentation.item.model.AddItemUiState
import org.deafsapps.storeit.presentation.item.viewmodel.AddItemViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddItemScreen(
    initialRackId: String?,
    initialSlotId: String?,
    onNavigateBack: () -> Unit,
) {
    val viewModel: AddItemViewModel = koinViewModel(
        parameters = { parametersOf(initialRackId, initialSlotId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is AddItemUiEvent.NavigateBack -> onNavigateBack()
                    is AddItemUiEvent.ShowError -> { }
                    null -> { }
                }
            }
        }
    }

    AddItemScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateName = viewModel::onUpdateName,
        onUpdateDescription = viewModel::onUpdateDescription,
        onUpdateQuantity = viewModel::onUpdateQuantity,
        onUpdateOwner = viewModel::onUpdateOwner,
        onUpdateTagInput = viewModel::onUpdateTagInput,
        onAddTag = viewModel::onAddTag,
        onRemoveTag = viewModel::onRemoveTag,
        onUpdatePhotoUri = viewModel::onUpdatePhotoUri,
        onSelectRackAndSlotClick = viewModel::onSelectRackAndSlotClick,
        onSaveItem = viewModel::onSaveItem,
        onRackSelected = viewModel::onRackSelected,
        onBackFromSelectRack = viewModel::onBackFromSelectRack,
        onBackFromSelectSlot = viewModel::onBackFromSelectSlot,
        onSlotSelectedForItem = viewModel::onSlotSelectedForItem,
    )
}

@Composable
private fun AddItemScreenContent(
    uiState: AddItemUiState,
    onNavigateBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateQuantity: (Int?) -> Unit,
    onUpdateOwner: (String) -> Unit,
    onUpdateTagInput: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onUpdatePhotoUri: (String?) -> Unit,
    onSelectRackAndSlotClick: () -> Unit,
    onSaveItem: () -> Unit,
    onRackSelected: (Rack) -> Unit,
    onBackFromSelectRack: () -> Unit,
    onBackFromSelectSlot: () -> Unit,
    onSlotSelectedForItem: (rackId: String, slotId: String) -> Unit,
) {
    when (uiState.step) {
        AddItemStep.FORM -> AddItemForm(
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onUpdateName = onUpdateName,
            onUpdateDescription = onUpdateDescription,
            onUpdateQuantity = onUpdateQuantity,
            onUpdateOwner = onUpdateOwner,
            onUpdateTagInput = onUpdateTagInput,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
            onUpdatePhotoUri = onUpdatePhotoUri,
            onSelectRackAndSlotClick = onSelectRackAndSlotClick,
            onSaveItem = onSaveItem,
        )
        AddItemStep.SELECT_RACK -> SelectRackContent(
            uiState = uiState,
            onRackSelected = onRackSelected,
            onBack = onBackFromSelectRack,
        )
        AddItemStep.SELECT_SLOT -> {
            val rackId = uiState.selectedRackId ?: return
            RackDetailScreen(
                rackId = rackId,
                onNavigateBack = onBackFromSelectSlot,
                forItemPlacement = true,
                onSlotSelectedForItem = onSlotSelectedForItem,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemForm(
    uiState: AddItemUiState,
    onNavigateBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateQuantity: (Int?) -> Unit,
    onUpdateOwner: (String) -> Unit,
    onUpdateTagInput: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onUpdatePhotoUri: (String?) -> Unit,
    onSelectRackAndSlotClick: () -> Unit,
    onSaveItem: () -> Unit,
) {
    var showImagePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Item") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingDefault),
        ) {
            PhotoPickerSection(
                photoUri = uiState.photoUri,
                onShowPicker = { showImagePicker = true },
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onUpdateName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onUpdateDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            OutlinedTextField(
                value = uiState.quantity?.toString() ?: "",
                onValueChange = { onUpdateQuantity(it.toIntOrNull()) },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.owner,
                onValueChange = onUpdateOwner,
                label = { Text("Owner") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.tagInput,
                    onValueChange = onUpdateTagInput,
                    label = { Text("Tags") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(onClick = onAddTag) {
                    Text("Add")
                }
            }
            if (uiState.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                ) {
                    uiState.tags.forEach { tag ->
                        Card(
                            onClick = { onRemoveTag(tag) },
                            shape = RoundedCornerShape(Dimens.cardCornerRadiusSmall),
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(
                                    horizontal = Dimens.spacingSmall,
                                    vertical = Dimens.spacingSmall / 2,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onSelectRackAndSlotClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.selectedRackId != null && uiState.selectedSlotId != null)
                        "Place: Rack & slot selected"
                    else
                        "Select rack & slot",
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Dimens.screenPadding),
                )
            }

            Button(
                onClick = onSaveItem,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.progressIndicatorSizeSmall),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Save Item")
                }
            }
        }
    }

    if (showImagePicker) {
        ImagePickerDialog(
            onDismiss = { showImagePicker = false },
            onImageSelected = {
                onUpdatePhotoUri(it)
                showImagePicker = false
            },
        )
    }
}

@Composable
private fun PhotoPickerSection(
    photoUri: String?,
    onShowPicker: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.photoPickerCardHeight)
                .clip(RoundedCornerShape(Dimens.cardCornerRadiusSmall)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Item photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text("No photo selected")
                }
            }
        }
        Spacer(modifier = Modifier.height(Dimens.photoPickerSectionSpacing))
        Button(
            onClick = onShowPicker,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (photoUri != null) "Change Photo" else "Select Photo")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectRackContent(
    uiState: AddItemUiState,
    onRackSelected: (Rack) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Rack") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.racks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text("No racks. Add a rack first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(Dimens.listContentPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing),
            ) {
                items(uiState.racks, key = { it.id }) { rack ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onRackSelected(rack) }),
                        shape = RoundedCornerShape(Dimens.cardCornerRadiusMedium),
                        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
                    ) {
                        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                            if (rack.photoUri != null) {
                                AsyncImage(
                                    model = rack.photoUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(Dimens.cardCornerRadiusSmall))
                                        .height(Dimens.rackListItemImageHeight),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(modifier = Modifier.size(Dimens.rackListItemContentSpacing))
                            }
                            Text(
                                text = rack.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (rack.location.isNotBlank()) {
                                Text(
                                    text = rack.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddItemScreenPreview() {
    MaterialTheme {
        AddItemScreenContent(
            uiState = AddItemUiState(
                name = "Drill",
                description = "Cordless power drill",
                quantity = 1,
                owner = "Me",
                tags = listOf("Tools", "Power"),
                tagInput = "",
                photoUri = null,
                selectedRackId = "1",
                selectedSlotId = "A1",
                racks = emptyList(),
                step = AddItemStep.FORM,
                isLoading = false,
                error = null,
                isSuccess = false,
            ),
            onNavigateBack = {},
            onUpdateName = {},
            onUpdateDescription = {},
            onUpdateQuantity = {},
            onUpdateOwner = {},
            onUpdateTagInput = {},
            onAddTag = {},
            onRemoveTag = {},
            onUpdatePhotoUri = {},
            onSelectRackAndSlotClick = {},
            onSaveItem = {},
            onRackSelected = {},
            onBackFromSelectRack = {},
            onBackFromSelectSlot = {},
            onSlotSelectedForItem = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddItemScreenSelectRackPreview() {
    MaterialTheme {
        AddItemScreenContent(
            uiState = AddItemUiState.getDefault().copy(
                step = AddItemStep.SELECT_RACK,
                racks = listOf(
                    Rack(id = "1", name = "Garage Rack", location = "Garage"),
                    Rack(id = "2", name = "Kitchen Shelf", location = "Kitchen"),
                )
            ),
            onNavigateBack = {},
            onUpdateName = {},
            onUpdateDescription = {},
            onUpdateQuantity = {},
            onUpdateOwner = {},
            onUpdateTagInput = {},
            onAddTag = {},
            onRemoveTag = {},
            onUpdatePhotoUri = {},
            onSelectRackAndSlotClick = {},
            onSaveItem = {},
            onRackSelected = {},
            onBackFromSelectRack = {},
            onBackFromSelectSlot = {},
            onSlotSelectedForItem = { _, _ -> },
        )
    }
}
