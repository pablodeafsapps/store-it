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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.androidapp.design.backArrowIcon
import org.deafsapps.storeit.androidapp.design.closeIcon
import org.deafsapps.storeit.androidapp.presentation.rack.ui.ImagePickerDialog
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackDetailScreen
import org.deafsapps.storeit.androidapp.R
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
    onNavigateToAddRack: () -> Unit = {},
) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore =
                ViewModelStore()
        }
    }
    val viewModel: AddItemViewModel = koinViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
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

    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    AddItemScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToAddRack = onNavigateToAddRack,
        onUpdateName = viewModel::onUpdateName,
        onUpdateDescription = viewModel::onUpdateDescription,
        onUpdateQuantity = viewModel::onUpdateQuantity,
        onUpdateOwner = viewModel::onUpdateOwner,
        onUpdateTagInput = viewModel::onUpdateTagInput,
        onAddTag = viewModel::onAddTag,
        onRemoveTag = viewModel::onRemoveTag,
        onUpdatePhotoUri = viewModel::onUpdatePhotoUri,
        onSelectRackAndSlotSelect = viewModel::onSelectRackAndSlotSelected,
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
    onNavigateToAddRack: () -> Unit = {},
    onUpdateName: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateQuantity: (Int?) -> Unit,
    onUpdateOwner: (String) -> Unit,
    onUpdateTagInput: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onUpdatePhotoUri: (String?) -> Unit,
    onSelectRackAndSlotSelect: () -> Unit,
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
            onSelectRackAndSlotSelect = onSelectRackAndSlotSelect,
            onSaveItem = onSaveItem,
        )
        AddItemStep.SELECT_RACK -> SelectRackContent(
            uiState = uiState,
            onRackSelected = onRackSelected,
            onBack = onBackFromSelectRack,
            onNavigateToAddRack = onNavigateToAddRack,
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
    onSelectRackAndSlotSelect: () -> Unit,
    onSaveItem: () -> Unit,
) {
    var showImagePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_item_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("addItemCancelButton"),
                    ) {
                        Icon(
                            imageVector = closeIcon,
                            contentDescription = stringResource(R.string.common_cancel),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
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
                label = { Text(stringResource(R.string.item_name_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addItemNameField"),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onUpdateDescription,
                label = { Text(stringResource(R.string.item_description_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addItemDescriptionField"),
                maxLines = 3,
            )

            OutlinedTextField(
                value = uiState.quantity?.toString() ?: "",
                onValueChange = { onUpdateQuantity(it.toIntOrNull()) },
                label = { Text(stringResource(R.string.item_quantity_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addItemQuantityField"),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.owner,
                onValueChange = onUpdateOwner,
                label = { Text(stringResource(R.string.item_owner_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addItemOwnerField"),
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
                    label = { Text(stringResource(R.string.item_tags_label)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("addItemTagsInputField"),
                    singleLine = true,
                )
                Button(
                    onClick = onAddTag,
                    modifier = Modifier.testTag("addTagButton"),
                ) {
                    Text(stringResource(R.string.common_add))
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
                            modifier = Modifier.testTag("addItemTagChip"),
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
                onClick = onSelectRackAndSlotSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selectRackAndSlotButton"),
            ) {
                Text(
                    if (uiState.selectedRackId != null && uiState.selectedSlotId != null)
                        stringResource(R.string.item_place_selected)
                    else
                        stringResource(R.string.item_place_select),
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = Dimens.screenPadding)
                        .testTag("addItemErrorText"),
                )
            }

            Button(
                onClick = onSaveItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saveItemButton"),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(Dimens.progressIndicatorSizeSmall)
                            .testTag("saveItemProgress"),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.item_save_button))
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
                        contentDescription = stringResource(R.string.item_photo_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(stringResource(R.string.photo_none_selected))
                }
            }
        }
        Spacer(modifier = Modifier.height(Dimens.photoPickerSectionSpacing))
        Button(
            onClick = onShowPicker,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("addItemPhotoPickerButton"),
        ) {
            Text(
                if (photoUri != null) stringResource(R.string.photo_change)
                else stringResource(R.string.photo_select),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectRackContent(
    uiState: AddItemUiState,
    onRackSelected: (Rack) -> Unit,
    onBack: () -> Unit,
    onNavigateToAddRack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_rack_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("selectRackBackButton"),
                    ) {
                        Icon(
                            imageVector = backArrowIcon,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.racks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(Dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingDefault),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.select_rack_empty_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onNavigateToAddRack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("addRackEmptyStateButton"),
                ) {
                    Text(stringResource(R.string.select_rack_add_rack))
                }
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
                            .testTag("addItemRackCardButton")
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
            onNavigateToAddRack = {},
            onUpdateName = {},
            onUpdateDescription = {},
            onUpdateQuantity = {},
            onUpdateOwner = {},
            onUpdateTagInput = {},
            onAddTag = {},
            onRemoveTag = {},
            onUpdatePhotoUri = {},
            onSelectRackAndSlotSelect = {},
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
            onNavigateToAddRack = {},
            onUpdateName = {},
            onUpdateDescription = {},
            onUpdateQuantity = {},
            onUpdateOwner = {},
            onUpdateTagInput = {},
            onAddTag = {},
            onRemoveTag = {},
            onUpdatePhotoUri = {},
            onSelectRackAndSlotSelect = {},
            onSaveItem = {},
            onRackSelected = {},
            onBackFromSelectRack = {},
            onBackFromSelectSlot = {},
            onSlotSelectedForItem = { _, _ -> },
        )
    }
}
