package org.deafsapps.storeit.androidapp.presentation.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.deafsapps.storeit.androidapp.design.backArrowIcon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import kotlinx.collections.immutable.persistentListOf
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.androidapp.presentation.rack.ui.ImagePickerDialog
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.presentation.item.model.ItemDetailUiState
import org.deafsapps.storeit.presentation.item.model.ItemDetailUiEvent
import org.deafsapps.storeit.presentation.item.viewmodel.ItemDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: ItemDetailViewModel = koinViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        parameters = { parametersOf(itemId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is ItemDetailUiEvent.NavigateBack -> onNavigateBack()
                    is ItemDetailUiEvent.ShowError -> snackbarHostState.showSnackbar(message = event.message)
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

    ItemDetailScreenContent(
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
        onSave = viewModel::onSave,
        onDeleteClick = viewModel::onDeleteSelected,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDelete = viewModel::onConfirmDelete,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailScreenContent(
    uiState: ItemDetailUiState,
    onNavigateBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateQuantity: (Int?) -> Unit,
    onUpdateOwner: (String) -> Unit,
    onUpdateTagInput: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onUpdatePhotoUri: (String?) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDelete: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showImagePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.item_detail_title),
                        modifier = Modifier.testTag("itemDetailScreenTitle"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("itemDetailBackButton"),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("itemDetailLoading"),
                )
            } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.screenPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingDefault),
                    ) {
                        ItemDetailPhotoSection(
                            photoUri = uiState.photoUri,
                            onShowPicker = { showImagePicker = true },
                        )

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = onUpdateName,
                            label = { Text(stringResource(R.string.item_name_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("itemDetailNameField"),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = onUpdateDescription,
                            label = { Text(stringResource(R.string.item_description_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("itemDetailDescriptionField"),
                            maxLines = 3,
                        )

                        OutlinedTextField(
                            value = uiState.quantity?.toString() ?: "",
                            onValueChange = { onUpdateQuantity(it.toIntOrNull()) },
                            label = { Text(stringResource(R.string.item_quantity_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("itemDetailQuantityField"),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = uiState.owner,
                            onValueChange = onUpdateOwner,
                            label = { Text(stringResource(R.string.item_owner_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("itemDetailOwnerField"),
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
                                    .testTag("itemDetailTagsInputField"),
                                singleLine = true,
                            )
                            Button(
                                onClick = onAddTag,
                                modifier = Modifier.testTag("itemDetailAddTagButton"),
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
                                        modifier = Modifier.testTag("itemDetailTagChip"),
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

                        uiState.error?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("itemDetailErrorText"),
                            )
                        }

                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("itemDetailSaveButton"),
                            enabled = !uiState.isSaving,
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(Dimens.progressIndicatorSizeSmall)
                                        .testTag("itemDetailSaveProgress"),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(stringResource(R.string.common_save))
                            }
                        }

                        TextButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("itemDetailDeleteButton"),
                            enabled = !uiState.isSaving,
                        ) {
                            Text(stringResource(R.string.item_remove_button), color = MaterialTheme.colorScheme.error)
                        }
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

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            title = { Text(stringResource(R.string.item_remove_confirm_title)) },
            text = { Text(stringResource(R.string.item_remove_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    modifier = Modifier.testTag("itemDetailDeleteConfirmButton"),
                ) {
                    Text(stringResource(R.string.common_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteConfirm,
                    modifier = Modifier.testTag("itemDetailDeleteCancelButton"),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ItemDetailPhotoSection(
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
                .testTag("itemDetailPhotoPickerButton"),
        ) {
            Text(
                if (photoUri != null) stringResource(R.string.photo_change)
                else stringResource(R.string.photo_select),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailScreenLoadingPreview() {
    MaterialTheme {
        ItemDetailScreenContent(
            uiState = ItemDetailUiState.getDefault(),
            onNavigateBack = {},
            onUpdateName = {},
            onUpdateDescription = {},
            onUpdateQuantity = {},
            onUpdateOwner = {},
            onUpdateTagInput = {},
            onAddTag = {},
            onRemoveTag = {},
            onUpdatePhotoUri = {},
            onSave = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailScreenContentPreview() {
    MaterialTheme {
        ItemDetailScreenContent(
            uiState = ItemDetailUiState.getDefault().copy(
                isLoading = false,
                name = "Camping stove",
                description = "Two-burner portable stove",
                quantity = 1,
                owner = "Pablo",
                tags = persistentListOf("camping", "kitchen"),
                tagInput = "",
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
            onSave = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailScreenDeleteConfirmPreview() {
    MaterialTheme {
        ItemDetailScreenContent(
            uiState = ItemDetailUiState.getDefault().copy(
                isLoading = false,
                name = "Camping stove",
                showDeleteConfirm = true,
                error = "Unsaved changes need attention.",
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
            onSave = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailScreenSnackbarErrorPreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = Unit) {
        snackbarHostState.showSnackbar(message = "Unable to save item right now.")
    }

    MaterialTheme {
        ItemDetailScreenContent(
            uiState = ItemDetailUiState.getDefault().copy(
                isLoading = false,
                name = "Camping stove",
                description = "Two-burner portable stove",
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
            onSave = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
            snackbarHostState = snackbarHostState,
        )
    }
}
